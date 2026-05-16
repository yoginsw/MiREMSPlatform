import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  resolve: {
    alias: {
      '@mirems/api-client': fileURLToPath(new URL('../packages/api-client/src', import.meta.url)),
      '@mirems/i18n': fileURLToPath(new URL('../packages/i18n/src', import.meta.url)),
      '@mirems/ui-core': fileURLToPath(new URL('../packages/ui-core/src', import.meta.url)),
    },
  },
  test: {
    coverage: {
      all: true,
      include: ['src/features/{elections,candidates,ballots,voters,voting,results,audit,admin}/**/*.{ts,tsx}', 'src/i18n/**/*.{ts,tsx}'],
      provider: 'v8',
      reporter: ['text', 'json-summary'],
      thresholds: {
        branches: 75,
        functions: 75,
        lines: 75,
        statements: 75,
      },
    },
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
});
