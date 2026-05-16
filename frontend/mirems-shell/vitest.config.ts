import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      all: true,
      include: ['src/features/{elections,candidates,ballots,voters,voting,results,audit,admin}/**/*.{ts,tsx}'],
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
