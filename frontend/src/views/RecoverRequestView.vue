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

const hasOtherAdult = ref<'unknown' | 'yes' | 'no'>('unknown')

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

      <template v-if="hasOtherAdult === 'unknown'">
        <p class="recover__hint">{{ t('login.recoverOtherAdultQuestion') }}</p>
        <div class="recover__choice-row">
          <button type="button" class="recover__choice" @click="hasOtherAdult = 'yes'">{{ t('common.yes') }}</button>
          <button type="button" class="recover__choice" @click="hasOtherAdult = 'no'">{{ t('common.no') }}</button>
        </div>
      </template>

      <template v-else-if="hasOtherAdult === 'yes'">
        <button type="button" class="recover__back" @click="hasOtherAdult = 'unknown'">← {{ t('login.recoverBack') }}</button>
        <p class="recover__prompt recover__prompt--sub">{{ t('login.recoverOtherAdultTitle') }}</p>
        <ol class="recover__steps">
          <li>{{ t('login.recoverOtherAdultStep1') }}</li>
          <li>{{ t('login.recoverOtherAdultStep2') }}</li>
          <li>{{ t('login.recoverOtherAdultStep3') }}</li>
          <li>{{ t('login.recoverOtherAdultStep4') }}</li>
        </ol>
        <BaseButton type="button" variant="ghost" @click="router.push({ name: 'login' })">
          {{ t('login.backToLogin') }}
        </BaseButton>
      </template>

      <template v-else>
        <button type="button" class="recover__back" @click="hasOtherAdult = 'unknown'">← {{ t('login.recoverBack') }}</button>

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
          <p class="recover__hint">{{ t('login.recoverCodeHint') }}</p>
          <label>
            {{ t('login.recoverCodeLabel') }}
            <input v-model="code" type="text" :placeholder="t('login.recoverCodePlaceholder')" required autofocus />
          </label>
          <p v-if="error" class="recover__error">{{ error }}</p>
          <BaseButton type="submit" variant="primary" :disabled="loading">
            {{ loading ? t('login.recoverChecking') : t('login.recoverContinue') }}
          </BaseButton>
        </form>
      </template>
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

.recover__prompt--sub {
  font-size: 0.92rem;
}

.recover__choice-row {
  display: flex;
  gap: 0.6rem;
}

.recover__choice {
  flex: 1;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
  padding: 0.65rem 0.5rem;
  border-radius: 12px;
  border: 2px solid color-mix(in srgb, var(--primary) 18%, transparent);
  background: white;
  color: var(--text);
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.recover__choice:hover {
  border-color: var(--primary);
}

@media (prefers-reduced-motion: reduce) {
  .recover__choice {
    transition: none;
  }
}

.recover__steps {
  background: color-mix(in srgb, var(--text) 4%, white);
  border-radius: 12px;
  padding: 0.85rem 0.9rem 0.85rem 1.6rem;
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.7;
  color: var(--text);
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
