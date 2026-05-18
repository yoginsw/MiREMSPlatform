import type { User } from 'oidc-client-ts';

export const SESSION_WARNING_LEAD_MS = 60 * 1_000;
export const SESSION_REFRESH_LEAD_MS = 30 * 1_000;

export interface SessionTimeoutState {
  expiresAtMs: number | null;
  millisUntilExpiry: number | null;
  showWarning: boolean;
  isExpired: boolean;
}

export interface SilentRefreshManager {
  signinSilent: () => Promise<User | null>;
  removeUser?: () => Promise<void> | void;
}

export interface SessionLifecycleOptions {
  user: User | null;
  manager: SilentRefreshManager;
  now?: () => number;
  setTimer?: (callback: () => void, delayMs: number) => unknown;
  clearTimer?: (timer: unknown) => void;
  onUser: (user: User | null) => void;
  onWarning: () => void;
  onLogout: () => void;
}

export function isUsableOidcUser(user: User | null): user is User {
  return Boolean(user && !user.expired);
}

export function getAccessTokenExpiryMs(user: User | null): number | null {
  if (!user?.expires_at) {
    return null;
  }

  return user.expires_at * 1_000;
}

export function getSessionTimeoutState(user: User | null, nowMs = Date.now()): SessionTimeoutState {
  const expiresAtMs = getAccessTokenExpiryMs(user);

  if (!expiresAtMs) {
    return {
      expiresAtMs: null,
      millisUntilExpiry: null,
      showWarning: false,
      isExpired: false,
    };
  }

  const millisUntilExpiry = Math.max(0, expiresAtMs - nowMs);
  const isExpired = user?.expired === true || expiresAtMs <= nowMs;

  return {
    expiresAtMs,
    millisUntilExpiry,
    showWarning: !isExpired && millisUntilExpiry <= SESSION_WARNING_LEAD_MS,
    isExpired,
  };
}

export function getRefreshDelayMs(user: User | null, nowMs = Date.now(), refreshLeadMs = SESSION_REFRESH_LEAD_MS) {
  const expiresAtMs = getAccessTokenExpiryMs(user);
  if (!expiresAtMs) {
    return null;
  }

  return Math.max(0, expiresAtMs - nowMs - refreshLeadMs);
}

export function scheduleSessionLifecycle({
  user,
  manager,
  now = Date.now,
  setTimer = (callback, delayMs) => window.setTimeout(callback, delayMs),
  clearTimer = (timer) => window.clearTimeout(timer as number),
  onUser,
  onWarning,
  onLogout,
}: SessionLifecycleOptions): () => void {
  if (!user) {
    return () => undefined;
  }

  const timers: unknown[] = [];
  const currentTimeMs = now();
  const timeoutState = getSessionTimeoutState(user, currentTimeMs);

  const logout = () => {
    onLogout();
    void manager.removeUser?.();
  };

  if (timeoutState.isExpired) {
    logout();
    return () => undefined;
  }

  const refreshDelayMs = getRefreshDelayMs(user, currentTimeMs);
  if (refreshDelayMs !== null) {
    timers.push(
      setTimer(() => {
        manager
          .signinSilent()
          .then((refreshedUser) => {
            if (isUsableOidcUser(refreshedUser)) {
              onUser(refreshedUser);
              return;
            }

            logout();
          })
          .catch(() => {
            logout();
          });
      }, refreshDelayMs),
    );
  }

  if (timeoutState.expiresAtMs) {
    const warningDelayMs = Math.max(0, timeoutState.expiresAtMs - currentTimeMs - SESSION_WARNING_LEAD_MS);
    timers.push(setTimer(onWarning, warningDelayMs));
    timers.push(setTimer(logout, Math.max(0, timeoutState.expiresAtMs - currentTimeMs)));
  }

  return () => timers.forEach(clearTimer);
}
