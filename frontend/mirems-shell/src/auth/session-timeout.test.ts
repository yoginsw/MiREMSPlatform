import { describe, expect, it, vi } from 'vitest';
import type { User } from 'oidc-client-ts';
import {
  SESSION_REFRESH_LEAD_MS,
  SESSION_WARNING_LEAD_MS,
  getRefreshDelayMs,
  getSessionTimeoutState,
  scheduleSessionLifecycle,
} from './session-timeout';

function userExpiringAt(expiresAtSeconds: number): User {
  return { expires_at: expiresAtSeconds, expired: false } as User;
}

describe('session timeout and silent refresh scheduling', () => {
  it('shows the timeout warning during the final five minutes before token expiry', () => {
    const now = 1_000_000;
    const user = userExpiringAt(Math.floor((now + SESSION_WARNING_LEAD_MS - 1_000) / 1_000));

    expect(getSessionTimeoutState(user, now)).toMatchObject({
      showWarning: true,
      millisUntilExpiry: SESSION_WARNING_LEAD_MS - 1_000,
    });
  });

  it('schedules silent refresh one minute before expiry', () => {
    const now = 1_000_000;
    const user = userExpiringAt(Math.floor((now + 10 * 60_000) / 1_000));

    expect(getRefreshDelayMs(user, now)).toBe(10 * 60_000 - SESSION_REFRESH_LEAD_MS);
  });

  it('refreshes the user silently when the refresh timer fires', async () => {
    const now = 1_000_000;
    const refreshedUser = userExpiringAt(Math.floor((now + 30 * 60_000) / 1_000));
    const manager = {
      signinSilent: vi.fn().mockResolvedValue(refreshedUser),
      removeUser: vi.fn(),
    };
    const onUser = vi.fn();
    const timers: Array<() => void> = [];

    scheduleSessionLifecycle({
      user: userExpiringAt(Math.floor((now + SESSION_REFRESH_LEAD_MS) / 1_000)),
      manager,
      now: () => now,
      setTimer: (callback) => {
        timers.push(callback);
        return timers.length;
      },
      clearTimer: vi.fn(),
      onUser,
      onWarning: vi.fn(),
      onLogout: vi.fn(),
    });

    timers[0]?.();
    await Promise.resolve();

    expect(manager.signinSilent).toHaveBeenCalledOnce();
    expect(onUser).toHaveBeenCalledWith(refreshedUser);
    expect(manager.removeUser).not.toHaveBeenCalled();
  });

  it('logs out when silent refresh fails', async () => {
    const now = 1_000_000;
    const manager = {
      signinSilent: vi.fn().mockRejectedValue(new Error('network down')),
      removeUser: vi.fn().mockResolvedValue(undefined),
    };
    const onLogout = vi.fn();
    const timers: Array<() => void> = [];

    scheduleSessionLifecycle({
      user: userExpiringAt(Math.floor((now + SESSION_REFRESH_LEAD_MS) / 1_000)),
      manager,
      now: () => now,
      setTimer: (callback) => {
        timers.push(callback);
        return timers.length;
      },
      clearTimer: vi.fn(),
      onUser: vi.fn(),
      onWarning: vi.fn(),
      onLogout,
    });

    timers[0]?.();
    await Promise.resolve();
    await Promise.resolve();

    expect(manager.signinSilent).toHaveBeenCalledOnce();
    expect(manager.removeUser).toHaveBeenCalledOnce();
    expect(onLogout).toHaveBeenCalledOnce();
  });

  it('logs out when silent refresh returns no usable user', async () => {
    const now = 1_000_000;
    const manager = {
      signinSilent: vi.fn().mockResolvedValue(null),
      removeUser: vi.fn().mockResolvedValue(undefined),
    };
    const onLogout = vi.fn();
    const timers: Array<() => void> = [];

    scheduleSessionLifecycle({
      user: userExpiringAt(Math.floor((now + SESSION_REFRESH_LEAD_MS) / 1_000)),
      manager,
      now: () => now,
      setTimer: (callback) => {
        timers.push(callback);
        return timers.length;
      },
      clearTimer: vi.fn(),
      onUser: vi.fn(),
      onWarning: vi.fn(),
      onLogout,
    });

    timers[0]?.();
    await Promise.resolve();
    await Promise.resolve();

    expect(manager.removeUser).toHaveBeenCalledOnce();
    expect(onLogout).toHaveBeenCalledOnce();
  });

  it('logs out when silent refresh returns an expired user', async () => {
    const now = 1_000_000;
    const manager = {
      signinSilent: vi.fn().mockResolvedValue({ ...userExpiringAt(Math.floor((now - 1_000) / 1_000)), expired: true }),
      removeUser: vi.fn().mockResolvedValue(undefined),
    };
    const onLogout = vi.fn();
    const timers: Array<() => void> = [];

    scheduleSessionLifecycle({
      user: userExpiringAt(Math.floor((now + SESSION_REFRESH_LEAD_MS) / 1_000)),
      manager,
      now: () => now,
      setTimer: (callback) => {
        timers.push(callback);
        return timers.length;
      },
      clearTimer: vi.fn(),
      onUser: vi.fn(),
      onWarning: vi.fn(),
      onLogout,
    });

    timers[0]?.();
    await Promise.resolve();
    await Promise.resolve();

    expect(manager.removeUser).toHaveBeenCalledOnce();
    expect(onLogout).toHaveBeenCalledOnce();
  });

  it('logs out immediately when the token is already expired', () => {
    const now = 1_000_000;
    const manager = { signinSilent: vi.fn(), removeUser: vi.fn() };
    const onLogout = vi.fn();

    scheduleSessionLifecycle({
      user: userExpiringAt(Math.floor((now - 1_000) / 1_000)),
      manager,
      now: () => now,
      setTimer: vi.fn(),
      clearTimer: vi.fn(),
      onUser: vi.fn(),
      onWarning: vi.fn(),
      onLogout,
    });

    expect(onLogout).toHaveBeenCalledOnce();
    expect(manager.removeUser).toHaveBeenCalledOnce();
  });
});
