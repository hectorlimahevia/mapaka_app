import type { AppLocale } from '@/i18n'

/** Les dates sí que es formaten segons l'idioma actiu (Prompt 5) — a diferència dels
 * imports, és només una etiqueta de lectura ("20 d'agost" / "20 de agosto" / "August 20"). */
const INTL_TAG: Record<AppLocale, string> = { ca: 'ca-ES', es: 'es-ES', en: 'en-GB' }

export function formatDate(value: Date | string, locale: AppLocale, options?: Intl.DateTimeFormatOptions): string {
  const date = typeof value === 'string' ? new Date(value) : value
  return new Intl.DateTimeFormat(INTL_TAG[locale], options ?? { day: 'numeric', month: 'long' }).format(date)
}
