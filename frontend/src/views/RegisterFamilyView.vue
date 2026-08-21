<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import MapakaLogo from '@/components/base/MapakaLogo.vue'
import { apiErrorMessage } from '@/utils/apiError'
import { i18n } from '@/i18n'
import type { FamilyRegisterResponse } from '@/types/auth'

const { t } = useI18n()
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
    error.value = t('common.pinInvalid')
    return
  }
  if (parent.pin !== parent.pinConfirm) {
    error.value = t('common.pinMismatch')
    return
  }
  loading.value = true
  try {
    const { data } = await api.post<FamilyRegisterResponse>('/api/families/register', {
      familyName: familyName.value,
      parentDisplayName: parent.displayName,
      parentPin: parent.pin,
      locale: i18n.global.locale.value,
    })
    await auth.applyRegisterResponse(data)
    recoveryCode.value = data.recoveryCode
    step.value = 3
  } catch (err) {
    error.value = apiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function addChild() {
  error.value = null
  if (!child.displayName.trim() || !child.birthDate) {
    error.value = t('registre.missingChildFields')
    return
  }
  if (!/^\d{4}$/.test(child.pin)) {
    error.value = t('common.pinInvalid')
    return
  }
  if (child.pin !== child.pinConfirm) {
    error.value = t('common.pinMismatch')
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
      locale: i18n.global.locale.value,
    })
    addedChildren.value.push(child.displayName)
    child.displayName = ''
    child.birthDate = ''
    child.pin = ''
    child.pinConfirm = ''
  } catch (err) {
    error.value = apiErrorMessage(err)
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
    <MapakaLogo class="register__brand" />

    <BaseCard class="register__card">
      <p class="register__step">{{ t('registre.step', { step }) }}</p>

      <form v-if="step === 1" class="register__form" @submit.prevent="nextFromFamily">
        <label>
          {{ t('registre.familyNameLabel') }}
          <input v-model="familyName" type="text" :placeholder="t('login.familyNamePlaceholder')" required autofocus />
        </label>
        <BaseButton type="submit" variant="primary">{{ t('registre.next') }}</BaseButton>
      </form>

      <form v-else-if="step === 2" class="register__form" @submit.prevent="registerParent">
        <label>
          {{ t('registre.yourNameLabel') }}
          <input v-model="parent.displayName" type="text" required autofocus />
        </label>
        <label>
          {{ t('common.pinLabel') }}
          <input v-model="parent.pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <label>
          {{ t('common.pinConfirmLabel') }}
          <input v-model="parent.pinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <p v-if="error" class="register__error">{{ error }}</p>
        <BaseButton type="submit" variant="primary" :disabled="loading">
          {{ loading ? t('registre.creating') : t('registre.createFamily') }}
        </BaseButton>
      </form>

      <div v-else-if="step === 3" class="register__form">
        <p class="register__prompt">{{ t('registre.childrenPrompt') }}</p>
        <ul v-if="addedChildren.length" class="register__added">
          <li v-for="name in addedChildren" :key="name">✓ {{ name }}</li>
        </ul>
        <form class="register__child-form" @submit.prevent="addChild">
          <label>
            {{ t('registre.childNameLabel') }}
            <input v-model="child.displayName" type="text" />
          </label>
          <label>
            {{ t('registre.birthDateLabel') }}
            <input v-model="child.birthDate" type="date" />
          </label>
          <label>
            {{ t('common.pinLabel') }}
            <input v-model="child.pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" />
          </label>
          <label>
            {{ t('common.pinConfirmLabel') }}
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
            {{ savingChild ? t('registre.adding') : t('registre.addChild') }}
          </BaseButton>
        </form>
        <BaseButton type="button" variant="primary" @click="goToRecovery">{{ t('registre.continue') }}</BaseButton>
      </div>

      <div v-else class="register__form">
        <p class="register__prompt">{{ t('registre.recoveryCodeTitle') }}</p>
        <p class="register__warning">{{ t('registre.recoveryCodeWarning') }}</p>
        <div class="register__code">{{ recoveryCode }}</div>
        <BaseButton type="button" variant="accent" @click="copyCode">
          {{ codeCopied ? t('registre.copied') : t('registre.copyCode') }}
        </BaseButton>
        <label class="register__checkbox">
          <input v-model="savedConfirmed" type="checkbox" />
          {{ t('registre.savedConfirm') }}
        </label>
        <BaseButton type="button" variant="primary" :disabled="!savedConfirmed" @click="finish">
          {{ t('registre.goToApp') }}
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
