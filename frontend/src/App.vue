<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import MapakaLogoLoader from '@/components/base/MapakaLogoLoader.vue'

const { t } = useI18n()
const auth = useAuthStore()

// El primer arrencada de l'app pot quedar esperant tryRestoreSession() (backend
// "adormit" a Render, fins a un minut) — sense aquesta pantalla, l'usuari no veu res
// fins que respon. La pista addicional només apareix si l'espera es fa llarga de debò,
// perquè en el cas normal (backend ja despert) ni es plantegi.
const showHint = ref(false)
let hintTimeout: ReturnType<typeof setTimeout> | null = null

watch(
  () => auth.initialized,
  (initialized) => {
    if (initialized && hintTimeout) {
      clearTimeout(hintTimeout)
      hintTimeout = null
    }
  },
)

hintTimeout = setTimeout(() => {
  showHint.value = true
}, 4000)

onBeforeUnmount(() => {
  if (hintTimeout) clearTimeout(hintTimeout)
})
</script>

<template>
  <div v-if="!auth.initialized" class="boot-loading">
    <MapakaLogoLoader :size="72" />
    <p class="boot-loading__text">{{ t('common.wakingUp') }}</p>
    <p v-if="showHint" class="boot-loading__hint">{{ t('common.wakingUpHint') }}</p>
  </div>
  <RouterView v-else />
</template>

<style scoped>
.boot-loading {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  background: var(--bg);
  padding: 1.5rem;
  text-align: center;
}

.boot-loading__text {
  font-family: var(--font-heading);
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.boot-loading__hint {
  font-size: 0.82rem;
  color: var(--muted);
  max-width: 280px;
  margin: 0;
}
</style>
