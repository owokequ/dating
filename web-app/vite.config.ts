import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '..', 'VITE_')
  const allowedHosts = env.VITE_DEV_ALLOWED_HOSTS
    ?.split(',')
    .map((host) => host.trim())
    .filter(Boolean)

  return {
    plugins: [react()],
    server: {
      port: 5173,
      allowedHosts,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/.well-known': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
