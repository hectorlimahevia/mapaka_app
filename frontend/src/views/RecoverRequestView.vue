<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useRecoveryStore } from '@/stores/recovery'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import type { FamilySummary, RecoverResponse } from '@/types/auth'

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
  } catch {
    error.value = 'Codi de recuperació no vàlid.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="recover">
    <div class="recover__brand">
      <img src="@/assets/mapaka-logo.svg" alt="" width="56" height="56" />
      <h1>Mapaka</h1>
    </div>

    <BaseCard class="recover__card">
      <p class="recover__prompt">Has oblidat el PIN?</p>
      <p class="recover__hint">
        Si un altre pare o mare de la família encara té accés, és més fàcil que et reseteixi el PIN des de
        Configuració → Fills i pares. Si no, introdueix aquí el codi de recuperació que es va mostrar en crear
        la família.
      </p>

      <template v-if="!selectedFamily">
        <label>
          Nom de la família
          <input v-model="familyQuery" type="text" placeholder="Sande-Lima" autocomplete="off" />
        </label>
        <ul v-if="familyResults.length" class="recover__list">
          <li v-for="family in familyResults" :key="family.id">
            <button type="button" @click="selectFamily(family)">{{ family.name }}</button>
          </li>
        </ul>
      </template>

      <form v-else class="recover__form" @submit.prevent="submit">
        <button type="button" class="recover__back" @click="selectedFamily = null">← Canviar família</button>
        <label>
          Codi de recuperació
          <input v-model="code" type="text" placeholder="ABCD1234" required autofocus />
        </label>
        <p v-if="error" class="recover__error">{{ error }}</p>
        <BaseButton type="submit" variant="primary" :disabled="loading">
          {{ loading ? 'Comprovant…' : 'Continuar' }}
        </BaseButton>
      </form>
    </BaseCard>

    <RouterLink :to="{ name: 'login' }" class="recover__cancel">← Tornar a l'inici de sessió</RouterLink>
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

.recover__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.recover__brand h1 {
  font-size: 1.75rem;
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
