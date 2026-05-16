import React from 'react';
import axe from 'axe-core';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { VotingSessionPage } from './voting-session-page';

const voterId = '10000000-0000-0000-0000-000000000052';
const electionId = '20000000-0000-0000-0000-000000000052';
const ballotId = '30000000-0000-0000-0000-000000000052';
const ballotStyleId = '40000000-0000-0000-0000-000000000052';
const sessionId = '50000000-0000-0000-0000-000000000052';
const mayorContestId = '60000000-0000-0000-0000-000000000052';
const councilContestId = '70000000-0000-0000-0000-000000000052';
const kimCandidateId = '80000000-0000-0000-0000-000000000052';
const parkCandidateId = '80000000-0000-0000-0000-000000000053';
const leeCandidateId = '90000000-0000-0000-0000-000000000052';
const choiCandidateId = '90000000-0000-0000-0000-000000000053';

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['VOTER'],
  electionScope: [electionId],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'VOTER',
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

beforeAll(() => {
  Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
    configurable: true,
    value: vi.fn(() => ({
      clearRect: vi.fn(),
      fillText: vi.fn(),
      getImageData: vi.fn(() => ({ data: new Uint8ClampedArray(4) })),
      measureText: vi.fn(() => ({ width: 0 })),
    })),
  });
});

function mockVotingSessionApis() {
  server.use(
    http.get('/miremsplatform/elections/:electionId/ballots/:ballotId/preview', ({ request, params }) => {
      expectBearerAuth(request);
      expect(params).toMatchObject({ electionId, ballotId });
      return HttpResponse.json({
        ballotId,
        layout: {
          title: '제8회 서울시장 선거',
          instructions: '각 경합의 선택지를 확인한 뒤 검토 단계로 이동하세요.',
          contests: [
            {
              id: mayorContestId,
              title: '서울시장',
              type: 'single',
              options: [
                { id: kimCandidateId, label: '김미래' },
                { id: parkCandidateId, label: '박정책' },
              ],
            },
            {
              id: councilContestId,
              title: '시의원 비례대표',
              type: 'multi',
              maxSelections: 2,
              options: [
                { id: leeCandidateId, label: '이투명' },
                { id: choiCandidateId, label: '최감사' },
              ],
            },
          ],
        },
      });
    }),
    http.post('/miremsplatform/sessions', async ({ request }) => {
      expectBearerAuth(request);
      const body = await request.json() as Record<string, unknown>;
      expect(body).toEqual({ voterId, electionId, ballotStyleId, deviceId: 'KIOSK-01' });
      return HttpResponse.json({ id: sessionId, voterId, electionId, ballotStyleId, status: 'OPENED' }, { status: 201 });
    }),
  );
}

describe('Voting session kiosk page', () => {
  it('starts a kiosk session, renders accessible ballot controls, reviews selections, and casts a vote', async () => {
    mockVotingSessionApis();
    server.use(
      http.post('/miremsplatform/sessions/:sessionId/cast', async ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.sessionId).toBe(sessionId);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toEqual({
          selections: [
            { contestId: mayorContestId, selectionIds: [kimCandidateId] },
            { contestId: councilContestId, selectionIds: [leeCandidateId, choiCandidateId] },
          ],
        });
        return HttpResponse.json({ sessionId, resultHashes: ['result-hash-1'], receiptHash: 'receipt-hash-052' });
      }),
    );

    const { container } = renderWithAuth(<VotingSessionPage initialDeviceId="KIOSK-01" />);

    fireEvent.change(screen.getByLabelText('선거인 ID'), { target: { value: voterId } });
    fireEvent.change(screen.getByLabelText('선거 ID'), { target: { value: electionId } });
    fireEvent.change(screen.getByLabelText('투표용지 ID'), { target: { value: ballotId } });
    fireEvent.change(screen.getByLabelText('BallotStyle ID'), { target: { value: ballotStyleId } });
    fireEvent.click(screen.getByRole('button', { name: '투표 시작' }));

    expect(await screen.findByRole('heading', { name: '제8회 서울시장 선거' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'VVSG 접근성 도구' })).toBeInTheDocument();
    const accessibility = await axe.run(container);
    expect(accessibility.violations).toHaveLength(0);

    fireEvent.click(screen.getByLabelText('김미래'));
    fireEvent.click(screen.getByLabelText('이투명'));
    fireEvent.click(screen.getByLabelText('최감사'));
    fireEvent.click(screen.getByRole('button', { name: '선택 검토' }));

    const review = await screen.findByRole('region', { name: '투표 검토' });
    expect(within(review).getByText('서울시장: 김미래')).toBeInTheDocument();
    expect(within(review).getByText('시의원 비례대표: 이투명, 최감사')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '투표 제출' }));

    expect(await screen.findByText('영수증 해시: receipt-hash-052')).toBeInTheDocument();
    expect(screen.getByText('결과 해시: result-hash-1')).toBeInTheDocument();
  });

  it('requires one selection per contest before review', async () => {
    mockVotingSessionApis();
    renderWithAuth(<VotingSessionPage initialDeviceId="KIOSK-01" />);

    fireEvent.change(screen.getByLabelText('선거인 ID'), { target: { value: voterId } });
    fireEvent.change(screen.getByLabelText('선거 ID'), { target: { value: electionId } });
    fireEvent.change(screen.getByLabelText('투표용지 ID'), { target: { value: ballotId } });
    fireEvent.change(screen.getByLabelText('BallotStyle ID'), { target: { value: ballotStyleId } });
    fireEvent.click(screen.getByRole('button', { name: '투표 시작' }));

    expect(await screen.findByRole('heading', { name: '제8회 서울시장 선거' })).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('김미래'));
    fireEvent.click(screen.getByRole('button', { name: '선택 검토' }));

    expect(await screen.findByText('모든 경합에서 최소 1개 선택이 필요합니다.')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: '투표 검토' })).not.toBeInTheDocument();
  });

  it('spoils a voting session only after confirmation', async () => {
    mockVotingSessionApis();
    server.use(
      http.post('/miremsplatform/sessions/:sessionId/spoil', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.sessionId).toBe(sessionId);
        return HttpResponse.json({ id: sessionId, voterId, electionId, ballotStyleId, status: 'SPOILED' });
      }),
    );

    renderWithAuth(<VotingSessionPage initialDeviceId="KIOSK-01" />);

    fireEvent.change(screen.getByLabelText('선거인 ID'), { target: { value: voterId } });
    fireEvent.change(screen.getByLabelText('선거 ID'), { target: { value: electionId } });
    fireEvent.change(screen.getByLabelText('투표용지 ID'), { target: { value: ballotId } });
    fireEvent.change(screen.getByLabelText('BallotStyle ID'), { target: { value: ballotStyleId } });
    fireEvent.click(screen.getByRole('button', { name: '투표 시작' }));

    expect(await screen.findByRole('heading', { name: '제8회 서울시장 선거' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '투표 무효 처리' }));
    expect(screen.getByText('이 투표 세션을 무효 처리할까요?')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '무효 처리 확정' }));

    expect(await screen.findByText('투표 세션이 무효 처리되었습니다.')).toBeInTheDocument();
  });

  it('blocks kiosk access for users without voter role before calling session APIs', () => {
    renderWithAuth(<VotingSessionPage initialDeviceId="KIOSK-01" />, {
      ...authBase,
      roles: ['ELECTION_OFFICER'],
      hasRole: (role) => role === 'ELECTION_OFFICER',
    });

    expect(screen.getByText('투표 세션은 인증된 선거인에게만 허용됩니다.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '투표 시작' })).not.toBeInTheDocument();
  });
});
