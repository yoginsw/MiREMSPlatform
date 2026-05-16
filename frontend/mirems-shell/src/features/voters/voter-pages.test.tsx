import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { VoterEligibilityCheckPage, VoterRegistrationPage, VoterRollAdminPage } from './voter-pages';

const voterId = '10000000-0000-0000-0000-000000000033';
const electionId = '20000000-0000-0000-0000-000000000033';
const externalReference = 'KR-SEOUL-19790101-1234567';

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['ELECTION_OFFICER'],
  electionScope: [electionId],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_OFFICER',
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

describe('Voter roll management pages', () => {
  it('registers a voter with eligible election IDs and never displays the raw external reference after submit', async () => {
    server.use(
      http.post('/miremsplatform/voters', async ({ request }) => {
        expectBearerAuth(request);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toEqual({
          externalVoterReference: externalReference,
          jurisdiction: 'KR-SEOUL',
          eligibleElectionIds: [electionId, '20000000-0000-0000-0000-000000000034'],
        });
        return HttpResponse.json({ id: voterId, maskedReference: '*****************4567', registrationStatus: 'ACTIVE' }, { status: 201 });
      }),
    );

    renderWithAuth(<VoterRegistrationPage />);

    fireEvent.change(screen.getByLabelText('외부 선거인 참조'), { target: { value: externalReference } });
    fireEvent.change(screen.getByLabelText('관할 구역'), { target: { value: 'KR-SEOUL' } });
    fireEvent.change(screen.getByLabelText('대상 선거 ID 목록'), { target: { value: `${electionId}\n20000000-0000-0000-0000-000000000034` } });
    fireEvent.click(screen.getByRole('button', { name: '선거인 등록' }));

    expect(await screen.findByText(/선거인 등록이 완료되었습니다/)).toBeInTheDocument();
    expect(screen.getByText(/\*{17}4567/)).toBeInTheDocument();
    expect(screen.queryByText(externalReference)).not.toBeInTheDocument();
  });

  it('checks voter eligibility for an election and shows the decision reason', async () => {
    server.use(
      http.get('/miremsplatform/voters/:voterId/eligibility/:electionId', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params).toMatchObject({ voterId, electionId });
        return HttpResponse.json({ voterId, electionId, eligible: false, reason: 'Residency verification is missing' });
      }),
    );

    renderWithAuth(<VoterEligibilityCheckPage />);

    fireEvent.change(screen.getByLabelText('선거인 ID'), { target: { value: voterId } });
    fireEvent.change(screen.getByLabelText('선거 ID'), { target: { value: electionId } });
    fireEvent.click(screen.getByRole('button', { name: '자격 확인' }));

    expect(await screen.findByText('투표 불가')).toBeInTheDocument();
    expect(screen.getByText('Residency verification is missing')).toBeInTheDocument();
  });

  it('lets an election officer search the voter roll by ID with PII masking', async () => {
    server.use(
      http.get('/miremsplatform/voters/:voterId', ({ request, params }) => {
        expectBearerAuth(request);
        expect(params.voterId).toBe(voterId);
        return HttpResponse.json({ id: voterId, maskedReference: '*****************4567', registrationStatus: 'ACTIVE' });
      }),
    );

    renderWithAuth(<VoterRollAdminPage />);

    fireEvent.change(screen.getByLabelText('검색할 선거인 ID'), { target: { value: voterId } });
    fireEvent.click(screen.getByRole('button', { name: '선거인 검색' }));

    const row = await screen.findByRole('row', { name: /\*\*\*\*0033/ });
    expect(within(row).queryByText(voterId)).not.toBeInTheDocument();
    expect(within(row).getByText('****0033')).toBeInTheDocument();
    expect(within(row).getByText('*****************4567')).toBeInTheDocument();
    expect(within(row).getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.queryByText(externalReference)).not.toBeInTheDocument();
    expect(screen.getByText('원본 외부 선거인 참조는 표시하지 않고, API가 제공한 마스킹 값만 표시합니다.')).toBeInTheDocument();
  });

  it('runs an admin eligibility check from the searched voter row', async () => {
    server.use(
      http.get('/miremsplatform/voters/:voterId', () => HttpResponse.json({ id: voterId, maskedReference: '*****************4567', registrationStatus: 'ACTIVE' })),
      http.get('/miremsplatform/voters/:voterId/eligibility/:electionId', ({ request }) => {
        expectBearerAuth(request);
        return HttpResponse.json({ voterId, electionId, eligible: true, reason: 'Eligible for the selected election' });
      }),
    );

    renderWithAuth(<VoterRollAdminPage />);

    fireEvent.change(screen.getByLabelText('검색할 선거인 ID'), { target: { value: voterId } });
    fireEvent.click(screen.getByRole('button', { name: '선거인 검색' }));
    expect(await screen.findByText('*****************4567')).toBeInTheDocument();
    expect(screen.getByLabelText('선택된 선거인')).toHaveTextContent('****0033');
    fireEvent.change(screen.getByLabelText('자격 확인 선거 ID'), { target: { value: electionId } });
    fireEvent.click(screen.getByRole('button', { name: '관리자 자격 확인' }));

    expect(await screen.findByText('투표 가능')).toBeInTheDocument();
    expect(screen.getByText('Eligible for the selected election')).toBeInTheDocument();
  });

  it('blocks voter roll admin page for non-officers before calling protected APIs', () => {
    renderWithAuth(<VoterRollAdminPage />, {
      ...authBase,
      roles: ['VOTER'],
      hasRole: (role) => role === 'VOTER',
    });

    expect(screen.queryByRole('heading', { name: '선거인 명부 관리' })).not.toBeInTheDocument();
    expect(screen.getByText('선거인 명부 관리는 선거 사무원에게만 허용됩니다.')).toBeInTheDocument();
  });
});
