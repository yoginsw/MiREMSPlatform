import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { UserManager, type User, type UserManagerSettings } from 'oidc-client-ts';
import { extractElectionScope, extractRoles, hasElectionScope as scopeContains, hasRole as roleContains } from './auth-claims';
import { authUpdatedEventName } from './auth-events';
import { createOidcSettings } from './oidc-config';
import { getSessionTimeoutState, isUsableOidcUser, scheduleSessionLifecycle } from './session-timeout';
import type { Role } from '../navigation';

export interface AuthContextValue {
  user: User | null;
  roles: Role[];
  electionScope: string[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (returnPath?: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (role: Role) => boolean;
  hasElectionScope: (electionId: string) => boolean;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export interface AuthProviderProps {
  children: ReactNode;
  settings?: UserManagerSettings;
  userManager?: UserManager;
}

export function AuthProvider({ children, settings = createOidcSettings(), userManager }: AuthProviderProps) {
  const [manager] = useState(() => userManager ?? new UserManager(settings));
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setLoading] = useState(true);
  const [showTimeoutWarning, setShowTimeoutWarning] = useState(false);

  useEffect(() => {
    let mounted = true;

    const syncCurrentUser = () => {
      void manager.getUser().then((loadedUser) => {
        if (mounted) {
          setUser(loadedUser && !loadedUser.expired ? loadedUser : null);
        }
      });
    };

    syncCurrentUser();
    manager.getUser().finally(() => {
      if (mounted) {
        setLoading(false);
      }
    });

    const onUserLoaded = (loadedUser: User) => {
      setUser(loadedUser && !loadedUser.expired ? loadedUser : null);
      setShowTimeoutWarning(false);
    };
    const onUserUnloaded = () => {
      setUser(null);
      setShowTimeoutWarning(false);
    };
    const onAccessTokenExpired = () => {
      setUser(null);
      setShowTimeoutWarning(false);
    };

    manager.events.addUserLoaded(onUserLoaded);
    manager.events.addUserUnloaded(onUserUnloaded);
    manager.events.addAccessTokenExpired(onAccessTokenExpired);
    window.addEventListener(authUpdatedEventName, syncCurrentUser);

    return () => {
      mounted = false;
      manager.events.removeUserLoaded(onUserLoaded);
      manager.events.removeUserUnloaded(onUserUnloaded);
      manager.events.removeAccessTokenExpired(onAccessTokenExpired);
      window.removeEventListener(authUpdatedEventName, syncCurrentUser);
    };
  }, [manager]);

  const login = useCallback(
    async (returnPath = `${window.location.pathname}${window.location.search}`) => {
      await manager.signinRedirect({ state: { returnPath } });
    },
    [manager],
  );

  const logout = useCallback(async () => {
    await manager.signoutRedirect();
  }, [manager]);

  const clearSessionState = useCallback(() => {
    setUser(null);
    setShowTimeoutWarning(false);
  }, []);

  const handleSessionLogout = useCallback(() => {
    clearSessionState();
    void manager.removeUser();
  }, [clearSessionState, manager]);

  useEffect(() => {
    return scheduleSessionLifecycle({
      user,
      manager,
      onUser: (refreshedUser) => {
        setUser(isUsableOidcUser(refreshedUser) ? refreshedUser : null);
        setShowTimeoutWarning(false);
      },
      onWarning: () => setShowTimeoutWarning(true),
      onLogout: clearSessionState,
    });
  }, [clearSessionState, manager, user]);

  const extendSession = useCallback(async () => {
    try {
      const refreshedUser = await manager.signinSilent();
      if (isUsableOidcUser(refreshedUser)) {
        setUser(refreshedUser);
        setShowTimeoutWarning(false);
        return;
      }
    } catch {
      // fall through to fail-closed logout below
    }

    handleSessionLogout();
  }, [handleSessionLogout, manager]);

  const sessionTimeoutState = useMemo(() => getSessionTimeoutState(user), [user, showTimeoutWarning]);

  const roles = useMemo(() => extractRoles(user), [user]);
  const electionScope = useMemo(() => extractElectionScope(user), [user]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      roles,
      electionScope,
      isAuthenticated: Boolean(user && !user.expired),
      isLoading,
      login,
      logout,
      hasRole: (role) => roleContains(roles, role),
      hasElectionScope: (electionId) => scopeContains(electionScope, electionId),
    }),
    [electionScope, isLoading, login, logout, roles, user],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      {showTimeoutWarning && sessionTimeoutState.millisUntilExpiry !== null ? (
        <SessionTimeoutWarningDialog
          millisUntilExpiry={sessionTimeoutState.millisUntilExpiry}
          onExtend={() => void extendSession()}
          onLogout={() => void logout()}
        />
      ) : null}
    </AuthContext.Provider>
  );
}

function SessionTimeoutWarningDialog({
  millisUntilExpiry,
  onExtend,
  onLogout,
}: {
  millisUntilExpiry: number;
  onExtend: () => void;
  onLogout: () => void;
}) {
  const minutesRemaining = Math.max(1, Math.ceil(millisUntilExpiry / 60_000));

  return (
    <div className="session-timeout-backdrop" role="presentation">
      <section className="session-timeout-dialog" role="alertdialog" aria-labelledby="session-timeout-title" aria-describedby="session-timeout-description">
        <p className="eyebrow">SESSION TIMEOUT</p>
        <h2 id="session-timeout-title">세션 만료 예정</h2>
        <p id="session-timeout-description">
          보안을 위해 약 {minutesRemaining}분 후 자동 로그아웃됩니다. 계속 작업하려면 세션을 연장해 주세요.
        </p>
        <div className="login-actions">
          <button className="button button--primary" type="button" onClick={onExtend}>
            세션 연장
          </button>
          <button className="button button--secondary" type="button" onClick={onLogout}>
            지금 로그아웃
          </button>
        </div>
      </section>
    </div>
  );
}
