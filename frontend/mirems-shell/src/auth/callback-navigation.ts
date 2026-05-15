import { notifyAuthUpdated } from './auth-events';
import { sanitizeReturnUrl } from './redirect';

export function completeCallbackNavigation(returnPath: string): string {
  const safeReturnPath = sanitizeReturnUrl(returnPath);
  window.history.replaceState(null, '', safeReturnPath);
  notifyAuthUpdated();
  return safeReturnPath;
}
