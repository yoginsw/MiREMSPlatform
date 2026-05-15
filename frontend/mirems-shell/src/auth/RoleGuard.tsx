import type { ReactNode } from 'react';
import { expandRoles, type Role } from '../navigation';
import { useAuth } from './useAuth';

export type RoleGuardDecision = 'show' | 'hide';

export interface RoleGuardDecisionInput {
  userRoles: Role[];
  allowedRoles: Role[];
}

export function getRoleGuardDecision({ userRoles, allowedRoles }: RoleGuardDecisionInput): RoleGuardDecision {
  if (allowedRoles.length === 0) {
    return 'hide';
  }

  const effectiveRoles = expandRoles(userRoles);
  return allowedRoles.some((role) => effectiveRoles.has(role)) ? 'show' : 'hide';
}

export interface RoleGuardProps {
  roles: Role[];
  children: ReactNode;
  fallback?: ReactNode;
}

export function RoleGuard({ roles, children, fallback = null }: RoleGuardProps) {
  const auth = useAuth();
  return getRoleGuardDecision({ userRoles: auth.roles, allowedRoles: roles }) === 'show' ? <>{children}</> : <>{fallback}</>;
}
