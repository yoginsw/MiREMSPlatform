import { UserManager } from 'oidc-client-ts';
import { platformHref } from '../navigation';
import { createOidcSettings } from './oidc-config';
import { sanitizeReturnUrl } from './redirect';

export interface SilentRenewCallbackManager {
  signinSilentCallback: () => Promise<void>;
}

export async function completeSigninRedirect(userManager = new UserManager(createOidcSettings())): Promise<string> {
  const user = await userManager.signinRedirectCallback();
  const state = user.state as { returnPath?: string } | undefined;
  return sanitizeReturnUrl(state?.returnPath ?? platformHref('/'));
}

export function completeSilentRenew(userManager: SilentRenewCallbackManager = new UserManager(createOidcSettings())): Promise<void> {
  return userManager.signinSilentCallback();
}
