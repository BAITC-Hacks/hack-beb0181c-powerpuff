import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Все запросы на /api идут на Spring Boot — CORS в dev не нужен
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
