import React from 'react';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { I18nProvider } from './i18n/I18nProvider';
import { resetShellLanguageForTests } from './i18n/i18n';
import { ShellThemeProvider } from './theme/shell-theme';
import { ShellChrome } from './ShellLayout';
import type { AuthContextValue } from './auth/AuthProvider';

const authBase: AuthContextValue = {
  user: null,
  roles: ['SYSTEM_ADMIN'],
  electionScope: [],
  isAuthenticated: true,
  isLoading: false,
  login: vi.fn(),
  logout: vi.fn(),
  hasRole: (role) => role === 'SYSTEM_ADMIN',
  hasElectionScope: () => true,
};

function setViewportWidth(width: number) {
  Object.defineProperty(window, 'innerWidth', { configurable: true, writable: true, value: width });
  window.dispatchEvent(new Event('resize'));
}

function renderShellChrome(width: number) {
  setViewportWidth(width);
  return render(
    <I18nProvider>
      <ShellThemeProvider>
        <ShellChrome auth={authBase} currentPath="/miremsplatform/admin">
          <h1>본문 패널</h1>
        </ShellChrome>
      </ShellThemeProvider>
    </I18nProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
  resetShellLanguageForTests();
});

describe('ShellChrome responsive layout', () => {
  it('renders desktop layout with sidebar navigation at wide breakpoints', () => {
    const { container } = renderShellChrome(1280);

    expect(container.querySelector('.app-shell')).toHaveAttribute('data-layout', 'desktop');
    expect(screen.getByLabelText('주요 내비게이션')).toHaveAttribute('data-navigation-placement', 'sidebar');
    expect(screen.queryByLabelText('모바일 하단 내비게이션')).not.toBeInTheDocument();
  });

  it('renders mobile layout with bottom navigation at narrow breakpoints', () => {
    const { container } = renderShellChrome(480);

    expect(container.querySelector('.app-shell')).toHaveAttribute('data-layout', 'mobile');
    expect(screen.getByLabelText('모바일 하단 내비게이션')).toHaveAttribute('data-navigation-placement', 'bottom');
    expect(screen.getByRole('group', { name: '언어 선택' })).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveTextContent('본문 패널');
  });

  it('renders tablet layout between mobile and desktop breakpoints', () => {
    const { container } = renderShellChrome(820);

    expect(container.querySelector('.app-shell')).toHaveAttribute('data-layout', 'tablet');
    expect(screen.getByLabelText('모바일 하단 내비게이션')).toHaveAttribute('data-navigation-placement', 'bottom');
  });

  it('keeps compact header controls available instead of hiding login or language controls', () => {
    const cssPath = join(process.cwd(), 'src/styles.css');
    const css = readFileSync(cssPath, 'utf8');

    expect(css).not.toMatch(/\.language-switcher\s*\{\s*display:\s*none/i);
    expect(css).not.toMatch(/\.language-switcher[^{}]*\{[^{}]*display:\s*none/i);
    expect(css).not.toMatch(/\.user-menu[^{}]*display:\s*none/i);
    expect(css).toContain('.user-menu__label');
  });

  it('defines phone-sized responsive rules for dense forms, grids, tables, and action bars', () => {
    const cssPath = join(process.cwd(), 'src/styles.css');
    const css = readFileSync(cssPath, 'utf8');

    expect(css).toContain('@media (max-width: 480px)');
    expect(css).toContain('.topbar-actions { flex-wrap: wrap; justify-content: flex-start; width: 100%; }');
    expect(css).toContain('.user-menu__label { display: none; }');
    expect(css).toContain('@media (max-width: 640px)');
    expect(css).toContain('.form-grid,');
    expect(css).toContain('.filters-grid,');
    expect(css).toContain('.ballot-preview-grid');
    expect(css).toContain('.wizard-actions,');
    expect(css).toContain('.page-actions,');
    expect(css).toContain('grid-template-columns: minmax(0, 1fr);');
    expect(css).toContain('min-width: 520px;');
  });
});
