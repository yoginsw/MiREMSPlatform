import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext, type AuthContextValue } from './AuthProvider';
import { LoginPage } from './LoginPage';

const authenticatedAuth: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: { preferred_username: 'ks.jang' } } as AuthContextValue['user'],
  roles: ['ELECTION_ADMIN'],
  electionScope: ['*'],
  isAuthenticated: true,
  isLoading: false,
  login: vi.fn(async () => undefined),
  logout: vi.fn(async () => undefined),
  hasRole: (role) => role === 'ELECTION_ADMIN',
  hasElectionScope: () => true,
};

function renderLoginPage(auth: AuthContextValue = authenticatedAuth) {
  return render(
    <AuthContext.Provider value={auth}>
      <LoginPage />
    </AuthContext.Provider>,
  );
}

describe('LoginPage', () => {
  it('sends already authenticated operators to the protected operations console by default', () => {
    window.history.replaceState(null, '', '/miremsplatform/login');

    renderLoginPage();

    expect(screen.getByRole('heading', { name: '이미 로그인되어 있습니다' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '운영 화면으로 이동' })).toHaveAttribute('href', '/miremsplatform/elections');
  });
});