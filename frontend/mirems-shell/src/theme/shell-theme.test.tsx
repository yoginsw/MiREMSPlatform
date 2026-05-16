import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n/I18nProvider';
import { resetShellLanguageForTests } from '../i18n/i18n';
import { getResponsiveLayoutMode, ShellThemeProvider, ThemeSwitcher, useShellTheme } from './shell-theme';

function ThemeProbe() {
  const { theme } = useShellTheme();
  return <span data-testid="active-theme">{theme}</span>;
}

function renderThemeSwitcher() {
  return render(
    <I18nProvider>
      <ShellThemeProvider>
        <ThemeSwitcher />
        <ThemeProbe />
      </ShellThemeProvider>
    </I18nProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
  resetShellLanguageForTests();
});

describe('responsive shell theme helpers', () => {
  it('classifies desktop, tablet, and mobile breakpoints deterministically', () => {
    expect(getResponsiveLayoutMode(1200)).toBe('desktop');
    expect(getResponsiveLayoutMode(960)).toBe('tablet');
    expect(getResponsiveLayoutMode(640)).toBe('mobile');
  });

  it('switches light, dark, and high-contrast themes in memory without browser storage', () => {
    const localStorageSet = vi.spyOn(window.localStorage.__proto__, 'setItem');
    const localStorageGet = vi.spyOn(window.localStorage.__proto__, 'getItem');

    renderThemeSwitcher();

    expect(screen.getByRole('group', { name: '테마 선택' })).toBeInTheDocument();
    expect(screen.getByTestId('active-theme')).toHaveTextContent('light');
    expect(screen.getByRole('button', { name: '라이트' })).toHaveAttribute('aria-pressed', 'true');

    fireEvent.click(screen.getByRole('button', { name: '다크' }));
    expect(screen.getByTestId('active-theme')).toHaveTextContent('dark');
    expect(screen.getByRole('button', { name: '다크' })).toHaveAttribute('aria-pressed', 'true');

    fireEvent.click(screen.getByRole('button', { name: '고대비' }));
    expect(screen.getByTestId('active-theme')).toHaveTextContent('high-contrast');
    expect(screen.getByRole('button', { name: '고대비' })).toHaveAttribute('aria-pressed', 'true');

    expect(localStorageSet).not.toHaveBeenCalled();
    expect(localStorageGet).not.toHaveBeenCalled();
  });
});
