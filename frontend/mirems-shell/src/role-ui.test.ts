import { describe, expect, it } from 'vitest';
import { visibleDashboardActions, visibleProcessActions, visibleTaskNotifications } from './role-ui';

describe('role-based dashboard UI', () => {
  it('hides administrator actions from observer users', () => {
    expect(visibleDashboardActions(['OBSERVER'])).toEqual([]);
    expect(visibleProcessActions(['OBSERVER'])).toEqual([]);
    expect(visibleTaskNotifications(['OBSERVER'])).toEqual([]);
  });

  it('shows review actions to election administrators', () => {
    expect(visibleDashboardActions(['ELECTION_ADMIN']).map((action) => action.label)).toEqual(['내보내기', '발행 검토']);
    expect(visibleProcessActions(['ELECTION_ADMIN']).map((action) => action.label)).toEqual(['승인 ✓', '반려 ✕']);
  });

  it('shows officer task notifications to election officers and higher roles', () => {
    expect(visibleTaskNotifications(['ELECTION_OFFICER']).map((task) => task.title)).toContain('후보자 자격 심사');
    expect(visibleTaskNotifications(['SYSTEM_ADMIN']).map((task) => task.title)).toContain('선거 발행 검토');
  });
});
