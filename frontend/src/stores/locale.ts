import { defineStore } from 'pinia'
import { computed } from 'vue'
import api from '@/services/api'
import { i18n, setAppLocale, type AppLocale } from '@/i18n'

/** Idioma actiu — envolta setAppLocale (vue-i18n + localStorage) i, quan hi ha sessió,
 * sincronitza el canvi amb el backend perquè es recuperi en entrar des d'un altre
 * dispositiu (Prompt 5). */
export const useLocaleStore = defineStore('locale', () => {
  const current = computed(() => i18n.global.locale.value as AppLocale)

  async function setLocale(locale: AppLocale, userId?: string | null) {
    await setAppLocale(locale)
    if (userId) {
      await api.patch(`/api/users/${userId}/locale`, { locale }).catch(() => {})
    }
  }

  return { current, setLocale }
})
