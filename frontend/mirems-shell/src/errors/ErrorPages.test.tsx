import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../auth/AuthProvider';
import { I18nProvider } from '../i18n/I18nProvider';
import { resetShellLanguageForTests } from '../i18n/i18n';
import { server } from '../test/msw-server';
import { AppErrorBoundary, RouteErrorPage, NotFoundPage } from './ErrorPages';

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['SYSTEM_ADMIN'],
  electionScope: [],
  isAuthenticated: true,
  isLoading: false,
  login: vi.fn(),
  logout: vi.fn(),
  hasRole: (role) => role === 'SYSTEM_ADMIN',
  hasElectionScope: () => true,
};

const authWithoutToken: AuthContextValue = {
  ...authBase,
  user: null,
};

function renderWithProviders(ui: React.ReactElement, auth: AuthContextValue = authBase) {
  return render(
    <ThemeProvider>
      <I18nProvider>
        <AuthContext.Provider value={auth}>{ui}</AuthContext.Provider>
      </I18nProvider>
    </ThemeProvider>,
  );
}

function BrokenWidget(): never {
  throw new Error('Render exploded');
}

afterEach(() => {
  resetShellLanguageForTests();
  vi.restoreAllMocks();
});

describe('MiREMS frontend error handling', () => {
  it('catches render errors, offers recovery actions, and reports to the audit endpoint', async () => {
    let reportCalls = 0;
    server.use(
      http.post('/miremsplatform/audit/frontend-errors', async ({ request }) => {
        reportCalls += 1;
        expect(request.headers.get('authorization')).toBe('Bearer test-access-token');
        expect(await request.json()).toMatchObject({ eventType: 'FRONTEND_RENDER_ERROR', message: 'Render exploded' });
        return HttpResponse.json({ accepted: true }, { status: 202 });
      }),
    );

    renderWithProviders(
      <AppErrorBoundary path="/miremsplatform/admin?access_token=secret#token=secret">
        <BrokenWidget />
      </AppErrorBoundary>,
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('화면을 표시하는 중 문제가 발생했습니다');
    expect(screen.getByText('/miremsplatform/admin')).toBeInTheDocument();
    expect(screen.queryByText(/access_token|secret/)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '홈으로 이동' })).toHaveAttribute('href', '/miremsplatform');
    expect(screen.getByRole('link', { name: '오류 신고' })).toHaveAttribute('href', 'mailto:support@mirems.local?subject=MiREMS%20frontend%20error');
    await waitFor(() => expect(reportCalls).toBe(1));
  });

  it('resets the boundary when retry is selected', async () => {
    const FlakyWidget = () => {
      const [shouldThrow, setShouldThrow] = React.useState(true);
      if (shouldThrow) {
        return <button type="button" onClick={() => setShouldThrow(false)}>문제 해결</button>;
      }
      return <span>복구된 화면</span>;
    };

    function ThrowAfterClick() {
      const [throwNow, setThrowNow] = React.useState(false);
      if (throwNow) {
        throw new Error('temporary render failure');
      }
      return <button type="button" onClick={() => setThrowNow(true)}>장애 발생</button>;
    }

    renderWithProviders(
      <AppErrorBoundary path="/miremsplatform/audit">
        <ThrowAfterClick />
        <FlakyWidget />
      </AppErrorBoundary>,
      authWithoutToken,
    );

    fireEvent.click(screen.getByRole('button', { name: '장애 발생' }));
    expect(await screen.findByRole('button', { name: '다시 시도' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }));

    expect(await screen.findByRole('button', { name: '장애 발생' })).toBeInTheDocument();
  });

  it('renders route-level error pages with the same recovery actions', () => {
    renderWithProviders(<RouteErrorPage error={new Error('route failed')} reset={vi.fn()} />, authWithoutToken);

    expect(screen.getByRole('alert')).toHaveTextContent('화면을 표시하는 중 문제가 발생했습니다');
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '홈으로 이동' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '오류 신고' })).toBeInTheDocument();
  });

  it('renders an accessible 404 page with a home recovery link', () => {
    renderWithProviders(<NotFoundPage />);

    expect(screen.getByRole('heading', { name: '페이지를 찾을 수 없습니다' })).toBeInTheDocument();
    expect(screen.getByText('요청한 MiREMS 화면이 존재하지 않거나 권한 범위 밖에 있습니다.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '홈으로 이동' })).toHaveAttribute('href', '/miremsplatform');
  });
});
