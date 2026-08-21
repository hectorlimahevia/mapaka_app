import { createI18n } from 'vue-i18n'

/** Prompt 5 (i18n): vue-i18n amb Composition API i càrrega diferida — el bundle inicial
 * només inclou el JSON de l'idioma actiu, la resta es carrega sota demanda. */
export const SUPPORTED_LOCALES = ['ca', 'es', 'en'] as const
export type AppLocale = (typeof SUPPORTED_LOCALES)[number]

const STORAGE_KEY = 'mapaka-locale'
const DEFAULT_LOCALE: AppLocale = 'ca'

function isSupportedLocale(value: string | null): value is AppLocale {
  return !!value && (SUPPORTED_LOCALES as readonly string[]).includes(value)
}

/** localStorage té prioritat; si no n'hi ha, es dedueix de navigator.language; qualsevol
 * altre idioma cau al català per defecte. */
export function detectInitialLocale(): AppLocale {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (isSupportedLocale(stored)) return stored

  const navLang = navigator.language?.slice(0, 2).toLowerCase() ?? null
  return isSupportedLocale(navLang) ? navLang : DEFAULT_LOCALE
}

export const i18n = createI18n<false>({
  legacy: false,
  locale: detectInitialLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {},
})

const loadedLocales = new Set<AppLocale>()

const localeLoaders: Record<AppLocale, () => Promise<{ default: Record<string, unknown> }>> = {
  ca: () => import('./locales/ca.json'),
  es: () => import('./locales/es.json'),
  en: () => import('./locales/en.json'),
}

export async function loadLocaleMessages(locale: AppLocale) {
  if (loadedLocales.has(locale)) return
  const messages = await localeLoaders[locale]()
  i18n.global.setLocaleMessage(locale, messages.default)
  loadedLocales.add(locale)
}

/** Canvia l'idioma actiu: carrega el JSON si cal, actualitza vue-i18n, persisteix a
 * localStorage i sincronitza l'atribut lang de <html> (accessibilitat/SEO bàsic). */
export async function setAppLocale(locale: AppLocale) {
  await loadLocaleMessages(locale)
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  document.documentElement.setAttribute('lang', locale)
}
