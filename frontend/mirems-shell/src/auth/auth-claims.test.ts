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

  it('extracts Keycloak client resource roles when realm roles are not exposed in the profile', () => {
    const roles = extractRoles({
      profile: {
        resource_access: {
          'mirems-frontend': { roles: ['ROLE_ELECTION_ADMIN', 'offline_access'] },
        },
      },
    });

    expect(roles).toEqual(['ELECTION_ADMIN']);
    expect(hasRole(roles, 'OBSERVER')).toBe(true);
  });

  it('extracts roles from the Keycloak access token when userinfo omits role claims', () => {
    const roles = extractRoles({
      access_token: jwtWithPayload({
        resource_access: {
          'mirems-frontend': { roles: ['TABULATION_OFFICER'] },
        },
      }),
      profile: {},
    });

    expect(roles).toEqual(['TABULATION_OFFICER']);
    expect(hasRole(roles, 'OBSERVER')).toBe(true);
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

function jwtWithPayload(payload: Record<string, unknown>): string {
  return [base64Url({ alg: 'none' }), base64Url(payload), ''].join('.');
}

function base64Url(value: Record<string, unknown>): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}
