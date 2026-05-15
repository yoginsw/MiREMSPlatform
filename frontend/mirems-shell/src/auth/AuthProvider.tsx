import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { UserManager, type User, type UserManagerSettings } from 'oidc-client-ts';
import { extractElectionScope, extractRoles, hasElectionScope as scopeContains, hasRole as roleContains } from './auth-claims';
import { authUpdatedEventName } from './auth-events';
import { createOidcSettings } from './oidc-config';
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

    const onUserLoaded = (loadedUser: User) => setUser(loadedUser && !loadedUser.expired ? loadedUser : null);
    const onUserUnloaded = () => setUser(null);
    const onAccessTokenExpired = () => setUser(null);

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

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
