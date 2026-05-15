export type Role =
  | 'SYSTEM_ADMIN'
  | 'ELECTION_ADMIN'
  | 'ELECTION_OFFICER'
  | 'TABULATION_OFFICER'
  | 'AUDITOR'
  | 'OBSERVER'
  | 'VOTER';

export const PLATFORM_BASE_PATH = '/miremsplatform';

export function platformHref(path: string): string {
  if (path === '/') {
    return PLATFORM_BASE_PATH;
  }

  return `${PLATFORM_BASE_PATH}${path}`;
}

export interface NavigationItem {
  icon: string;
  label: string;
  href: string;
  minimumRoles: Role[];
  section?: 'main' | 'operations' | 'audit' | 'system';
}

const roleHierarchy: Record<Role, Role[]> = {
  SYSTEM_ADMIN: ['ELECTION_ADMIN', 'ELECTION_OFFICER', 'TABULATION_OFFICER', 'AUDITOR', 'OBSERVER'],
  ELECTION_ADMIN: ['ELECTION_OFFICER', 'OBSERVER'],
  ELECTION_OFFICER: ['OBSERVER'],
  TABULATION_OFFICER: ['OBSERVER'],
  AUDITOR: ['OBSERVER'],
  OBSERVER: [],
  VOTER: [],
};

export const navigationItems: NavigationItem[] = [
  { icon: '🏠', label: '대시보드', href: platformHref('/'), minimumRoles: ['OBSERVER', 'VOTER'], section: 'main' },
  { icon: '📋', label: '선거 관리', href: platformHref('/elections'), minimumRoles: ['OBSERVER'], section: 'main' },
  {
    icon: '👤',
    label: '후보자 관리',
    href: platformHref('/elections/current/candidates'),
    minimumRoles: ['ELECTION_OFFICER'],
    section: 'operations',
  },
  {
    icon: '🗳',
    label: '투표소 관리',
    href: platformHref('/elections/current/ballots'),
    minimumRoles: ['ELECTION_OFFICER'],
    section: 'operations',
  },
  { icon: '👥', label: '선거인 명부', href: platformHref('/voters'), minimumRoles: ['ELECTION_OFFICER'], section: 'operations' },
  {
    icon: '📊',
    label: '개표 결과',
    href: platformHref('/elections/current/results'),
    minimumRoles: ['OBSERVER', 'VOTER'],
    section: 'operations',
  },
  { icon: '📁', label: '감사 로그', href: platformHref('/audit'), minimumRoles: ['AUDITOR', 'SYSTEM_ADMIN'], section: 'audit' },
  { icon: '⚙️', label: '시스템 관리', href: platformHref('/admin'), minimumRoles: ['SYSTEM_ADMIN'], section: 'system' },
];

export function visibleNavigationItems(userRoles: Role[]): NavigationItem[] {
  const effectiveRoles = expandRoles(userRoles);
  return navigationItems.filter((item) => item.minimumRoles.some((role) => effectiveRoles.has(role)));
}

export function expandRoles(userRoles: Role[]): Set<Role> {
  const expanded = new Set<Role>();
  const visit = (role: Role) => {
    if (expanded.has(role)) {
      return;
    }
    expanded.add(role);
    roleHierarchy[role].forEach(visit);
  };
  userRoles.forEach(visit);
  return expanded;
}
