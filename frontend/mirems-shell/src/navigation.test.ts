import { describe, expect, it } from 'vitest';
import { getNavigationTarget, routerPathFromPlatformHref, visibleNavigationItems } from './navigation';

describe('visibleNavigationItems', () => {
  it('shows audit logs only to auditor and system admin roles under the platform base path', () => {
    expect(visibleNavigationItems(['AUDITOR']).map((item) => item.href)).toContain('/miremsplatform/audit');
    expect(visibleNavigationItems(['SYSTEM_ADMIN']).map((item) => item.href)).toContain('/miremsplatform/audit');
    expect(visibleNavigationItems(['VOTER']).map((item) => item.href)).not.toContain('/miremsplatform/audit');
  });

  it('shows officer work areas through the election admin hierarchy under the platform base path', () => {
    const hrefs = visibleNavigationItems(['ELECTION_ADMIN']).map((item) => item.href);

    expect(hrefs).toContain('/miremsplatform/elections/current/contests');
    expect(hrefs).toContain('/miremsplatform/elections/current/ballots');
    expect(hrefs).toContain('/miremsplatform/voters');
  });
});

describe('routerPathFromPlatformHref', () => {
  it('converts platform hrefs to TanStack Router paths so shell navigation does not reload the app', () => {
    expect(routerPathFromPlatformHref('/miremsplatform')).toBe('/');
    expect(routerPathFromPlatformHref('/miremsplatform/elections')).toBe('/elections');
    expect(routerPathFromPlatformHref('/miremsplatform/voters')).toBe('/voters');
    expect(routerPathFromPlatformHref('/external-dashboard')).toBe('/external-dashboard');
  });
});

describe('getNavigationTarget', () => {
  it('disables current-election placeholder links until an election scope is selected', () => {
    const candidateItem = visibleNavigationItems(['ELECTION_ADMIN']).find(
      (item) => item.labelKey === 'navigation.items.candidates',
    );

    expect(candidateItem).toBeDefined();
    expect(getNavigationTarget(candidateItem!)).toEqual({
      disabled: true,
      reasonKey: 'navigation.disabled.selectElectionFirst',
    });
  });

  it('returns router paths for routable items', () => {
    const electionItem = visibleNavigationItems(['OBSERVER']).find((item) => item.labelKey === 'navigation.items.elections');

    expect(electionItem).toBeDefined();
    expect(getNavigationTarget(electionItem!)).toEqual({
      disabled: false,
      to: '/elections',
    });
  });
});
