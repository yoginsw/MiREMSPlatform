import { sanitizeReturnUrl } from './redirect';
import { useAuth } from './useAuth';

export function LoginPage() {
  const { isAuthenticated, isLoading, login, logout, user } = useAuth();
  const returnUrl = sanitizeReturnUrl(new URLSearchParams(window.location.search).get('returnUrl'));

  if (isLoading) {
    return <p className="auth-state-message">인증 상태를 확인하고 있습니다…</p>;
  }

  if (isAuthenticated) {
    return (
      <main className="login-page" aria-labelledby="login-title">
        <section className="login-card">
          <p className="eyebrow">OIDC AUTHENTICATED</p>
          <h1 id="login-title">이미 로그인되어 있습니다</h1>
          <p>{user?.profile.preferred_username ?? user?.profile.sub ?? '인증된 사용자'} 계정으로 MiREMS에 접속 중입니다.</p>
          <div className="login-actions">
            <a className="button button--primary" href={returnUrl}>운영 화면으로 이동</a>
            <button className="button button--secondary" type="button" onClick={() => void logout()}>로그아웃</button>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="login-page" aria-labelledby="login-title">
      <section className="login-card">
        <p className="eyebrow">OIDC PKCE LOGIN</p>
        <h1 id="login-title">MiREMS 운영자 로그인</h1>
        <p>Keycloak 기반 OIDC Authorization Code + PKCE 흐름으로 안전하게 로그인합니다.</p>
        <button className="button button--primary" type="button" onClick={() => void login(returnUrl)}>
          Keycloak으로 로그인
        </button>
      </section>
    </main>
  );
}
