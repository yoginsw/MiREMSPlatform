import { describe, expect, it } from 'vitest';
import { getRoleGuardDecision } from './RoleGuard';

describe('getRoleGuardDecision', () => {
  it('allows a user with a directly required role', () => {
    expect(getRoleGuardDecision({ userRoles: ['AUDITOR'], allowedRoles: ['AUDITOR'] })).toBe('show');
  });

  it('allows a higher role through the MiREMS role hierarchy', () => {
    expect(getRoleGuardDecision({ userRoles: ['SYSTEM_ADMIN'], allowedRoles: ['ELECTION_OFFICER'] })).toBe('show');
  });

  it('hides election-admin actions from observer-only users', () => {
    expect(getRoleGuardDecision({ userRoles: ['OBSERVER'], allowedRoles: ['ELECTION_ADMIN'] })).toBe('hide');
  });

  it('hides guarded UI when the guard is accidentally configured without allowed roles', () => {
    expect(getRoleGuardDecision({ userRoles: ['SYSTEM_ADMIN'], allowedRoles: [] })).toBe('hide');
  });

  it('hides guarded UI when no authenticated role is available', () => {
    expect(getRoleGuardDecision({ userRoles: [], allowedRoles: ['SYSTEM_ADMIN'] })).toBe('hide');
  });
});
