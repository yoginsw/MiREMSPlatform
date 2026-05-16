import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { I18nProvider } from '../i18n/I18nProvider';
import { resetShellLanguageForTests } from '../i18n/i18n';
import { ShellThemeProvider } from '../theme/shell-theme';
import { LandingPage } from './index';

function renderLandingPage() {
  return render(
    <ThemeProvider>
      <I18nProvider>
        <ShellThemeProvider>
          <LandingPage />
        </ShellThemeProvider>
      </I18nProvider>
    </ThemeProvider>,
  );
}

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
    expect(screen.getAllByRole('link', { name: '운영자 로그인' }).map((link) => link.getAttribute('href'))).toContain('/miremsplatform/login');
    expect(screen.getByRole('link', { name: '공식 결과 보기' })).toHaveAttribute('href', '/miremsplatform/elections/current/results');

    const features = screen.getByRole('region', { name: '핵심 역량' });
    expect(within(features).getByText('전 과정 선거 워크플로')).toBeInTheDocument();
    expect(within(features).getByText('감사 가능성과 추적성')).toBeInTheDocument();
    expect(within(features).getByText('글로벌 확장 아키텍처')).toBeInTheDocument();

    const trustSection = screen.getByRole('region', { name: '신뢰 기준' });
    expect(within(trustSection).getByText('VVSG 2.0 aligned')).toBeInTheDocument();
    expect(within(trustSection).getByText('WCAG AA oriented')).toBeInTheDocument();
    expect(within(trustSection).getByText('Role-based access')).toBeInTheDocument();
  });

  it('switches landing copy to English and applies theme styling in memory', async () => {
    renderLandingPage();

    fireEvent.click(screen.getByRole('button', { name: 'English' }));

    expect(await screen.findByRole('banner', { name: 'MiREMS Portal header' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'An integrated election management platform for trusted electoral operations' })).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Operator sign in' }).map((link) => link.getAttribute('href'))).toContain('/miremsplatform/login');
    expect(screen.getByText('End-to-end election workflow')).toBeInTheDocument();

    const landingRoot = screen.getByTestId('landing-page');
    expect(landingRoot).toHaveAttribute('data-theme', 'light');

    fireEvent.click(screen.getByRole('button', { name: 'High contrast' }));
    expect(landingRoot).toHaveAttribute('data-theme', 'high-contrast');
  });
});
