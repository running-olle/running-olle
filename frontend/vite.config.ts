import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        cleanupOutdatedCaches: true,
        navigateFallbackDenylist: [
          /^\/api\//,
          /^\/oauth2\//,
          /^\/login\/oauth2\//,
          /^\/uploads\//,
        ],
      },
      manifest: {
        name: 'Running Olle',
        short_name: 'RunningOlle',
        description: '제주를 달리며 여행하는 러닝 플랫폼',
        theme_color: '#ffffff',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
      },
    }),
  ],
  server: { port: 5173 },
})
