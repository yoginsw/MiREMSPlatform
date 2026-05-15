import { describe, expect, it } from 'vitest';
import { visibleNavigationItems } from './navigation';

describe('visibleNavigationItems', () => {
  it('shows audit logs only to auditor and system admin roles', () => {
    expect(visibleNavigationItems(['AUDITOR']).map((item) => item.href)).toContain('/audit');
    expect(visibleNavigationItems(['SYSTEM_ADMIN']).map((item) => item.href)).toContain('/audit');
    expect(visibleNavigationItems(['VOTER']).map((item) => item.href)).not.toContain('/audit');
  });

  it('shows officer work areas through the election admin hierarchy', () => {
    const hrefs = visibleNavigationItems(['ELECTION_ADMIN']).map((item) => item.href);

    expect(hrefs).toContain('/elections/current/candidates');
    expect(hrefs).toContain('/elections/current/ballots');
    expect(hrefs).toContain('/voters');
  });
});
