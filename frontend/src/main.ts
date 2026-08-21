import { createApp } from 'vue'
import { createPinia } from 'pinia'

import './assets/tokens.css'
import './assets/base.css'

import App from './App.vue'
import router from './router'
import { i18n, loadLocaleMessages, detectInitialLocale } from './i18n'

async function bootstrap() {
  await loadLocaleMessages(detectInitialLocale())

  const app = createApp(App)

  app.use(createPinia())
  app.use(i18n)
  app.use(router)

  app.mount('#app')
}

bootstrap()
