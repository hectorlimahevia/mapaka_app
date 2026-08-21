<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useLocaleStore } from '@/stores/locale'
import { SUPPORTED_LOCALES, type AppLocale } from '@/i18n'

const { t } = useI18n()
const auth = useAuthStore()
const localeStore = useLocaleStore()

const LABELS: Record<AppLocale, string> = { ca: 'CA', es: 'ES', en: 'EN' }

function choose(locale: AppLocale) {
  localeStore.setLocale(locale, auth.isAuthenticated ? auth.userId : null)
}
</script>

<template>
  <div class="language-switcher" role="group" :aria-label="t('common.language')">
    <button
      v-for="locale in SUPPORTED_LOCALES"
      :key="locale"
      type="button"
      class="language-switcher__option"
      :class="{ 'language-switcher__option--active': localeStore.current === locale }"
      @click="choose(locale)"
    >
      {{ LABELS[locale] }}
    </button>
  </div>
</template>

<style scoped>
.language-switcher {
  display: inline-flex;
  gap: 0.25rem;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.2rem;
}

.language-switcher__option {
  border: none;
  background: transparent;
  padding: 0.3rem 0.65rem;
  border-radius: 999px;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.72rem;
  color: var(--muted);
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease;
}

.language-switcher__option--active {
  background: var(--primary);
  color: white;
}
</style>
