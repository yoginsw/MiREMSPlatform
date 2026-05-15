import type { UserManagerSettings } from 'oidc-client-ts';
import { WebStorageStateStore } from 'oidc-client-ts';
import { platformHref } from '../navigation';
import { inMemoryUserStorage, transientOidcStateStorage } from './oidc-storage';

export interface OidcEnvironment {
  VITE_OIDC_AUTHORITY?: string;
  VITE_OIDC_CLIENT_ID?: string;
  VITE_OIDC_SCOPE?: string;
}

export function createOidcSettings(
  env: OidcEnvironment = import.meta.env,
  origin: string = globalThis.location?.origin ?? 'http://localhost:5173',
): UserManagerSettings {
  return {
    authority: env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8081/realms/mirems',
    client_id: env.VITE_OIDC_CLIENT_ID ?? 'mirems-shell',
    redirect_uri: `${origin}${platformHref('/auth/callback')}`,
    post_logout_redirect_uri: `${origin}${platformHref('/login')}`,
    silent_redirect_uri: `${origin}${platformHref('/auth/silent-renew')}`,
    response_type: 'code',
    scope: env.VITE_OIDC_SCOPE ?? 'openid profile email roles',
    automaticSilentRenew: false,
    loadUserInfo: true,
    stateStore: new WebStorageStateStore({ prefix: 'mirems:oidc:', store: transientOidcStateStorage() }),
    userStore: new WebStorageStateStore({ prefix: 'mirems:user:', store: inMemoryUserStorage }),
  };
}
