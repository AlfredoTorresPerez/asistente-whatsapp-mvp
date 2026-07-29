import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replaceAll('\\', '/')
          if (normalizedId.includes('node_modules/d3')) {
            return 'd3'
          }
          if (normalizedId.includes('node_modules/recharts')) {
            if (normalizedId.includes('/cartesian/') || normalizedId.includes('/polar/')) {
              return 'charts-shapes'
            }
            if (normalizedId.includes('/component/') || normalizedId.includes('/container/')) {
              return 'charts-components'
            }
            return 'charts-core'
          }
          if (normalizedId.includes('node_modules/@tanstack/react-query')) {
            return 'query'
          }
          if (
            normalizedId.includes('node_modules/react') ||
            normalizedId.includes('node_modules/react-dom') ||
            normalizedId.includes('node_modules/react-router-dom')
          ) {
            return 'react'
          }
          const moduleMatch = normalizedId.match(/\/src\/modules\/([^/]+)\//)
          if (moduleMatch?.[1]) {
            return `module-${moduleMatch[1]}`
          }
          return undefined
        },
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: ['localhost', '127.0.0.1', 'frontend-react', '.trycloudflare.com', '.ngrok-free.dev'],
    proxy: process.env.VITE_PROXY_API
      ? {
          '/api/v1': {
            target: process.env.VITE_PROXY_API,
            changeOrigin: true,
          },
        }
      : undefined,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**'],
  },
})
