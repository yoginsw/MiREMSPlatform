import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { ElectionDetailPage, ElectionListPage } from './election-pages';
import { server } from '../../test/msw-server';

const elections = [
  {
    id: 'el-2026-kr-local',
    name: '2026 지방선거',
    electionType: 'LOCAL',
    jurisdiction: 'KR-11',
    status: 'DRAFT',
    scheduledDate: '2026-06-03',
    countryCode: 'KR',
    extensionPackId: 'ext-kr',
  },
  {
    id: 'el-2027-presidential',
    name: '2027 대통령선거',
    electionType: 'PRESIDENTIAL',
    jurisdiction: 'KR',
    status: 'PUBLISHED',
    scheduledDate: '2027-03-09',
    countryCode: 'KR',
    extensionPackId: 'ext-kr',
  },
  {
    id: 'el-2024-us-general',
    name: '2024 US General Election',
    electionType: 'REGIONAL',
    jurisdiction: 'US-CA',
    status: 'CLOSED',
    scheduledDate: '2024-11-05',
    countryCode: 'US',
    extensionPackId: 'ext-us',
  },
];

const contests = [
  { id: 'contest-mayor', electionId: 'el-2026-kr-local', title: '서울시장', type: 'SINGLE_MEMBER', seats: 1 },
  { id: 'contest-council', electionId: 'el-2026-kr-local', title: '시의원 비례대표', type: 'PROPORTIONAL', seats: 5 },
];

const ballots = [
  {
    id: 'ballot-1',
    electionId: 'el-2026-kr-local',
    ballotVersion: 2,
    active: true,
    contests: [],
    styles: [
      { id: 'style-kr-ko', electionId: 'el-2026-kr-local', ballotId: 'ballot-1', language: 'ko', district: '서울', accessibilityFeatures: ['LARGE_PRINT'] },
      { id: 'style-kr-en', electionId: 'el-2026-kr-local', ballotId: 'ballot-1', language: 'en', district: '서울', accessibilityFeatures: [] },
    ],
  },
];

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['ELECTION_ADMIN'],
  electionScope: ['*'],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_ADMIN',
  hasElectionScope: () => true,
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

describe('Election pages', () => {
  it('loads elections from the generated API client and filters by status and type', async () => {
    server.use(http.get('/miremsplatform/elections', ({ request }) => {
      expectBearerAuth(request);
      return HttpResponse.json(elections);
    }));

    renderWithAuth(<ElectionListPage />);

    expect(await screen.findByRole('heading', { name: '선거 목록' })).toBeInTheDocument();
    expect(await screen.findByRole('cell', { name: '2026 지방선거' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '2027 대통령선거' })).toBeInTheDocument();
    expect(screen.getByText('총 3건 중 1-3건 표시')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('상태 필터'), { target: { value: 'PUBLISHED' } });
    expect(screen.queryByRole('cell', { name: '2026 지방선거' })).not.toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '2027 대통령선거' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('유형 필터'), { target: { value: 'LOCAL' } });
    expect(screen.getByText('조건에 맞는 선거가 없습니다.')).toBeInTheDocument();
  });

  it('paginates election rows with accessible navigation controls', async () => {
    server.use(http.get('/miremsplatform/elections', ({ request }) => {
      expectBearerAuth(request);
      return HttpResponse.json(elections);
    }));

    renderWithAuth(<ElectionListPage pageSize={2} />);

    expect(await screen.findByText('총 3건 중 1-2건 표시')).toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: '2024 US General Election' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '다음 페이지' }));
    expect(screen.getByText('총 3건 중 3-3건 표시')).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '2024 US General Election' })).toBeInTheDocument();
  });

  it('renders election details, related contest and ballot summaries, and role-based actions', async () => {
    server.use(
      http.get('/miremsplatform/elections/el-2026-kr-local', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(elections[0]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/contests', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(contests);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/ballots', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(ballots);
      }),
    );

    renderWithAuth(<ElectionDetailPage electionId="el-2026-kr-local" />);

    expect(await screen.findByRole('heading', { name: '2026 지방선거' })).toBeInTheDocument();
    expect(screen.getByText(/서울시장/)).toBeInTheDocument();
    expect(screen.getByText(/시의원 비례대표/)).toBeInTheDocument();
    expect(screen.getByText(/활성 투표용지\s*1\s*개 · 스타일\s*2\s*개/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '선거 공표' })).toBeEnabled();
    expect(screen.getByRole('button', { name: '선거 종료' })).toBeDisabled();
  });

  it('optimistically updates election status when an admin publishes a draft election', async () => {
    server.use(
      http.get('/miremsplatform/elections/el-2026-kr-local', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(elections[0]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/contests', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/ballots', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([]);
      }),
      http.put('/miremsplatform/elections/el-2026-kr-local/publish', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json({ ...elections[0], status: 'PUBLISHED' });
      }),
    );

    renderWithAuth(<ElectionDetailPage electionId="el-2026-kr-local" />);

    await waitFor(() => expect(screen.getAllByText('DRAFT').length).toBeGreaterThan(0));
    fireEvent.click(screen.getByRole('button', { name: '선거 공표' }));
    await waitFor(() => expect(screen.getAllByText('PUBLISHED').length).toBeGreaterThan(0));
    expect(screen.getByRole('status')).toHaveTextContent('상태 변경 요청이 반영되었습니다.');
  });

  it('hides administrative election actions for observer role', async () => {
    server.use(
      http.get('/miremsplatform/elections/el-2026-kr-local', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(elections[0]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/contests', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/ballots', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([]);
      }),
    );

    renderWithAuth(<ElectionDetailPage electionId="el-2026-kr-local" />, {
      ...authBase,
      roles: ['OBSERVER'],
      hasRole: () => false,
    });

    await waitFor(() => expect(screen.getByRole('heading', { name: '2026 지방선거' })).toBeInTheDocument());
    const actions = screen.getByLabelText('선거 상태 작업');
    expect(within(actions).queryByRole('button', { name: '선거 공표' })).not.toBeInTheDocument();
    expect(screen.getByText('상태 변경 작업은 선거 관리자에게만 표시됩니다.')).toBeInTheDocument();
  });

  it('hides administrative election actions for admins outside election scope', async () => {
    server.use(
      http.get('/miremsplatform/elections/el-2026-kr-local', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json(elections[0]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/contests', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([]);
      }),
      http.get('/miremsplatform/elections/el-2026-kr-local/ballots', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([]);
      }),
    );

    renderWithAuth(<ElectionDetailPage electionId="el-2026-kr-local" />, {
      ...authBase,
      electionScope: ['other-election'],
      hasElectionScope: () => false,
    });

    await waitFor(() => expect(screen.getByRole('heading', { name: '2026 지방선거' })).toBeInTheDocument());
    const actions = screen.getByLabelText('선거 상태 작업');
    expect(within(actions).queryByRole('button', { name: '선거 공표' })).not.toBeInTheDocument();
    expect(screen.getByText('상태 변경 작업은 선거 관리자에게만 표시됩니다.')).toBeInTheDocument();
  });
});
