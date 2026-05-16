import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { BallotPreviewPage, BallotStyleManagementPage } from './ballot-pages';

const electionId = 'el-2028-kr-parliamentary';
const ballotId = 'ballot-001';

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['ELECTION_ADMIN'],
  electionScope: [electionId],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_ADMIN',
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

describe('Ballot style management pages', () => {
  it('lists ballot styles grouped by ballot version', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/ballots', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.electionId).toBe(electionId);
        return HttpResponse.json([
          {
            id: ballotId,
            electionId,
            ballotVersion: 3,
            active: true,
            contests: [{ contestId: 'contest-national', displayOrder: 1, presentationTitle: '비례대표 국회의원' }],
            styles: [],
          },
        ]);
      }),
      http.get('/miremsplatform/elections/:electionId/ballot-styles', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json([
          { id: 'style-kr-seoul', ballotId, styleCode: 'KR-SEOUL', district: '서울특별시', language: 'ko', accessibilityFeatures: ['LARGE_TEXT', 'SCREEN_READER'] },
        ]);
      }),
    );

    renderWithAuth(<BallotStyleManagementPage electionId={electionId} />);

    expect(await screen.findByRole('heading', { name: 'Ballot v3 · 활성' })).toBeInTheDocument();
    expect(screen.getByText('KR-SEOUL')).toBeInTheDocument();
    expect(screen.getByText('서울특별시')).toBeInTheDocument();
    expect(screen.getByText('ko')).toBeInTheDocument();
    expect(screen.getByText('LARGE_TEXT, SCREEN_READER')).toBeInTheDocument();
  });

  it('creates a ballot style with accessibility features through generated API client', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/ballots', () => HttpResponse.json([
        { id: ballotId, electionId, ballotVersion: 1, active: true, contests: [], styles: [] },
      ])),
      http.get('/miremsplatform/elections/:electionId/ballot-styles', () => HttpResponse.json([])),
      http.post('/miremsplatform/elections/:electionId/ballot-styles', async ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.electionId).toBe(electionId);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toMatchObject({ ballotId, styleCode: 'KR-BUSAN', district: '부산광역시', language: 'ko' });
        expect(body.accessibilityFeatures).toEqual(['AUDIO', 'HIGH_CONTRAST']);
        return HttpResponse.json({ id: 'style-busan', ...body }, { status: 201 });
      }),
    );

    renderWithAuth(<BallotStyleManagementPage electionId={electionId} />);

    fireEvent.change(await screen.findByLabelText('투표용지 버전'), { target: { value: ballotId } });
    fireEvent.change(screen.getByLabelText('스타일 코드'), { target: { value: 'KR-BUSAN' } });
    fireEvent.change(screen.getByLabelText('관할 구역'), { target: { value: '부산광역시' } });
    fireEvent.change(screen.getByLabelText('언어 코드'), { target: { value: 'ko' } });
    fireEvent.click(screen.getByLabelText('오디오 안내'));
    fireEvent.click(screen.getByLabelText('고대비'));
    fireEvent.click(screen.getByRole('button', { name: 'BallotStyle 생성' }));

    expect(await screen.findByText('BallotStyle이 저장되었습니다.')).toBeInTheDocument();
    expect(screen.getByText('KR-BUSAN')).toBeInTheDocument();
  });

  it('edits an existing ballot style and uses PUT with bearer auth', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/ballots', () => HttpResponse.json([
        { id: ballotId, electionId, ballotVersion: 2, active: true, contests: [], styles: [] },
      ])),
      http.get('/miremsplatform/elections/:electionId/ballot-styles', () => HttpResponse.json([
        { id: 'style-edit', ballotId, styleCode: 'KR-OLD', district: '기존 구역', language: 'ko', accessibilityFeatures: ['LARGE_TEXT'] },
      ])),
      http.put('/miremsplatform/elections/:electionId/ballot-styles/style-edit', async ({ request }) => {
        expectBearerAuth(request);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toMatchObject({ ballotId, styleCode: 'KR-NEW', district: '수정 구역', language: 'en' });
        return HttpResponse.json({ id: 'style-edit', ...body });
      }),
    );

    renderWithAuth(<BallotStyleManagementPage electionId={electionId} />);

    const row = await screen.findByRole('row', { name: /KR-OLD/ });
    fireEvent.click(within(row).getByRole('button', { name: '편집' }));
    fireEvent.change(screen.getByLabelText('스타일 코드'), { target: { value: 'KR-NEW' } });
    fireEvent.change(screen.getByLabelText('관할 구역'), { target: { value: '수정 구역' } });
    fireEvent.change(screen.getByLabelText('언어 코드'), { target: { value: 'en' } });
    fireEvent.click(screen.getByRole('button', { name: 'BallotStyle 수정' }));

    expect(await screen.findByText('BallotStyle이 저장되었습니다.')).toBeInTheDocument();
    expect(screen.getByText('KR-NEW')).toBeInTheDocument();
  });

  it('renders a KR vertical party-list ballot preview with party logos and an election calendar widget', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/ballots/:ballotId/preview', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params).toMatchObject({ electionId, ballotId });
        return HttpResponse.json({
          ballotId,
          layout: {
            variant: 'KR_PARTY_LIST',
            title: '제22대 국회의원선거 비례대표 투표용지',
            instructions: '정당명부 중 하나의 정당만 선택하세요.',
            electionDay: '2028-04-12',
            parties: [
              { id: 'party-citizen', displayOrder: 1, name: '시민당', logoUri: '/logos/citizen.svg' },
              { id: 'party-green', displayOrder: 2, name: '녹색당', logoUri: '/logos/green.svg' },
            ],
          },
        });
      }),
    );

    renderWithAuth(<BallotPreviewPage electionId={electionId} ballotId={ballotId} />);

    expect(await screen.findByRole('heading', { name: '제22대 국회의원선거 비례대표 투표용지' })).toBeInTheDocument();
    expect(screen.getByText('정당명부 중 하나의 정당만 선택하세요.')).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: '비례대표 정당명부' })).toHaveClass('kr-party-list');
    const citizenParty = screen.getByRole('radio', { name: /시민당/ });
    expect(citizenParty).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '시민당 로고' })).toHaveAttribute('src', '/logos/citizen.svg');
    expect(screen.getAllByText('기표란')).toHaveLength(2);
    expect(screen.getByRole('region', { name: '대한민국 선거 일정' })).toBeInTheDocument();
    expect(screen.getByText('사전투표 시작')).toBeInTheDocument();
    expect(screen.getByText('2028-04-07')).toBeInTheDocument();
    expect(screen.getByText('사전투표 종료')).toBeInTheDocument();
    expect(screen.getByText('2028-04-08')).toBeInTheDocument();
    expect(screen.getByText('선거일')).toBeInTheDocument();
    expect(screen.getByText('2028-04-12')).toBeInTheDocument();
  });

  it('renders a visual ballot preview from ballot layout JSON', async () => {
    server.use(
      http.get('/miremsplatform/elections/:electionId/ballots/:ballotId/preview', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params).toMatchObject({ electionId, ballotId });
        return HttpResponse.json({
          ballotId,
          layout: {
            title: '제22대 국회의원선거 투표용지',
            instructions: '한 명의 후보자 또는 정당을 선택하세요.',
            contests: [
              { title: '비례대표 국회의원', options: ['미래당', '시민당', '녹색당'] },
              { title: '지역구 국회의원', options: ['김후보', '이후보'] },
            ],
          },
        });
      }),
    );

    renderWithAuth(<BallotPreviewPage electionId={electionId} ballotId={ballotId} />);

    expect(await screen.findByRole('heading', { name: '제22대 국회의원선거 투표용지' })).toBeInTheDocument();
    expect(screen.getByText('한 명의 후보자 또는 정당을 선택하세요.')).toBeInTheDocument();
    expect(screen.getByRole('group', { name: '비례대표 국회의원' })).toBeInTheDocument();
    expect(screen.getByText('미래당')).toBeInTheDocument();
    expect(screen.getByText('김후보')).toBeInTheDocument();
  });

  it('blocks ballot preview outside election admin scope', () => {
    renderWithAuth(<BallotPreviewPage electionId={electionId} ballotId={ballotId} />, {
      ...authBase,
      roles: ['ELECTION_OFFICER'],
      electionScope: ['another-election'],
      hasRole: () => false,
      hasElectionScope: () => false,
    });

    expect(screen.queryByText('투표용지 미리보기를 불러오는 중입니다.')).not.toBeInTheDocument();
    expect(screen.getByText('투표용지 미리보기는 관할 선거 관리자에게만 허용됩니다.')).toBeInTheDocument();
  });

  it('blocks ballot style management outside election admin scope', () => {
    renderWithAuth(<BallotStyleManagementPage electionId={electionId} />, {
      ...authBase,
      electionScope: ['another-election'],
      hasElectionScope: () => false,
    });

    expect(screen.queryByRole('heading', { name: 'BallotStyle 관리' })).not.toBeInTheDocument();
    expect(screen.getByText('투표용지 스타일 관리는 관할 선거 관리자에게만 허용됩니다.')).toBeInTheDocument();
  });
});
