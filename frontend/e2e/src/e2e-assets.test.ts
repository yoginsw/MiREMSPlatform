import { describe, expect, it } from 'vitest';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(__dirname, '..');

function read(relativePath: string): string {
  return readFileSync(path.join(root, relativePath), 'utf8');
}

describe('P9 E2E and load-test assets', () => {
  it('defines Playwright projects for critical election paths', () => {
    expect(existsSync(path.join(root, 'playwright.config.ts'))).toBe(true);
    const config = read('playwright.config.ts');

    for (const projectName of ['election-lifecycle', 'voting', 'tabulation-certification']) {
      expect(config).toContain(`name: '${projectName}'`);
    }
  });

  it('contains Playwright specs for lifecycle, voting, tabulation, and certification', () => {
    const expectedSpecs = [
      'tests/election-lifecycle.spec.ts',
      'tests/voting-critical-path.spec.ts',
      'tests/tabulation-certification.spec.ts',
    ];

    for (const spec of expectedSpecs) {
      expect(existsSync(path.join(root, spec))).toBe(true);
      const content = read(spec);
      expect(content).toContain('test(');
      expect(content).not.toContain('test.skip');
    }
  });

  it('defines a k6 10k-concurrent-voter load profile and chaos smoke scenario', () => {
    expect(read('load/k6-voting-load.js')).toContain('target: 10000');
    expect(read('load/k6-voting-load.js')).toContain('mirems_voting_success_rate');
    expect(read('chaos/k6-api-chaos.js')).toContain('chaosRate');
    expect(read('chaos/k6-api-chaos.js')).toContain('MIREMS_CHAOS_ENABLED');
  });

  it('documents a full VVSG 2.0 compliance checklist run for E2E evidence', () => {
    const checklist = read('../../docs/vvsg/VVSG2_E2E_CHECKLIST.md');
    for (const heading of [
      'Election lifecycle',
      'Voting session',
      'Tabulation and certification',
      'Audit trail and chain of custody',
      'Accessibility and usability',
      'Security and privacy',
      'Load and resilience',
    ]) {
      expect(checklist).toContain(heading);
    }
  });
});
