import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: '/miremsplatform/',
  plugins: [react()],
  server: {
    allowedHosts: ['remote.ejbt.co.kr'],
  },
});
