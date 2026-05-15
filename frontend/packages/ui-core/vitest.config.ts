import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      all: true,
      exclude: ['dist/**', 'src/**/*.d.ts', 'src/**/*.stories.tsx', 'src/**/*.test.tsx', 'src/test-setup.ts'],
      include: ['src/**/*.{ts,tsx}'],
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
