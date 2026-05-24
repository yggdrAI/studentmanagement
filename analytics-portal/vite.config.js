import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  define: {
    // Provide a build-time replacement so modules expecting `global` resolve
    // to the browser's globalThis.
    global: 'globalThis'
  },
  server: {
    host: '0.0.0.0',
    port: 5173
  }
});
