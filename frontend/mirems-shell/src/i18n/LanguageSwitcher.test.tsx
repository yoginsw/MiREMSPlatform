import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { I18nProvider } from './I18nProvider';
import { LanguageSwitcher } from './LanguageSwitcher';
import { resetShellLanguageForTests } from './i18n';

function renderSwitcher() {
  return render(
    <ThemeProvider>
      <I18nProvider>
        <LanguageSwitcher />
      </I18nProvider>
    </ThemeProvider>,
  );
}

afterEach(() => {
  resetShellLanguageForTests();
});

describe('LanguageSwitcher', () => {
  it('renders Korean by default and switches shell text to English in memory', async () => {
    renderSwitcher();

    expect(screen.getByRole('group', { name: '언어 선택' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '한국어' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'false');

    fireEvent.click(screen.getByRole('button', { name: 'English' }));

    expect(await screen.findByRole('group', { name: 'Language selection' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Korean' })).toHaveAttribute('aria-pressed', 'false');
  });
});
