<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import type { FamilyRegisterResponse } from '@/types/auth'

const router = useRouter()
const auth = useAuthStore()

const step = ref<1 | 2 | 3 | 4>(1)
const loading = ref(false)
const error = ref<string | null>(null)

const familyName = ref('')

const parent = reactive({ displayName: '', pin: '', pinConfirm: '' })
const recoveryCode = ref('')
const codeCopied = ref(false)
const savedConfirmed = ref(false)

const COLORS = ['#6C4DFF', '#FF5D8F', '#FFC93C', '#2ECC71', '#3AA0FF']
const child = reactive({ displayName: '', birthDate: '', colorTheme: COLORS[0], pin: '', pinConfirm: '' })
const addedChildren = ref<string[]>([])
const savingChild = ref(false)

function nextFromFamily() {
  if (!familyName.value.trim()) return
  step.value = 2
}

async function registerParent() {
  error.value = null
  if (!/^\d{4}$/.test(parent.pin)) {
    error.value = 'El PIN ha de tenir exactament 4 dígits.'
    return
  }
  if (parent.pin !== parent.pinConfirm) {
    error.value = 'Els PIN no coincideixen.'
    return
  }
  loading.value = true
  try {
    const { data } = await api.post<FamilyRegisterResponse>('/api/families/register', {
      familyName: familyName.value,
      parentDisplayName: parent.displayName,
      parentPin: parent.pin,
    })
    auth.applyRegisterResponse(data)
    recoveryCode.value = data.recoveryCode
    step.value = 3
  } catch {
    error.value = 'No s\'ha pogut crear la família. Torna-ho a provar.'
  } finally {
    loading.value = false
  }
}

async function addChild() {
  error.value = null
  if (!child.displayName.trim() || !child.birthDate) {
    error.value = 'Cal un nom i una data de naixement.'
    return
  }
  if (!/^\d{4}$/.test(child.pin)) {
    error.value = 'El PIN ha de tenir exactament 4 dígits.'
    return
  }
  if (child.pin !== child.pinConfirm) {
    error.value = 'Els PIN no coincideixen.'
    return
  }
  savingChild.value = true
  try {
    await api.post('/api/children', {
      displayName: child.displayName,
      birthDate: child.birthDate,
      avatar: null,
      colorTheme: child.colorTheme,
      pin: child.pin,
    })
    addedChildren.value.push(child.displayName)
    child.displayName = ''
    child.birthDate = ''
    child.pin = ''
    child.pinConfirm = ''
  } catch {
    error.value = 'No s\'ha pogut afegir el fill. Torna-ho a provar.'
  } finally {
    savingChild.value = false
  }
}

function goToRecovery() {
  step.value = 4
}

async function copyCode() {
  await navigator.clipboard.writeText(recoveryCode.value)
  codeCopied.value = true
}

async function finish() {
  await router.push({ name: 'parent-resum' })
}
</script>

<template>
  <div class="register">
    <div class="register__brand">
      <img src="@/assets/mapaka-logo.svg" alt="" width="56" height="56" />
      <h1>Mapaka</h1>
    </div>

    <BaseCard class="register__card">
      <p class="register__step">Pas {{ step }} de 4</p>

      <form v-if="step === 1" class="register__form" @submit.prevent="nextFromFamily">
        <label>
          Nom de la família
          <input v-model="familyName" type="text" placeholder="Sande-Lima" required autofocus />
        </label>
        <BaseButton type="submit" variant="primary">Següent</BaseButton>
      </form>

      <form v-else-if="step === 2" class="register__form" @submit.prevent="registerParent">
        <label>
          El teu nom
          <input v-model="parent.displayName" type="text" required autofocus />
        </label>
        <label>
          PIN de 4 dígits
          <input v-model="parent.pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <label>
          Confirma el PIN
          <input v-model="parent.pinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <p v-if="error" class="register__error">{{ error }}</p>
        <BaseButton type="submit" variant="primary" :disabled="loading">
          {{ loading ? 'Creant…' : 'Crear família' }}
        </BaseButton>
      </form>

      <div v-else-if="step === 3" class="register__form">
        <p class="register__prompt">Afegeix els fills (ho pots fer més tard des de Configuració)</p>
        <ul v-if="addedChildren.length" class="register__added">
          <li v-for="name in addedChildren" :key="name">✓ {{ name }}</li>
        </ul>
        <form class="register__child-form" @submit.prevent="addChild">
          <label>
            Nom del fill
            <input v-model="child.displayName" type="text" />
          </label>
          <label>
            Data de naixement
            <input v-model="child.birthDate" type="date" />
          </label>
          <label>
            PIN de 4 dígits
            <input v-model="child.pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" />
          </label>
          <label>
            Confirma el PIN
            <input v-model="child.pinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" />
          </label>
          <div class="register__colors">
            <button
              v-for="color in COLORS"
              :key="color"
              type="button"
              class="register__color"
              :class="{ active: child.colorTheme === color }"
              :style="{ background: color }"
              @click="child.colorTheme = color"
            />
          </div>
          <p v-if="error" class="register__error">{{ error }}</p>
          <BaseButton type="submit" variant="accent" :disabled="savingChild">
            {{ savingChild ? 'Afegint…' : 'Afegir fill' }}
          </BaseButton>
        </form>
        <BaseButton type="button" variant="primary" @click="goToRecovery">Continuar</BaseButton>
      </div>

      <div v-else class="register__form">
        <p class="register__prompt">Codi de recuperació</p>
        <p class="register__warning">
          Apunta'l en un lloc segur — no es tornarà a mostrar mai més. Si un dia oblides el PIN i ets l'únic
          adult de la família, el necessitaràs per recuperar l'accés.
        </p>
        <div class="register__code">{{ recoveryCode }}</div>
        <BaseButton type="button" variant="accent" @click="copyCode">
          {{ codeCopied ? 'Copiat ✓' : 'Copiar al porta-retalls' }}
        </BaseButton>
        <label class="register__checkbox">
          <input v-model="savedConfirmed" type="checkbox" />
          L'he desat
        </label>
        <BaseButton type="button" variant="primary" :disabled="!savedConfirmed" @click="finish">
          Anar a Mapaka
        </BaseButton>
      </div>
    </BaseCard>
  </div>
</template>

<style scoped>
.register {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  padding: 2rem 1.5rem;
}

.register__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.register__brand h1 {
  font-size: 1.75rem;
}

.register__card {
  width: 100%;
  max-width: 380px;
}

.register__step {
  margin: 0 0 1rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.register__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.register__form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 700;
  font-size: 0.9rem;
}

.register__form input {
  font: inherit;
  padding: 0.65rem 0.85rem;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
  background: white;
}

.register__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.register__prompt {
  font-family: var(--font-heading);
  font-weight: 700;
  margin: 0;
}

.register__added {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 0.85rem;
  color: var(--success);
  font-weight: 700;
}

.register__child-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.9rem;
  background: color-mix(in srgb, var(--primary) 5%, transparent);
  border-radius: 14px;
}

.register__colors {
  display: flex;
  gap: 0.5rem;
}

.register__color {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
}

.register__color.active {
  border-color: var(--text);
}

.register__warning {
  font-size: 0.82rem;
  color: var(--muted);
  line-height: 1.5;
  margin: 0;
}

.register__code {
  font-family: var(--font-body);
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  font-size: 1.4rem;
  letter-spacing: 0.15em;
  text-align: center;
  padding: 0.85rem;
  border-radius: 12px;
  background: color-mix(in srgb, var(--accent) 15%, transparent);
}

.register__checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 700;
}
</style>
