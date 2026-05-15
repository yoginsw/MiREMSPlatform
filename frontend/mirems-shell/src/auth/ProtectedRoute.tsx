import type { ReactNode } from 'react';
import { platformHref } from '../navigation';

export type ProtectedRouteDecision =
  | { type: 'allow' }
  | { type: 'loading' }
  | { type: 'redirect'; loginHref: string };

export interface ProtectedRouteDecisionInput {
  isAuthenticated: boolean;
  isLoading: boolean;
  currentPath: string;
}

export function getProtectedRouteDecision({
  isAuthenticated,
  isLoading,
  currentPath,
}: ProtectedRouteDecisionInput): ProtectedRouteDecision {
  if (isLoading) {
    return { type: 'loading' };
  }

  if (isAuthenticated) {
    return { type: 'allow' };
  }

  return {
    type: 'redirect',
    loginHref: `${platformHref('/login')}?returnUrl=${encodeURIComponent(currentPath)}`,
  };
}

export interface ProtectedRouteProps extends ProtectedRouteDecisionInput {
  children: ReactNode;
}

export function ProtectedRoute({ children, ...input }: ProtectedRouteProps) {
  const decision = getProtectedRouteDecision(input);

  if (decision.type === 'allow') {
    return <>{children}</>;
  }

  if (decision.type === 'loading') {
    return <p className="auth-state-message">인증 상태를 확인하고 있습니다…</p>;
  }

  return (
    <section className="auth-redirect" aria-labelledby="auth-required-title">
      <h2 id="auth-required-title">로그인이 필요합니다</h2>
      <p>보호된 MiREMS 운영 화면에 접근하려면 먼저 로그인해 주세요.</p>
      <a className="button button--primary" href={decision.loginHref}>
        로그인으로 이동
      </a>
    </section>
  );
}
