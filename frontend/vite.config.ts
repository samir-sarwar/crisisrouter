import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        secure: false,
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        secure: false,
      },
      '/login/oauth2': {
        target: 'http://localhost:8080',
        secure: false,
      },
      '/logout': {
        target: 'http://localhost:8080',
        secure: false,
      },
    }
  }
})
