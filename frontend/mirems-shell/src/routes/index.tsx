import { createFileRoute } from '@tanstack/react-router';
import { platformHref } from '../navigation';

export const Route = createFileRoute('/')({
  component: LandingPage,
});

export function LandingPage() {
  return (
    <main id="main-content" className="main-content main-content--public">
      <section className="hero-panel" aria-labelledby="landing-title">
        <div>
          <p className="eyebrow">NIST VVSG 2.0 ALIGNED PLATFORM</p>
          <h1 id="landing-title">신뢰할 수 있고, 감사 가능한, 확장 가능한 선거 플랫폼</h1>
          <p>
            선거 생성부터 후보자 심사, 투표 세션, 개표, 결과 인증, 감사 로그까지 하나의 제도적 흐름으로
            관리합니다.
          </p>
          <div className="hero-actions">
            <a className="button button--secondary-on-dark" href={platformHref('/login')}>
              시스템 로그인 →
            </a>
            <a className="button button--ghost-on-dark" href={platformHref('/elections/current/results')}>
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
    </main>
  );
}
