import { UserManager } from 'oidc-client-ts';
import { platformHref } from '../navigation';
import { createOidcSettings } from './oidc-config';
import { sanitizeReturnUrl } from './redirect';

export async function completeSigninRedirect(userManager = new UserManager(createOidcSettings())): Promise<string> {
  const user = await userManager.signinRedirectCallback();
  const state = user.state as { returnPath?: string } | undefined;
  return sanitizeReturnUrl(state?.returnPath ?? platformHref('/'));
}
