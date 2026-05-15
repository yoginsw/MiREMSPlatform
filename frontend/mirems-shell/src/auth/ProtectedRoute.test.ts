import { describe, expect, it } from 'vitest';
import { getProtectedRouteDecision } from './ProtectedRoute';

describe('getProtectedRouteDecision', () => {
  it('allows authenticated users to render the protected content', () => {
    expect(getProtectedRouteDecision({ isAuthenticated: true, isLoading: false, currentPath: '/miremsplatform/audit' })).toEqual({
      type: 'allow',
    });
  });

  it('redirects unauthenticated users to login with the original path as returnUrl', () => {
    expect(getProtectedRouteDecision({ isAuthenticated: false, isLoading: false, currentPath: '/miremsplatform/audit' })).toEqual({
      type: 'redirect',
      loginHref: '/miremsplatform/login?returnUrl=%2Fmiremsplatform%2Faudit',
    });
  });

  it('keeps rendering in loading mode while auth state is being resolved', () => {
    expect(getProtectedRouteDecision({ isAuthenticated: false, isLoading: true, currentPath: '/miremsplatform/audit' })).toEqual({
      type: 'loading',
    });
  });
});
