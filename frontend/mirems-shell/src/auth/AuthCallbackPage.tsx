import { useEffect, useState } from 'react';
import { completeSigninRedirect } from './auth-callback';
import { completeCallbackNavigation } from './callback-navigation';

export function AuthCallbackPage() {
  const [message, setMessage] = useState('로그인 응답을 확인하고 있습니다…');

  useEffect(() => {
    completeSigninRedirect()
      .then((returnPath) => {
        completeCallbackNavigation(returnPath);
      })
      .catch(() => {
        setMessage('로그인 처리에 실패했습니다. 다시 로그인해 주세요.');
      });
  }, []);

  return (
    <main className="login-page" aria-labelledby="auth-callback-title">
      <section className="login-card">
        <p className="eyebrow">OIDC CALLBACK</p>
        <h1 id="auth-callback-title">로그인 처리</h1>
        <p>{message}</p>
      </section>
    </main>
  );
}
