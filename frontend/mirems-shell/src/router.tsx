import { ThemeProvider } from '@mirems/ui-core';
import { createRouter, RouterProvider } from '@tanstack/react-router';
import { AuthProvider } from './auth/AuthProvider';
import { PLATFORM_BASE_PATH } from './navigation';
import { routeTree } from './routeTree.gen';

export interface MiremsRouteDefinition {
  path: string;
  file: string;
  protected: boolean;
}

export const routerBasepath = PLATFORM_BASE_PATH;

export const routeDefinitions: MiremsRouteDefinition[] = [
  { path: '/', file: 'routes/index.tsx', protected: false },
  { path: '/login', file: 'routes/login.tsx', protected: false },
  { path: '/auth/callback', file: 'routes/auth/callback.tsx', protected: false },
  { path: '/elections', file: 'routes/_protected/elections/index.tsx', protected: true },
  { path: '/elections/$id', file: 'routes/_protected/elections/$id.tsx', protected: true },
  { path: '/elections/$id/contests', file: 'routes/_protected/elections/$id/contests.tsx', protected: true },
  { path: '/elections/$id/ballots', file: 'routes/_protected/elections/$id/ballots.tsx', protected: true },
  { path: '/elections/$id/results', file: 'routes/_protected/elections/$id/results.tsx', protected: true },
  { path: '/voters/register', file: 'routes/voters/register.tsx', protected: false },
  { path: '/voters', file: 'routes/_protected/voters/index.tsx', protected: true },
  { path: '/voters/eligibility', file: 'routes/_protected/voters/eligibility.tsx', protected: true },
  { path: '/vote/session', file: 'routes/_protected/vote/session.tsx', protected: true },
  { path: '/admin', file: 'routes/_protected/admin.tsx', protected: true },
  { path: '/audit', file: 'routes/_protected/audit.tsx', protected: true },
];

export const publicRouteDefinitions = routeDefinitions.filter((route) => !route.protected);
export const protectedRouteDefinitions = routeDefinitions.filter((route) => route.protected);
export const routerDevtoolsEnabled = import.meta.env.DEV;

export const router = createRouter({
  basepath: routerBasepath,
  routeTree,
});

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

export function AppRouter() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ThemeProvider>
  );
}
