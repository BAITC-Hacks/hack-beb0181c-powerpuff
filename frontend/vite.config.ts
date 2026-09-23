import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy preserves the browser origin; allow that origin in backend CORS.
    proxy: {
      '/api': env.BACKEND_PROXY_TARGET || 'http://localhost:8080',
    },
  },
  };
})
