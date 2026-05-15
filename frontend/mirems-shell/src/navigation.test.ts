import { describe, expect, it } from 'vitest';
import { visibleNavigationItems } from './navigation';

describe('visibleNavigationItems', () => {
  it('shows audit logs only to auditor and system admin roles under the platform base path', () => {
    expect(visibleNavigationItems(['AUDITOR']).map((item) => item.href)).toContain('/miremsplatform/audit');
    expect(visibleNavigationItems(['SYSTEM_ADMIN']).map((item) => item.href)).toContain('/miremsplatform/audit');
    expect(visibleNavigationItems(['VOTER']).map((item) => item.href)).not.toContain('/miremsplatform/audit');
  });

  it('shows officer work areas through the election admin hierarchy under the platform base path', () => {
    const hrefs = visibleNavigationItems(['ELECTION_ADMIN']).map((item) => item.href);

    expect(hrefs).toContain('/miremsplatform/elections/current/candidates');
    expect(hrefs).toContain('/miremsplatform/elections/current/ballots');
    expect(hrefs).toContain('/miremsplatform/voters');
  });
});
