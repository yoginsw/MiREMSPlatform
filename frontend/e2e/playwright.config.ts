import { defineConfig, devices } from '@playwright/test';

const shellBaseUrl = process.env.MIREMS_E2E_BASE_URL ?? 'http://127.0.0.1:5173/miremsplatform/';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    baseURL: shellBaseUrl,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'election-lifecycle',
      testMatch: /election-lifecycle\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'voting',
      testMatch: /voting-critical-path\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'tabulation-certification',
      testMatch: /tabulation-certification\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: process.env.MIREMS_E2E_START_SHELL === 'true'
    ? {
        command: 'pnpm --filter @mirems/mirems-shell dev -- --host 127.0.0.1',
        cwd: '..',
        url: shellBaseUrl,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      }
    : undefined,
});
