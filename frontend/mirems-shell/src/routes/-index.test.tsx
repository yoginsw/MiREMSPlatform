import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';

vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  const ReactModule = await import('react');

  return {
    ...actual,
    Link: ({
      to,
      params,
      children,
      className,
    }: {
      to: string;
      params?: Record<string, string>;
      children: React.ReactNode;
      className?: string;
    }) => {
      const href = Object.entries(params ?? {}).reduce(
        (path, [key, value]) => path.replace(`$${key}`, value),
        to,
      );
      return ReactModule.createElement('a', { className, href, 'data-router-link': 'true' }, children);
    },
  };
});
import { AuthContext, type AuthContextValue } from '../auth/AuthProvider';
import { I18nProvider } from '../i18n/I18nProvider';
import { resetShellLanguageForTests } from '../i18n/i18n';
import { ShellThemeProvider } from '../theme/shell-theme';
import { LandingPage } from './index';

const unauthenticatedAuth: AuthContextValue = {
  user: null,
  roles: [],
  electionScope: [],
  isAuthenticated: false,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: () => false,
  hasElectionScope: () => false,
};

const authenticatedAuth: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: { preferred_username: 'ks.jang' } } as AuthContextValue['user'],
  roles: ['ELECTION_ADMIN'],
  electionScope: ['*'],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_ADMIN' || role === 'OBSERVER',
  hasElectionScope: () => true,
};

function renderLandingPage(auth: AuthContextValue = unauthenticatedAuth) {
  return render(
    <ThemeProvider>
      <AuthContext.Provider value={auth}>
        <I18nProvider>
          <ShellThemeProvider>
            <LandingPage />
          </ShellThemeProvider>
        </I18nProvider>
      </AuthContext.Provider>
    </ThemeProvider>,
  );
}

const stylesCss = readFileSync(join(process.cwd(), 'src/styles.css'), 'utf8');

afterEach(() => {
  resetShellLanguageForTests();
});

describe('MiREMS public landing page', () => {
  it('presents a professional portal landing page with accessible public controls', () => {
    renderLandingPage();

    expect(screen.getByRole('banner', { name: 'MiREMS Portal 헤더' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: '언어 선택' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: '테마 선택' })).toBeInTheDocument();

    expect(screen.getByRole('heading', { level: 1, name: '신뢰 가능한 선거 운영을 위한 통합 관리 플랫폼' })).toBeInTheDocument();
    expect(screen.getByText(/선거 준비, 후보자·선거인 관리, 투표 운영, 개표·결과 공표/)).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: '운영자 로그인' }).map((link) => link.getAttribute('href'))).toContain('/login');
    expect(screen.getByRole('link', { name: '공식 결과 보기' })).toHaveAttribute('href', '/elections/current/results');
    expect(screen.getByRole('link', { name: '공식 결과 보기' })).toHaveAttribute('data-router-link', 'true');

    const features = screen.getByRole('region', { name: '핵심 역량' });
    expect(within(features).getByText('전 과정 선거 워크플로')).toBeInTheDocument();
    expect(within(features).getByText('감사 가능성과 추적성')).toBeInTheDocument();
    expect(within(features).getByText('글로벌 확장 아키텍처')).toBeInTheDocument();

    const trustSection = screen.getByRole('region', { name: '신뢰 기준' });
    expect(within(trustSection).getByText('VVSG 2.0 aligned')).toBeInTheDocument();
    expect(within(trustSection).getByText('WCAG AA oriented')).toBeInTheDocument();
    expect(within(trustSection).getByText('Role-based access')).toBeInTheDocument();
  });

  it('offers authenticated operators a direct operations console link instead of another login prompt', () => {
    renderLandingPage(authenticatedAuth);

    expect(screen.queryByRole('link', { name: '운영자 로그인' })).not.toBeInTheDocument();
    const consoleLinks = screen.getAllByRole('link', { name: '운영 콘솔로 이동' });
    expect(consoleLinks.map((link) => link.getAttribute('href'))).toContain('/elections');
    expect(consoleLinks.every((link) => link.getAttribute('data-router-link') === 'true')).toBe(true);
    expect(screen.getByText('ks.jang 계정으로 로그인되어 있습니다.')).toBeInTheDocument();
  });

  it('switches landing copy to English and applies theme styling in memory', async () => {
    renderLandingPage();

    fireEvent.click(screen.getByRole('button', { name: 'English' }));

    expect(await screen.findByRole('banner', { name: 'MiREMS Portal header' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'An integrated election management platform for trusted electoral operations' })).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Operator sign in' }).map((link) => link.getAttribute('href'))).toContain('/login');
    expect(screen.getByText('End-to-end election workflow')).toBeInTheDocument();

    const landingRoot = screen.getByTestId('landing-page');
    expect(landingRoot).toHaveAttribute('data-theme', 'light');

    fireEvent.click(screen.getByRole('button', { name: 'High contrast' }));
    expect(landingRoot).toHaveAttribute('data-theme', 'high-contrast');
  });

  it('keeps inactive landing header language and theme buttons readable in light and dark themes', () => {
    expect(stylesCss).toContain('.landing-header .theme-switcher__button,');
    expect(stylesCss).toContain('.landing-header .language-switcher__button {');
    expect(stylesCss).toContain('background: var(--color-bg-surface-alt);');
    expect(stylesCss).toContain('color: var(--color-text-primary);');
    expect(stylesCss).toContain('border-color: var(--color-border-strong);');
    expect(stylesCss).toContain('.landing-header .theme-switcher__button[aria-pressed="true"],');
    expect(stylesCss).toContain('color: var(--color-text-inverse);');
    expect(stylesCss).toContain('.landing-page[data-theme="dark"] .landing-header .theme-switcher__button[aria-pressed="true"],');
    expect(stylesCss).toContain('color: var(--color-navy-950);');
  });
});
