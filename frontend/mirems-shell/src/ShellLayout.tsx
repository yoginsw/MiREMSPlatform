import React from 'react';
import { Outlet, useRouterState } from '@tanstack/react-router';
import { designSystemName } from '@mirems/ui-core';
import { useTranslation } from 'react-i18next';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { useAuth } from './auth/useAuth';
import type { AuthContextValue } from './auth/AuthProvider';
import { LanguageSwitcher } from './i18n/LanguageSwitcher';
import { platformHref, type NavigationItem, visibleNavigationItems } from './navigation';
import { visibleTaskNotifications } from './role-ui';
import { ThemeSwitcher, useResponsiveLayoutMode, useShellTheme } from './theme/shell-theme';

export function ProtectedShellLayout() {
  const auth = useAuth();
  const currentPath = useRouterState({ select: (state) => state.location.href });

  return (
    <ShellChrome auth={auth} currentPath={currentPath}>
      <ProtectedRoute isAuthenticated={auth.isAuthenticated} isLoading={auth.isLoading} currentPath={currentPath}>
        <Outlet />
      </ProtectedRoute>
    </ShellChrome>
  );
}

export function ShellChrome({
  auth,
  currentPath,
  children,
}: {
  auth: AuthContextValue;
  currentPath: string;
  children: React.ReactNode;
}) {
  const { t } = useTranslation();
  const { theme } = useShellTheme();
  const layoutMode = useResponsiveLayoutMode();
  const navItems = visibleNavigationItems(auth.roles);
  const visibleTaskCount = visibleTaskNotifications(auth.roles).length;
  const isCompactLayout = layoutMode !== 'desktop';

  return (
    <div className="app-shell" data-theme={theme} data-layout={layoutMode}>
      <a className="skip-link" href="#main-content">
        {t('shell.skipToContent')}
      </a>
      <header className="topbar" aria-label={t('shell.globalHeaderLabel')}>
        <div className="brand-block" aria-label={t('app.name')}>
          <span className="brand-logo">MiREMS</span>
          <span className="brand-subtitle">{t('shell.brandSubtitle')}</span>
        </div>
        <button className="scope-selector" type="button" aria-label={t('shell.scopeSelectorLabel')}>
          📋 {t('shell.currentElectionScope')} ▾
        </button>
        <div className="topbar-actions">
          <span className="extension-badge">KR</span>
          <span className="extension-badge extension-badge--muted">US</span>
          <ThemeSwitcher />
          <LanguageSwitcher />
          <button className="notification-button" type="button" aria-label={t('shell.incompleteTasks', { count: visibleTaskCount })}>
            🔔<span className="notification-count">{visibleTaskCount}</span>
          </button>
          <button
            className="user-menu"
            type="button"
            aria-label={t('shell.userMenuLabel')}
            onClick={() => void (auth.isAuthenticated ? auth.logout() : auth.login(currentPath))}
          >
            <span className="avatar" aria-hidden="true">{auth.isAuthenticated ? '관' : '?'}</span>
            {auth.isAuthenticated ? auth.user?.profile.preferred_username ?? t('shell.authenticatedUser') : t('shell.login')} ▾
          </button>
        </div>
      </header>

      <div className="workspace">
        {!isCompactLayout ? <ShellNavigation items={navItems} placement="sidebar" /> : null}

        <main id="main-content" className="main-content">
          {children}
        </main>
      </div>

      {isCompactLayout ? <ShellNavigation items={navItems} placement="bottom" /> : null}
    </div>
  );
}

function ShellNavigation({ items, placement }: { items: NavigationItem[]; placement: 'sidebar' | 'bottom' }) {
  const { t } = useTranslation();
  const isBottom = placement === 'bottom';

  return (
    <aside
      className={isBottom ? 'bottom-nav' : 'sidebar'}
      aria-label={isBottom ? t('layout.mobileNavigationLabel') : t('shell.primaryNavigationLabel')}
      data-navigation-placement={placement}
    >
      <nav>
        {items.map((item, index) => (
          <React.Fragment key={item.href}>
            {!isBottom && index > 0 && item.section !== items[index - 1]?.section ? (
              <div className="nav-section-label">{sectionLabel(item.section, t)}</div>
            ) : null}
            <a className={item.href === platformHref('/') ? 'nav-item nav-item--active' : 'nav-item'} href={item.href}>
              <span aria-hidden="true">{item.icon}</span>
              <span>{t(item.labelKey)}</span>
            </a>
          </React.Fragment>
        ))}
      </nav>
      {!isBottom ? (
        <div className="sidebar-footer">
          <span>{designSystemName}</span>
          <strong>{t('shell.footerCompliance')}</strong>
        </div>
      ) : null}
    </aside>
  );
}

function sectionLabel(section: string | undefined, t: (key: string) => string) {
  switch (section) {
    case 'operations':
      return t('navigation.sections.operations');
    case 'audit':
      return t('navigation.sections.audit');
    case 'system':
      return t('navigation.sections.system');
    default:
      return t('navigation.sections.main');
  }
}
