import { expandRoles, platformHref, type Role } from './navigation';

export interface DashboardAction {
  id: string;
  label: string;
  variant: 'primary' | 'secondary' | 'danger-ghost';
  roles: Role[];
}

export interface TaskNotification {
  id: string;
  title: string;
  meta: string;
  href: string;
  roles: Role[];
}

export const dashboardActions: DashboardAction[] = [
  { id: 'export', label: '내보내기', variant: 'secondary', roles: ['ELECTION_ADMIN', 'AUDITOR'] },
  { id: 'publication-review', label: '발행 검토', variant: 'primary', roles: ['ELECTION_ADMIN'] },
];

export const processActions: DashboardAction[] = [
  { id: 'approve-publication', label: '승인 ✓', variant: 'primary', roles: ['ELECTION_ADMIN'] },
  { id: 'reject-publication', label: '반려 ✕', variant: 'danger-ghost', roles: ['ELECTION_ADMIN'] },
];

export const taskNotifications: TaskNotification[] = [
  {
    id: 'task-1',
    title: '선거 발행 검토',
    meta: '관리자 검토 · 3일 2시간 남음',
    href: platformHref('/admin/processes'),
    roles: ['ELECTION_ADMIN'],
  },
  {
    id: 'task-2',
    title: '후보자 자격 심사',
    meta: 'ELECTION_OFFICER 필요 · 18건',
    href: platformHref('/elections/current/candidates'),
    roles: ['ELECTION_OFFICER'],
  },
];

export function visibleDashboardActions(userRoles: Role[]): DashboardAction[] {
  return visibleRoleItems(dashboardActions, userRoles);
}

export function visibleProcessActions(userRoles: Role[]): DashboardAction[] {
  return visibleRoleItems(processActions, userRoles);
}

export function visibleTaskNotifications(userRoles: Role[]): TaskNotification[] {
  return visibleRoleItems(taskNotifications, userRoles);
}

function visibleRoleItems<T extends { roles: Role[] }>(items: T[], userRoles: Role[]): T[] {
  const effectiveRoles = expandRoles(userRoles);
  return items.filter((item) => item.roles.some((role) => effectiveRoles.has(role)));
}
