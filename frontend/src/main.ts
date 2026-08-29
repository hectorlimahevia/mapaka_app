import { createApp } from 'vue'
import { createPinia } from 'pinia'

import './assets/tokens.css'
import './assets/base.css'

import App from './App.vue'
import router from './router'
import { i18n, loadLocaleMessages, detectInitialLocale } from './i18n'
import { isNativePlatform } from './services/nfc'

/** El Service Worker de PWA (vite-plugin-pwa) només té sentit a web/iOS Safari — mai dins
 * de l'app empaquetada amb Capacitor. Registrar-lo allà provoca pantalla en blanc en
 * actualitzar l'APK: el SW vell segueix servint des de cache un index.html/JS que ja no
 * coincideix amb els fitxers (amb hash) del bundle nou (bug real trobat verificant el
 * Prompt 12 en un dispositiu real). Com a defensa per a instal·lacions que ja n'arrosseguin
 * un de registrat d'una versió anterior de l'app, es desregistra activament. */
async function setupServiceWorker() {
  if (isNativePlatform()) {
    if ('serviceWorker' in navigator) {
      const registrations = await navigator.serviceWorker.getRegistrations()
      await Promise.all(registrations.map((registration) => registration.unregister()))
    }
    if ('caches' in window) {
      const keys = await caches.keys()
      await Promise.all(keys.map((key) => caches.delete(key)))
    }
    return
  }

  const { registerSW } = await import('virtual:pwa-register')
  registerSW({ immediate: true })
}

async function bootstrap() {
  await loadLocaleMessages(detectInitialLocale())
  await setupServiceWorker()

  const app = createApp(App)

  app.use(createPinia())
  app.use(i18n)
  app.use(router)

  app.mount('#app')
}

bootstrap()
