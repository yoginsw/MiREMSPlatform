import { describe, expect, it } from 'vitest';
import { getNavigationItemTranslationKey, visibleNavigationItems } from './navigation';

describe('i18n navigation metadata', () => {
  it('maps each visible navigation item to a stable translation key instead of hardcoded UI labels', () => {
    const visibleItems = visibleNavigationItems(['SYSTEM_ADMIN']);

    expect(visibleItems.map((item) => item.labelKey)).toEqual([
      'navigation.items.dashboard',
      'navigation.items.elections',
      'navigation.items.candidates',
      'navigation.items.ballots',
      'navigation.items.voters',
      'navigation.items.results',
      'navigation.items.auditLogs',
      'navigation.items.systemAdmin',
    ]);
    expect(visibleItems.map((item) => getNavigationItemTranslationKey(item.href))).toContain('navigation.items.auditLogs');
  });
});
