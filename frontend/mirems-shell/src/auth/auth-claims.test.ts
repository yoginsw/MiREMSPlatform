import { describe, expect, it } from 'vitest';
import { extractElectionScope, extractRoles, hasRole } from './auth-claims';

describe('auth claim helpers', () => {
  it('extracts Keycloak realm roles and normalizes ROLE_ prefixes', () => {
    const roles = extractRoles({
      profile: {
        realm_access: { roles: ['ROLE_SYSTEM_ADMIN', 'offline_access', 'AUDITOR'] },
      },
    });

    expect(roles).toEqual(['SYSTEM_ADMIN', 'AUDITOR']);
    expect(hasRole(roles, 'AUDITOR')).toBe(true);
    expect(hasRole(roles, 'ELECTION_OFFICER')).toBe(true);
    expect(hasRole(roles, 'VOTER')).toBe(false);
  });

  it('extracts MiREMS election scope claim values', () => {
    expect(
      extractElectionScope({
        profile: {
          mirems_election_scope: ['11111111-1111-4111-8111-111111111111', '*'],
        },
      }),
    ).toEqual(['11111111-1111-4111-8111-111111111111', '*']);
  });
});
