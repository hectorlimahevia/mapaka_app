import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Mapaka',
        short_name: 'Mapaka',
        description: 'Gestió familiar de paga, tasques i temps de pantalla',
        lang: 'ca',
        theme_color: '#6C4DFF',
        background_color: '#FFFDF7',
        display: 'standalone',
        // Icones 192x192 i 512x512 pendents del sistema de disseny (Fase 2)
        icons: [],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
