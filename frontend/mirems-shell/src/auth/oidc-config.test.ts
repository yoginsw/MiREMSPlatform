import { describe, expect, it } from 'vitest';
import { createOidcSettings } from './oidc-config';

describe('createOidcSettings', () => {
  it('builds a public OIDC authorization-code PKCE client below the platform base path', () => {
    const settings = createOidcSettings(
      {
        VITE_OIDC_AUTHORITY: 'https://keycloak.example/realms/mirems',
        VITE_OIDC_CLIENT_ID: 'mirems-shell',
        VITE_OIDC_SCOPE: 'openid profile email roles',
      },
      'https://ops.example',
    );

    expect(settings.authority).toBe('https://keycloak.example/realms/mirems');
    expect(settings.client_id).toBe('mirems-shell');
    expect(settings.response_type).toBe('code');
    expect(settings.scope).toBe('openid profile email roles');
    expect(settings.redirect_uri).toBe('https://ops.example/miremsplatform/auth/callback');
    expect(settings.post_logout_redirect_uri).toBe('https://ops.example/miremsplatform/login');
    expect(settings.silent_redirect_uri).toBe('https://ops.example/miremsplatform/auth/silent-renew');
  });

  it('uses safe MiREMS defaults when optional environment values are absent', () => {
    const settings = createOidcSettings({}, 'https://ops.example');

    expect(settings.authority).toBe('http://localhost:8081/realms/mirems');
    expect(settings.client_id).toBe('mirems-shell');
    expect(settings.scope).toBe('openid profile email roles');
  });
});
