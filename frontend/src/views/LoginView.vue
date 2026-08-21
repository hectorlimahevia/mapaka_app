<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import type { FamilySummary, LoginProfile, UserRole } from '@/types/auth'

const router = useRouter()
const auth = useAuthStore()

const mode = ref<'adult' | 'child'>('adult')
const loading = ref(false)
const error = ref<string | null>(null)

// --- Família → perfil → PIN, igual per a tots dos rols (Prompt 6) ---
const step = ref<'family' | 'profile' | 'pin'>('family')
const familyQuery = ref('')
const familyResults = ref<FamilySummary[]>([])
const selectedFamily = ref<FamilySummary | null>(null)
const profiles = ref<LoginProfile[]>([])
const selectedProfile = ref<LoginProfile | null>(null)
const pin = ref('')

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

function profilesForMode(all: LoginProfile[], targetMode: 'adult' | 'child'): LoginProfile[] {
  const role: UserRole = targetMode === 'adult' ? 'PARENT' : 'CHILD'
  return all.filter((profile) => profile.role === role)
}

async function selectFamily(family: FamilySummary) {
  error.value = null
  selectedFamily.value = family
  loading.value = true
  try {
    const { data } = await api.get<LoginProfile[]>(`/api/families/${family.id}/login-profiles`)
    profiles.value = data
    step.value = 'profile'
  } finally {
    loading.value = false
  }
}

function selectProfile(profile: LoginProfile) {
  selectedProfile.value = profile
  pin.value = ''
  error.value = null
  step.value = 'pin'
}

function backTo(target: 'family' | 'profile') {
  error.value = null
  step.value = target
}

async function submitPin() {
  if (!selectedFamily.value || !selectedProfile.value) return
  error.value = null
  loading.value = true
  try {
    await auth.login({
      familyId: selectedFamily.value.id,
      username: selectedProfile.value.username,
      password: pin.value,
    })
    await goHome()
  } catch {
    error.value = 'PIN incorrecte.'
  } finally {
    loading.value = false
  }
}

async function goHome() {
  await router.push(auth.role === 'PARENT' ? { name: 'parent-resum' } : { name: 'child-inici' })
}

function switchMode(next: 'adult' | 'child') {
  mode.value = next
  error.value = null
  step.value = 'family'
  familyQuery.value = ''
  familyResults.value = []
  selectedFamily.value = null
  selectedProfile.value = null
}

const visibleProfiles = () => profilesForMode(profiles.value, mode.value)
</script>

<template>
  <div class="login">
    <div class="login__brand">
      <img src="@/assets/mapaka-logo.svg" alt="" width="56" height="56" />
      <h1>Mapaka</h1>
    </div>

    <div class="login__toggle">
      <button type="button" :class="{ active: mode === 'child' }" @click="switchMode('child')">Sóc un fill</button>
      <button type="button" :class="{ active: mode === 'adult' }" @click="switchMode('adult')">Sóc un adult</button>
    </div>

    <BaseCard class="login__card">
      <div class="login__form">
        <template v-if="step === 'family'">
          <label>
            Nom de la família
            <input v-model="familyQuery" type="text" placeholder="Sande-Lima" autocomplete="off" />
          </label>
          <ul v-if="familyResults.length" class="login__list">
            <li v-for="family in familyResults" :key="family.id">
              <button type="button" @click="selectFamily(family)">{{ family.name }}</button>
            </li>
          </ul>
        </template>

        <template v-else-if="step === 'profile'">
          <button type="button" class="login__back" @click="backTo('family')">← Canviar família</button>
          <p class="login__prompt">Qui ets?</p>
          <ul v-if="visibleProfiles().length" class="login__profiles">
            <li v-for="profile in visibleProfiles()" :key="profile.username">
              <button type="button" class="login__profile" @click="selectProfile(profile)">
                <img v-if="profile.avatar" :src="profile.avatar" alt="" />
                <span v-else class="login__profile-fallback">{{ profile.displayName.charAt(0) }}</span>
                <span>{{ profile.displayName }}</span>
              </button>
            </li>
          </ul>
          <p v-else class="login__empty">
            Aquesta família encara no té cap {{ mode === 'adult' ? 'adult' : 'fill' }} donat d'alta.
          </p>
        </template>

        <template v-else>
          <button type="button" class="login__back" @click="backTo('profile')">← {{ selectedProfile?.displayName }} no sóc jo</button>
          <form @submit.prevent="submitPin">
            <label>
              PIN de {{ selectedProfile?.displayName }}
              <input v-model="pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required autofocus />
            </label>
            <p v-if="error" class="login__error">{{ error }}</p>
            <BaseButton type="submit" variant="accent" :disabled="loading">
              {{ loading ? 'Entrant…' : 'Entrar' }}
            </BaseButton>
          </form>
          <RouterLink :to="{ name: 'recover-request' }" class="login__forgot">Has oblidat el PIN?</RouterLink>
        </template>
      </div>
    </BaseCard>

    <RouterLink v-if="step === 'family'" :to="{ name: 'register-family' }" class="login__register">
      Ets nou a Mapaka? Crea una família →
    </RouterLink>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  padding: 2rem 1.5rem;
}

.login__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.login__brand h1 {
  font-size: 1.75rem;
}

.login__toggle {
  display: inline-flex;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.25rem;
}

.login__toggle button {
  border: none;
  background: transparent;
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
  font-family: var(--font-heading);
  font-weight: 700;
  color: var(--muted);
  cursor: pointer;
  transition:
    background 0.2s ease,
    color 0.2s ease;
}

.login__toggle button.active {
  background: var(--primary);
  color: white;
}

.login__card {
  width: 100%;
  max-width: 360px;
}

.login__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.login__form form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.login__form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 700;
  font-size: 0.9rem;
}

.login__form input {
  font: inherit;
  padding: 0.65rem 0.85rem;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
  background: white;
}

.login__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.login__empty {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0;
}

.login__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.login__list button {
  width: 100%;
  text-align: left;
  padding: 0.6rem 0.85rem;
  border-radius: 12px;
  border: none;
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  font-weight: 700;
  cursor: pointer;
}

.login__prompt {
  font-family: var(--font-heading);
  font-weight: 700;
  margin: 0;
}

.login__profiles {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 0.75rem;
}

.login__profile {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  border: none;
  background: none;
  cursor: pointer;
  font-weight: 700;
  color: var(--text);
}

.login__profile img,
.login__profile-fallback {
  width: 56px;
  height: 56px;
  border-radius: 999px;
  object-fit: cover;
  background: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 1.4rem;
}

.login__back {
  align-self: flex-start;
  border: none;
  background: none;
  color: var(--muted);
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

.login__forgot {
  align-self: center;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--muted);
  text-decoration: none;
}

.login__register {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--primary);
  text-decoration: none;
}
</style>
