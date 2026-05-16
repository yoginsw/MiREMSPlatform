import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { AdminDashboardPage } from './admin-dashboard-page';

const healthPayload = {
  status: 'UP',
  components: {
    db: { status: 'UP', details: { database: 'PostgreSQL', validationQuery: 'isValid()' } },
    kafka: { status: 'DOWN', details: { clusterId: 'dev-kafka', error: 'broker unavailable' } },
    kogito: { status: 'UP', details: { runtime: 'Kogito 10.2.0' } },
  },
};

const processPayload = [
  {
    instanceId: 'proc-tabulation-1',
    processId: 'tabulationWorkflow',
    status: 'ACTIVE',
    variables: { electionId: 'election-2026', phase: 'review', accessToken: 'sensitive-token-value' },
    activeNodes: ['ReviewTabulation'],
  },
  {
    instanceId: 'proc-certification-2',
    processId: 'resultCertificationWorkflow',
    status: 'ACTIVE',
    variables: { electionId: 'election-2026', phase: 'legal-review' },
    activeNodes: ['LegalReview'],
  },
];

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['SYSTEM_ADMIN'],
  electionScope: [],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'SYSTEM_ADMIN',
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

function mockAdminApis() {
  server.use(
    http.get('/miremsplatform/actuator/health', ({ request }) => {
      expectBearerAuth(request);
      return HttpResponse.json(healthPayload);
    }),
    http.get('/miremsplatform/admin/processes', ({ request }) => {
      expectBearerAuth(request);
      return HttpResponse.json(processPayload);
    }),
  );
}

describe('AdminDashboardPage', () => {
  it('renders DB, Kafka, and Kogito health cards with loaded extension packs', async () => {
    mockAdminApis();

    renderWithAuth(<AdminDashboardPage />);

    expect(await screen.findByRole('heading', { name: '관리자 대시보드' })).toBeInTheDocument();
    expect(within(screen.getByTestId('health-db')).getByText('UP')).toBeInTheDocument();
    expect(within(screen.getByTestId('health-kafka')).getByText('DOWN')).toBeInTheDocument();
    expect(within(screen.getByTestId('health-kogito')).getByText('UP')).toBeInTheDocument();
    expect(screen.getByText('@mirems/ext-kr-ui')).toBeInTheDocument();
    expect(screen.getByText('@mirems/ext-us-ui')).toBeInTheDocument();
  });

  it('lists active process instances and sends a manual signal with Authorization', async () => {
    let signalPayload: unknown;
    mockAdminApis();
    server.use(
      http.post('/miremsplatform/admin/processes/:processInstanceId/signal', async ({ params, request }) => {
        expect(params.processInstanceId).toBe('proc-tabulation-1');
        expectBearerAuth(request);
        signalPayload = await request.json();
        return HttpResponse.json({
          ...processPayload[0],
          status: 'ACTIVE',
          activeNodes: ['SignalReceived'],
          variables: { electionId: 'election-2026', lastSignal: 'continueReview' },
        });
      }),
    );

    renderWithAuth(<AdminDashboardPage />);

    const row = await screen.findByRole('row', { name: /tabulationWorkflow/ });
    expect(within(row).getByText('ReviewTabulation')).toBeInTheDocument();
    expect(within(row).getByText(/\[REDACTED\]/)).toBeInTheDocument();
    expect(screen.queryByText('sensitive-token-value')).not.toBeInTheDocument();
    fireEvent.change(within(row).getByLabelText('시그널 이름'), { target: { value: 'continueReview' } });
    fireEvent.change(within(row).getByLabelText('시그널 JSON payload'), { target: { value: '{"approved":true}' } });
    fireEvent.click(within(row).getByRole('button', { name: '시그널 전송' }));

    await waitFor(() => expect(signalPayload).toEqual({ signalName: 'continueReview', payload: { approved: true } }));
    expect(await screen.findByText('프로세스 proc-tabulation-1에 시그널을 전송했습니다.')).toBeInTheDocument();
  });

  it('rejects invalid and non-object signal JSON before calling the signal endpoint', async () => {
    let signalCalls = 0;
    mockAdminApis();
    server.use(
      http.post('/miremsplatform/admin/processes/:processInstanceId/signal', () => {
        signalCalls += 1;
        return HttpResponse.json(processPayload[0]);
      }),
    );

    renderWithAuth(<AdminDashboardPage />);

    const row = await screen.findByRole('row', { name: /tabulationWorkflow/ });
    fireEvent.change(within(row).getByLabelText('시그널 이름'), { target: { value: 'continueReview' } });
    fireEvent.change(within(row).getByLabelText('시그널 JSON payload'), { target: { value: '{bad-json' } });
    fireEvent.click(within(row).getByRole('button', { name: '시그널 전송' }));

    expect(await screen.findByText('시그널 payload는 올바른 JSON이어야 합니다.')).toBeInTheDocument();
    expect(signalCalls).toBe(0);

    fireEvent.change(within(row).getByLabelText('시그널 JSON payload'), { target: { value: '[]' } });
    fireEvent.click(within(row).getByRole('button', { name: '시그널 전송' }));

    expect(await screen.findByText('시그널 payload는 JSON 객체여야 합니다.')).toBeInTheDocument();
    expect(signalCalls).toBe(0);
  });

  it('blocks non-SYSTEM_ADMIN roles before health or process API calls', async () => {
    let healthCalls = 0;
    let processCalls = 0;
    server.use(
      http.get('/miremsplatform/actuator/health', () => {
        healthCalls += 1;
        return HttpResponse.json(healthPayload);
      }),
      http.get('/miremsplatform/admin/processes', () => {
        processCalls += 1;
        return HttpResponse.json(processPayload);
      }),
    );

    renderWithAuth(<AdminDashboardPage />, {
      ...authBase,
      roles: ['AUDITOR'],
      hasRole: () => false,
    });

    expect(screen.getByText('관리자 대시보드는 SYSTEM_ADMIN 권한이 필요합니다.')).toBeInTheDocument();
    await waitFor(() => {
      expect(healthCalls).toBe(0);
      expect(processCalls).toBe(0);
    });
  });
});
