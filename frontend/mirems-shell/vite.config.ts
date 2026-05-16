import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { TanStackRouterVite } from '@tanstack/router-plugin/vite';

export default defineConfig({
  base: '/miremsplatform/',
  plugins: [
    TanStackRouterVite({
      target: 'react',
      routesDirectory: './src/routes',
      generatedRouteTree: './src/routeTree.gen.ts',
    }),
    react(),
  ],
  resolve: {
    alias: {
      '@mirems/api-client': fileURLToPath(new URL('../packages/api-client/src', import.meta.url)),
      '@mirems/i18n': fileURLToPath(new URL('../packages/i18n/src', import.meta.url)),
      '@mirems/ext-kr-ui': fileURLToPath(new URL('../extensions/ext-kr-ui/src', import.meta.url)),
      '@mirems/ui-core': fileURLToPath(new URL('../packages/ui-core/src', import.meta.url)),
    },
  },
  server: {
    allowedHosts: ['remote.ejbt.co.kr'],
  },
});
