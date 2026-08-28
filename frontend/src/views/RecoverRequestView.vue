<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useRecoveryStore } from '@/stores/recovery'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import MapakaLogo from '@/components/base/MapakaLogo.vue'
import { apiErrorMessage } from '@/utils/apiError'
import type { FamilySummary, RecoverResponse } from '@/types/auth'

const { t } = useI18n()
const router = useRouter()
const recovery = useRecoveryStore()

const loading = ref(false)
const error = ref<string | null>(null)

const familyQuery = ref('')
const familyResults = ref<FamilySummary[]>([])
const selectedFamily = ref<FamilySummary | null>(null)
const code = ref('')

let searchTimeout: ReturnType<typeof setTimeout>
watch(familyQuery, (q) => {
  clearTimeout(searchTimeout)
  if (q.trim().length < 2) {
    familyResults.value = []
    return
  }
  searchTimeout = setTimeout(async () => {
    const { data } = await api.get<FamilySummary[]>('/api/families/lookup', { params: { q } })
    familyResults.value = data
  }, 300)
})

function selectFamily(family: FamilySummary) {
  selectedFamily.value = family
  familyResults.value = []
}

async function submit() {
  if (!selectedFamily.value) return
  error.value = null
  loading.value = true
  try {
    const { data } = await api.post<RecoverResponse>('/api/auth/recover', {
      familyId: selectedFamily.value.id,
      recoveryCode: code.value.trim().toUpperCase(),
    })
    recovery.setToken(data.recoveryToken)
    await router.push({ name: 'recover-set-pin' })
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="recover">
    <MapakaLogo class="recover__brand" />

    <BaseCard class="recover__card">
      <p class="recover__prompt">{{ t('login.forgotPin') }}</p>
      <p class="recover__hint">{{ t('login.recoverHint') }}</p>

      <template v-if="!selectedFamily">
        <label>
          {{ t('login.familyNameLabel') }}
          <input v-model="familyQuery" type="text" :placeholder="t('login.familyNamePlaceholder')" autocomplete="off" />
        </label>
        <ul v-if="familyResults.length" class="recover__list">
          <li v-for="family in familyResults" :key="family.id">
            <button type="button" @click="selectFamily(family)">{{ family.name }}</button>
          </li>
        </ul>
      </template>

      <form v-else class="recover__form" @submit.prevent="submit">
        <button type="button" class="recover__back" @click="selectedFamily = null">← {{ t('login.changeFamily') }}</button>
        <label>
          {{ t('login.recoverCodeLabel') }}
          <input v-model="code" type="text" :placeholder="t('login.recoverCodePlaceholder')" required autofocus />
        </label>
        <p v-if="error" class="recover__error">{{ error }}</p>
        <BaseButton type="submit" variant="primary" :disabled="loading">
          {{ loading ? t('login.recoverChecking') : t('login.recoverContinue') }}
        </BaseButton>
      </form>
    </BaseCard>

    <RouterLink :to="{ name: 'login' }" class="recover__cancel text-link-underline">← {{ t('login.backToLogin') }}</RouterLink>
  </div>
</template>

<style scoped>
.recover {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  padding: 2rem 1.5rem;
}

.recover__card {
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.recover__prompt {
  font-family: var(--font-heading);
  font-weight: 700;
  margin: 0;
}

.recover__hint {
  font-size: 0.82rem;
  color: var(--muted);
  line-height: 1.5;
  margin: 0;
}

.recover__card label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 700;
  font-size: 0.9rem;
}

.recover__card input {
  font: inherit;
  padding: 0.65rem 0.85rem;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
  background: white;
}

.recover__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.recover__list button {
  width: 100%;
  text-align: left;
  padding: 0.6rem 0.85rem;
  border-radius: 12px;
  border: none;
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  font-weight: 700;
  cursor: pointer;
}

.recover__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.recover__back {
  align-self: flex-start;
  border: none;
  background: none;
  color: var(--muted);
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.recover__back:hover {
  color: var(--text);
}

@media (prefers-reduced-motion: reduce) {
  .recover__back {
    transition: none;
  }
}

.recover__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.recover__cancel {
  font-size: 0.82rem;
  color: var(--muted);
  text-decoration: none;
}
</style>
