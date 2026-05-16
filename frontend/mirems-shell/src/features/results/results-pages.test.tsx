import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { ResultsDashboardPage, TabulationProgressPage } from './results-pages';

const electionId = '20000000-0000-0000-0000-000000000053';
const processInstanceId = '30000000-0000-0000-0000-000000000053';

const completedResults = {
  electionId,
  status: 'COMPLETED',
  generatedAt: '2026-05-16T01:00:00Z',
  contestTallies: [
    {
      contestId: 'mayor-contest',
      candidateTallies: [
        { candidateId: 'kim-future', voteCount: 120 },
        { candidateId: 'park-policy', voteCount: 80 },
      ],
    },
    {
      contestId: 'council-contest',
      candidateTallies: [
        { candidateId: 'lee-open', voteCount: 70 },
        { candidateId: 'choi-audit', voteCount: 30 },
      ],
    },
  ],
};

const pendingResults = { ...completedResults, status: 'PENDING', contestTallies: [] };

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['ELECTION_OFFICER', 'TABULATION_OFFICER'],
  electionScope: [electionId],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_OFFICER' || role === 'TABULATION_OFFICER',
  hasElectionScope: (scopeElectionId) => scopeElectionId === electionId,
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

beforeEach(() => {
  Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:official-results') });
  Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() });
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('Results dashboard page', () => {
  it('renders bar and pie chart summaries from tabulation results and refreshes while pending', async () => {
    let calls = 0;
    server.use(
      http.get('/miremsplatform/elections/:electionId/results', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.electionId).toBe(electionId);
        calls += 1;
        return HttpResponse.json(calls === 1 ? pendingResults : completedResults);
      }),
    );

    renderWithAuth(<ResultsDashboardPage electionId={electionId} pollIntervalMs={20} />);

    expect(await screen.findByText('집계 진행 중')).toBeInTheDocument();
    expect(await screen.findByText('집계 완료')).toBeInTheDocument();
    const mayor = screen.getByRole('region', { name: 'Contest mayor-contest 결과 차트' });
    expect(within(mayor).getByText('kim-future')).toBeInTheDocument();
    expect(within(mayor).getByText('120표')).toBeInTheDocument();
    expect(within(mayor).getByLabelText('kim-future 득표율 60.0%')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'mayor-contest pie chart' })).toBeInTheDocument();
    expect(calls).toBeGreaterThanOrEqual(2);
  });

  it('downloads an official results PDF generated from the current results', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/results', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(completedResults);
      }),
    );
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    renderWithAuth(<ResultsDashboardPage electionId={electionId} pollIntervalMs={0} />);

    expect(await screen.findByText('집계 완료')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '공식 결과 PDF 다운로드' }));

    expect(URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));
    expect(clickSpy).toHaveBeenCalledOnce();
    expect(screen.getByText('공식 결과 PDF가 준비되었습니다.')).toBeInTheDocument();
    clickSpy.mockRestore();
  });

  it('blocks result loading when the user lacks election scope', () => {
    renderWithAuth(<ResultsDashboardPage electionId={electionId} pollIntervalMs={0} />, {
      ...authBase,
      electionScope: [],
      hasElectionScope: () => false,
    });

    expect(screen.getByText('이 선거의 결과를 조회할 권한이 없습니다.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '새로고침' })).not.toBeInTheDocument();
  });
});

describe('Tabulation progress page', () => {
  it('allows a TABULATION_OFFICER to trigger tabulation and monitors active BPMN progress', async () => {
    server.use(
      http.post('/miremsplatform/elections/:electionId/tabulate', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.electionId).toBe(electionId);
        return HttpResponse.json({
          instanceId: processInstanceId,
          processId: 'ballot-tabulation',
          status: 'ACTIVE',
          variables: { electionId },
          activeNodes: ['load-results', 'aggregate-contests'],
        });
      }),
      http.get('/miremsplatform/admin/processes', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([
          { instanceId: 'stale', processId: 'ballot-tabulation', status: 'COMPLETED', variables: { electionId }, activeNodes: [] },
          { instanceId: processInstanceId, processId: 'ballot-tabulation', status: 'ACTIVE', variables: { electionId }, activeNodes: ['publish-results'] },
          { instanceId: 'unrelated', processId: 'candidate-review', status: 'ACTIVE', variables: { electionId }, activeNodes: ['review'] },
        ]);
      }),
    );

    renderWithAuth(<TabulationProgressPage electionId={electionId} pollIntervalMs={20} />);

    fireEvent.click(screen.getByRole('button', { name: '집계 시작' }));

    expect(await screen.findByText(`프로세스 인스턴스: ${processInstanceId}`)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('활성 노드: publish-results')).toBeInTheDocument());
    expect(screen.queryByText('활성 노드: review')).not.toBeInTheDocument();
  });

  it('blocks tabulation trigger for non-tabulation officers before API calls', () => {
    renderWithAuth(<TabulationProgressPage electionId={electionId} pollIntervalMs={0} />, {
      ...authBase,
      roles: ['ELECTION_OFFICER'],
      hasRole: (role) => role === 'ELECTION_OFFICER',
    });

    expect(screen.getByText('집계 워크플로우는 TABULATION_OFFICER 권한이 필요합니다.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '집계 시작' })).not.toBeInTheDocument();
  });
});
