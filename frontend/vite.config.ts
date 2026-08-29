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
      // injectRegister: false — el registre és manual a main.ts, només per a web
      // (iOS PWA/navegador). L'app empaquetada amb Capacitor no ha de registrar mai
      // cap Service Worker: en actualitzar l'APK, un SW vell servint des de la cache
      // deixa la pantalla en blanc perquè referencia fitxers JS/CSS que ja no existeixen
      // al bundle nou (bug real trobat en verificar el Prompt 12 en un dispositiu real).
      injectRegister: false,
      manifest: {
        name: 'Mapaka',
        short_name: 'Mapaka',
        description: 'Gestió familiar de paga, tasques i temps de pantalla',
        lang: 'ca',
        theme_color: '#6C4DFF',
        background_color: '#FFFDF7',
        display: 'standalone',
        icons: [
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
