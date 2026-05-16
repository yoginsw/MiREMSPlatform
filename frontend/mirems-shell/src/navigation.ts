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
  labelKey: string;
  /** @deprecated Use labelKey with i18n for user-facing text. */
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
  { icon: '🏠', labelKey: 'navigation.items.dashboard', label: '대시보드', href: platformHref('/'), minimumRoles: ['OBSERVER', 'VOTER'], section: 'main' },
  { icon: '📋', labelKey: 'navigation.items.elections', label: '선거 관리', href: platformHref('/elections'), minimumRoles: ['OBSERVER'], section: 'main' },
  {
    icon: '👤',
    labelKey: 'navigation.items.candidates',
    label: '후보자 관리',
    href: platformHref('/elections/current/candidates'),
    minimumRoles: ['ELECTION_OFFICER'],
    section: 'operations',
  },
  {
    icon: '🗳',
    labelKey: 'navigation.items.ballots',
    label: '투표소 관리',
    href: platformHref('/elections/current/ballots'),
    minimumRoles: ['ELECTION_OFFICER'],
    section: 'operations',
  },
  { icon: '👥', labelKey: 'navigation.items.voters', label: '선거인 명부', href: platformHref('/voters'), minimumRoles: ['ELECTION_OFFICER'], section: 'operations' },
  {
    icon: '📊',
    labelKey: 'navigation.items.results',
    label: '개표 결과',
    href: platformHref('/elections/current/results'),
    minimumRoles: ['OBSERVER', 'VOTER'],
    section: 'operations',
  },
  { icon: '📁', labelKey: 'navigation.items.auditLogs', label: '감사 로그', href: platformHref('/audit'), minimumRoles: ['AUDITOR', 'SYSTEM_ADMIN'], section: 'audit' },
  { icon: '⚙️', labelKey: 'navigation.items.systemAdmin', label: '시스템 관리', href: platformHref('/admin'), minimumRoles: ['SYSTEM_ADMIN'], section: 'system' },
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

export function getNavigationItemTranslationKey(href: string): string | undefined {
  return navigationItems.find((item) => item.href === href)?.labelKey;
}
