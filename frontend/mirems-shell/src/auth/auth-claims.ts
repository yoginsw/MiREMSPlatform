import { expandRoles, type Role } from '../navigation';

interface UserLike {
  profile?: Record<string, unknown>;
}

const knownRoles: ReadonlySet<string> = new Set<Role>([
  'SYSTEM_ADMIN',
  'ELECTION_ADMIN',
  'ELECTION_OFFICER',
  'TABULATION_OFFICER',
  'AUDITOR',
  'OBSERVER',
  'VOTER',
]);

export function extractRoles(user: UserLike | null | undefined): Role[] {
  const profile = user?.profile ?? {};
  const realmRoles = readStringArray((profile.realm_access as { roles?: unknown } | undefined)?.roles);
  const directRoles = readStringArray(profile.roles);
  const normalized = [...realmRoles, ...directRoles]
    .map((role) => role.replace(/^ROLE_/, ''))
    .filter((role): role is Role => knownRoles.has(role));

  return Array.from(new Set(normalized));
}

export function extractElectionScope(user: UserLike | null | undefined): string[] {
  return readStringArray(user?.profile?.mirems_election_scope);
}

export function hasRole(userRoles: Role[], requiredRole: Role): boolean {
  return expandRoles(userRoles).has(requiredRole);
}

export function hasElectionScope(electionScope: string[], electionId: string): boolean {
  return electionScope.includes('*') || electionScope.includes(electionId);
}

function readStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((item): item is string => typeof item === 'string' && item.length > 0);
}
