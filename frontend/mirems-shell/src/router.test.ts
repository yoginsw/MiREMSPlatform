import { describe, expect, it } from 'vitest';
import { PLATFORM_BASE_PATH } from './navigation';
import { protectedRouteDefinitions, publicRouteDefinitions, routeDefinitions, routerBasepath } from './router';

describe('MiREMS TanStack route definitions', () => {
  it('uses the MiREMS platform base path for the router', () => {
    expect(routerBasepath).toBe(PLATFORM_BASE_PATH);
  });

  it('declares the Phase 5 shell route tree from route files', () => {
    expect(routeDefinitions.map((route) => route.path)).toEqual([
      '/',
      '/login',
      '/auth/callback',
      '/elections',
      '/elections/$id',
      '/elections/$id/contests',
      '/elections/$id/ballots',
      '/elections/$id/results',
      '/admin',
      '/audit',
    ]);

    expect(routeDefinitions.map((route) => route.file)).toEqual([
      'routes/index.tsx',
      'routes/login.tsx',
      'routes/auth/callback.tsx',
      'routes/_protected/elections/index.tsx',
      'routes/_protected/elections/$id.tsx',
      'routes/_protected/elections/$id/contests.tsx',
      'routes/_protected/elections/$id/ballots.tsx',
      'routes/_protected/elections/$id/results.tsx',
      'routes/_protected/admin.tsx',
      'routes/_protected/audit.tsx',
    ]);
  });

  it('keeps only landing, login, and callback public', () => {
    expect(publicRouteDefinitions.map((route) => route.path)).toEqual(['/', '/login', '/auth/callback']);
    expect(protectedRouteDefinitions.map((route) => route.path)).toEqual([
      '/elections',
      '/elections/$id',
      '/elections/$id/contests',
      '/elections/$id/ballots',
      '/elections/$id/results',
      '/admin',
      '/audit',
    ]);
  });
});
