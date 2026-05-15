import React from 'react';
import { Outlet, useRouterState } from '@tanstack/react-router';
import { designSystemName } from '@mirems/ui-core';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { useAuth } from './auth/useAuth';
import { platformHref, visibleNavigationItems } from './navigation';
import { visibleTaskNotifications } from './role-ui';

export function ProtectedShellLayout() {
  const auth = useAuth();
  const currentPath = useRouterState({ select: (state) => state.location.href });
  const navItems = visibleNavigationItems(auth.roles);
  const visibleTaskCount = visibleTaskNotifications(auth.roles).length;

  return (
    <div className="app-shell" data-theme="light">
      <a className="skip-link" href="#main-content">
        본문으로 바로가기
      </a>
      <header className="topbar" aria-label="MiREMS 전역 헤더">
        <div className="brand-block" aria-label="MiREMS Platform">
          <span className="brand-logo">MiREMS</span>
          <span className="brand-subtitle">Miru Election Management Solution</span>
        </div>
        <button className="scope-selector" type="button" aria-label="현재 선거 범위 선택">
          📋 제22대 국회의원 선거 ▾
        </button>
        <div className="topbar-actions">
          <span className="extension-badge">KR</span>
          <span className="extension-badge extension-badge--muted">US</span>
          <button className="notification-button" type="button" aria-label={`미완료 태스크 ${visibleTaskCount}건`}>
            🔔<span className="notification-count">{visibleTaskCount}</span>
          </button>
          <button
            className="user-menu"
            type="button"
            aria-label="사용자 메뉴 열기"
            onClick={() => void (auth.isAuthenticated ? auth.logout() : auth.login(currentPath))}
          >
            <span className="avatar" aria-hidden="true">{auth.isAuthenticated ? '관' : '?'}</span>
            {auth.isAuthenticated ? auth.user?.profile.preferred_username ?? '인증 사용자' : '로그인'} ▾
          </button>
        </div>
      </header>

      <div className="workspace">
        <aside className="sidebar" aria-label="주요 내비게이션">
          <nav>
            {navItems.map((item, index) => (
              <React.Fragment key={item.href}>
                {index > 0 && item.section !== navItems[index - 1]?.section ? (
                  <div className="nav-section-label">{sectionLabel(item.section)}</div>
                ) : null}
                <a className={item.href === platformHref('/') ? 'nav-item nav-item--active' : 'nav-item'} href={item.href}>
                  <span aria-hidden="true">{item.icon}</span>
                  <span>{item.label}</span>
                </a>
              </React.Fragment>
            ))}
          </nav>
          <div className="sidebar-footer">
            <span>{designSystemName}</span>
            <strong>VVSG 2.0 · WCAG AA</strong>
          </div>
        </aside>

        <main id="main-content" className="main-content">
          <ProtectedRoute isAuthenticated={auth.isAuthenticated} isLoading={auth.isLoading} currentPath={currentPath}>
            <Outlet />
          </ProtectedRoute>
        </main>
      </div>
    </div>
  );
}


function sectionLabel(section?: string) {
  switch (section) {
    case 'operations':
      return 'OPERATIONS';
    case 'audit':
      return 'AUDIT';
    case 'system':
      return 'SYSTEM';
    default:
      return 'MAIN';
  }
}
