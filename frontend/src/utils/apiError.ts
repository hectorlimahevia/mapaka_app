import { i18n } from '@/i18n'

/** Tradueix un error de backend pel seu codi semàntic (Família+.pdf secció 61 / Prompt 4) —
 * el backend mai envia text directament, sempre un `code` estable que aquí es mapeja a
 * `errors.<CODE>` (Prompt 5). Cau a `errors.GENERIC` si el codi no té traducció. */
export function apiErrorMessage(error: unknown): string {
  const code = (error as { response?: { data?: { code?: string } } })?.response?.data?.code
  const { t, te } = i18n.global
  if (code && te(`errors.${code}`)) {
    return t(`errors.${code}`)
  }
  return t('errors.GENERIC')
}
