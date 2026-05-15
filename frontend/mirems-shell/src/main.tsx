import React from 'react';
import { createRoot } from 'react-dom/client';
import { colors, designSystemName } from '@mirems/ui-core';
import { type Role, visibleNavigationItems } from './navigation';
import './styles.css';

const activeRoles: Role[] = ['SYSTEM_ADMIN'];
const taskNotifications = [
  { id: 'task-1', title: '선거 발행 검토', meta: '관리자 검토 · 3일 2시간 남음' },
  { id: 'task-2', title: '후보자 자격 심사', meta: 'ELECTION_OFFICER 필요 · 18건' },
];

const processSteps = [
  { label: '시작', status: 'completed' },
  { label: '구성 검증', status: 'completed' },
  { label: '관리자 검토', status: 'active' },
  { label: '발행 완료', status: 'upcoming' },
] as const;

const resultRows = [
  { district: '서울 제1선거구', status: 'CERTIFIED', turnout: '71.4%', hash: '8fa3…c91e' },
  { district: '부산 제2선거구', status: 'ACTIVE', turnout: '68.2%', hash: '71cd…aa02' },
  { district: '대전 제3선거구', status: 'PENDING', turnout: '64.8%', hash: 'b00f…19dd' },
];

function App() {
  const navItems = visibleNavigationItems(activeRoles);

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
          <button className="notification-button" type="button" aria-label="미완료 태스크 2건">
            🔔<span className="notification-count">2</span>
          </button>
          <button className="user-menu" type="button" aria-label="사용자 메뉴 열기">
            <span className="avatar" aria-hidden="true">관</span>
            시스템 관리자 ▾
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
                <a className={item.href === '/' ? 'nav-item nav-item--active' : 'nav-item'} href={item.href}>
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
          <section className="hero-panel" aria-labelledby="landing-title">
            <div>
              <p className="eyebrow">NIST VVSG 2.0 ALIGNED PLATFORM</p>
              <h1 id="landing-title">신뢰할 수 있고, 감사 가능한, 확장 가능한 선거 플랫폼</h1>
              <p>
                선거 생성부터 후보자 심사, 투표 세션, 개표, 결과 인증, 감사 로그까지 하나의 제도적 흐름으로
                관리합니다.
              </p>
              <div className="hero-actions">
                <a className="button button--secondary-on-dark" href="/login">
                  시스템 로그인 →
                </a>
                <a className="button button--ghost-on-dark" href="/elections/current/results">
                  공식 결과 보기
                </a>
              </div>
            </div>
            <div className="trust-card" aria-label="플랫폼 신뢰 기준">
              <span>NIST VVSG 2.0</span>
              <span>보안 감사 추적</span>
              <span>WCAG 2.1 AA</span>
            </div>
          </section>

          <section className="page-header" aria-labelledby="dashboard-title">
            <div>
              <p className="breadcrumb">대시보드 / 제22대 국회의원선거</p>
              <div className="title-row">
                <h2 id="dashboard-title">선거 운영 현황</h2>
                <span className="badge badge--active">ACTIVE ●</span>
              </div>
              <p>현재 프로세스 단계와 감사 가능한 다음 조치를 명확하게 표시합니다.</p>
            </div>
            <div className="page-actions">
              <button className="button button--secondary" type="button">내보내기</button>
              <button className="button button--primary" type="button">발행 검토</button>
            </div>
          </section>

          <section className="dashboard-grid" aria-label="선거 운영 핵심 지표">
            <MetricCard label="등록 선거인" value="1,284,399" detail="전일 대비 +2,418" variant="navy" />
            <MetricCard label="대기 중 심사" value="18" detail="후보자 등록 프로세스" variant="amber" />
            <MetricCard label="감사 이벤트" value="42,901" detail="최근 24시간 append-only" variant="sage" />
          </section>

          <section className="process-panel" aria-labelledby="process-title">
            <div className="process-panel__header">
              <div>
                <p className="eyebrow">BPMN PROCESS</p>
                <h3 id="process-title">선거 발행 프로세스 · 실행 중</h3>
              </div>
              <span className="badge badge--pending">관리자 검토 필요</span>
            </div>
            <ol className="progress-steps" aria-label="선거 발행 프로세스 단계">
              {processSteps.map((step) => (
                <li className={`progress-step progress-step--${step.status}`} key={step.label}>
                  <span className="progress-step__dot" aria-hidden="true" />
                  <span>{step.label}</span>
                </li>
              ))}
            </ol>
            <div className="process-callout">
              <strong>현재 단계: 관리자 검토</strong>
              <span>담당자: admin@election.go.kr · 기한: 2026-05-20 18:00</span>
              <div className="process-actions">
                <button className="button button--primary" type="button">승인 ✓</button>
                <button className="button button--danger-ghost" type="button">반려 ✕</button>
              </div>
            </div>
          </section>

          <section className="content-grid">
            <article className="card" aria-labelledby="notifications-title">
              <h3 id="notifications-title">태스크 알림</h3>
              <div className="task-list">
                {taskNotifications.map((task) => (
                  <a className="task-item" href="/admin/processes" key={task.id}>
                    <strong>{task.title}</strong>
                    <span>{task.meta}</span>
                  </a>
                ))}
              </div>
            </article>

            <article className="card card--wide" aria-labelledby="results-title">
              <div className="card-header">
                <h3 id="results-title">개표 결과 무결성 현황</h3>
                <span className="badge badge--certified">CERTIFIED</span>
              </div>
              <div className="table-wrap">
                <table>
                  <caption>선거구별 개표 결과 인증 및 해시 상태</caption>
                  <thead>
                    <tr>
                      <th scope="col">선거구</th>
                      <th scope="col">상태</th>
                      <th scope="col">투표율</th>
                      <th scope="col">보고서 해시</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resultRows.map((row) => (
                      <tr key={row.district}>
                        <td>{row.district}</td>
                        <td><StatusBadge status={row.status} /></td>
                        <td>{row.turnout}</td>
                        <td><code>{row.hash}</code></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </article>
          </section>
        </main>
      </div>
    </div>
  );
}

function MetricCard({ label, value, detail, variant }: { label: string; value: string; detail: string; variant: string }) {
  return (
    <article className={`metric-card metric-card--${variant}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}

function StatusBadge({ status }: { status: string }) {
  const className =
    status === 'CERTIFIED' ? 'badge badge--certified' : status === 'ACTIVE' ? 'badge badge--active' : 'badge badge--pending';
  return <span className={className}>{status}</span>;
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

const root = document.getElementById('root');

if (!root) {
  throw new Error('Root element #root was not found.');
}

createRoot(root).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);

void colors;
