import { expandRoles, type Role } from '../navigation';

interface UserLike {
  access_token?: string;
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
  const accessTokenPayload = decodeJwtPayload(user?.access_token);
  const realmRoles = readStringArray((profile.realm_access as { roles?: unknown } | undefined)?.roles);
  const accessTokenRealmRoles = readStringArray((accessTokenPayload.realm_access as { roles?: unknown } | undefined)?.roles);
  const clientRoles = readClientResourceRoles(profile.resource_access);
  const accessTokenClientRoles = readClientResourceRoles(accessTokenPayload.resource_access);
  const directRoles = readStringArray(profile.roles);
  const normalized = [...realmRoles, ...accessTokenRealmRoles, ...clientRoles, ...accessTokenClientRoles, ...directRoles]
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

function readClientResourceRoles(value: unknown): string[] {
  if (!value || typeof value !== 'object') {
    return [];
  }

  return Object.values(value as Record<string, { roles?: unknown }>).flatMap((resource) => readStringArray(resource?.roles));
}

function decodeJwtPayload(token: string | undefined): Record<string, unknown> {
  const payload = token?.split('.')[1];
  if (!payload || typeof globalThis.atob !== 'function') {
    return {};
  }

  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const parsed: unknown = JSON.parse(globalThis.atob(paddedBase64));
    return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}
