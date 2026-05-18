import { useEffect, useRef, useState } from 'react';
import { completeSilentRenew } from './auth-callback';

export function SilentRenewPage() {
  const [message, setMessage] = useState('세션 연장 응답을 확인하고 있습니다…');
  const callbackHandledRef = useRef(false);

  useEffect(() => {
    if (callbackHandledRef.current) {
      return;
    }

    callbackHandledRef.current = true;
    completeSilentRenew().catch(() => {
      setMessage('세션 연장 처리에 실패했습니다. 다시 로그인해 주세요.');
    });
  }, []);

  return (
    <main className="login-page" aria-labelledby="silent-renew-title">
      <section className="login-card">
        <p className="eyebrow">OIDC SILENT RENEW</p>
        <h1 id="silent-renew-title">세션 연장 처리</h1>
        <p>{message}</p>
      </section>
    </main>
  );
}
