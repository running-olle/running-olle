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
      manifest: {
        name: 'Running Olle',
        short_name: 'RunningOlle',
        description: '러닝과 여행을 연결하는 위치 기반 서비스',
        theme_color: '#ff6530',
        background_color: '#eef3f0',
        display: 'standalone',
        start_url: '/',
      },
    }),
  ],
  server: {
    host: true,
    port: 5173,
  },
})
