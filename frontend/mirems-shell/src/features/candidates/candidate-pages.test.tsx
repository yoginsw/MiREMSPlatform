import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { CandidateListPage, CandidateRegistrationForm, CandidateReviewPage } from './candidate-pages';

const routeContext = { electionId: 'el-2028-kr-parliamentary', contestId: 'contest-national' };

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['ELECTION_ADMIN', 'ELECTION_OFFICER'],
  electionScope: ['el-2028-kr-parliamentary'],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_ADMIN' || role === 'ELECTION_OFFICER',
  hasElectionScope: (electionId) => electionId === 'el-2028-kr-parliamentary',
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

afterEach(() => {
  vi.useRealTimers();
});

describe('Candidate management pages', () => {
  it('lists candidates for a contest and polls status updates', async () => {
    let calls = 0;
    server.use(
      http.get('/miremsplatform/elections/:electionId/contests/:contestId/candidates', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params).toMatchObject(routeContext);
        calls += 1;
        return HttpResponse.json([
          { id: 'cand-001', contestId: routeContext.contestId, displayName: '김후보', party: '미래당', status: calls === 1 ? 'PENDING' : 'APPROVED' },
        ]);
      }),
    );

    renderWithAuth(<CandidateListPage {...routeContext} pollIntervalMs={100} />);

    expect(await screen.findByText('김후보')).toBeInTheDocument();
    expect(await screen.findByText('PENDING')).toBeInTheDocument();

    await waitFor(() => expect(screen.getByText('APPROVED')).toBeInTheDocument());
    expect(calls).toBeGreaterThanOrEqual(2);
  });

  it('submits candidate registration through the generated API client', async () => {
    server.use(
      http.post('/miremsplatform/elections/:electionId/contests/:contestId/candidates', async ({ request, params }) => {
        expectBearerAuth(request);
        expect(params).toMatchObject(routeContext);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toMatchObject({ displayName: '이후보', party: '시민당', externalReference: 'CAND-2028-02' });
        return HttpResponse.json({ id: 'cand-002', contestId: routeContext.contestId, displayName: '이후보', party: '시민당', status: 'PENDING' }, { status: 201 });
      }),
    );

    renderWithAuth(<CandidateRegistrationForm {...routeContext} />);

    fireEvent.change(screen.getByLabelText('후보자명'), { target: { value: '이후보' } });
    fireEvent.change(screen.getByLabelText('정당'), { target: { value: '시민당' } });
    fireEvent.change(screen.getByLabelText('외부 참조 ID'), { target: { value: 'CAND-2028-02' } });
    fireEvent.click(screen.getByRole('button', { name: '후보자 등록' }));

    expect(await screen.findByText(/후보자 등록 프로세스가 시작되었습니다/)).toBeInTheDocument();
    expect(screen.getByText(/등록 후보자 ID:\s*cand-002/)).toBeInTheDocument();
  });

  it('lets election officers approve pending candidates through process signals', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/contests/:contestId/candidates', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([
          { id: 'cand-003', contestId: routeContext.contestId, displayName: '박후보', party: '무소속', status: 'PENDING' },
        ]);
      }),
      http.get('/miremsplatform/admin/processes', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([
          { instanceId: 'pi-completed-cand-003', processId: 'candidate-registration', status: 'COMPLETED', variables: { candidateId: 'cand-003' }, activeNodes: [] },
          { instanceId: 'pi-unrelated-cand-003', processId: 'election-publishing', status: 'ACTIVE', variables: { candidateId: 'cand-003' }, activeNodes: ['Review'] },
          { instanceId: 'pi-cand-003', processId: 'candidate-registration', status: 'ACTIVE', variables: { candidateId: 'cand-003' }, activeNodes: ['OfficerReview'] },
        ]);
      }),
      http.post('/miremsplatform/admin/processes/pi-cand-003/signal', async ({ request }) => {
        expectBearerAuth(request);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toMatchObject({ signalName: 'approveCandidate', payload: { candidateId: 'cand-003', approved: true } });
        return HttpResponse.json({ instanceId: 'pi-cand-003', processId: 'candidate-registration', status: 'COMPLETED', variables: {}, activeNodes: [] });
      }),
    );

    renderWithAuth(<CandidateReviewPage {...routeContext} />);

    const row = await screen.findByRole('row', { name: /박후보/ });
    expect(within(row).getByText('PENDING')).toBeInTheDocument();
    fireEvent.click(within(row).getByRole('button', { name: '승인' }));

    expect(await screen.findByText('후보자 심사 신호를 전송했습니다.')).toBeInTheDocument();
    expect(within(row).getByText('APPROVED')).toBeInTheDocument();
  });

  it('hides candidate review from users without ELECTION_OFFICER role', () => {
    renderWithAuth(<CandidateReviewPage {...routeContext} />, {
      ...authBase,
      roles: ['ELECTION_ADMIN'],
      hasRole: (role) => role === 'ELECTION_ADMIN',
    });

    expect(screen.queryByRole('heading', { name: '후보자 심사' })).not.toBeInTheDocument();
    expect(screen.getByText('후보자 심사는 선거 사무관에게만 허용됩니다.')).toBeInTheDocument();
  });
});
