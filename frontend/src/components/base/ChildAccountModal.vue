<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils/apiError'
import BaseButton from '@/components/base/BaseButton.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import { AVATAR_ICONS } from '@/utils/avatarIcons'
import { CHILD_COLORS } from '@/utils/childColors'

const { t } = useI18n()
const auth = useAuthStore()
const emit = defineEmits<{ close: [] }>()

const section = ref<'avatar' | 'pin'>('avatar')
const avatarTab = ref<'color' | 'icon'>('color')
const selectedColor = ref(auth.avatarColor ?? CHILD_COLORS[0]!)
const selectedIcon = ref<string | null>(auth.avatarIcon)
const savingAvatar = ref(false)

async function saveAvatar() {
  if (!auth.childId || savingAvatar.value) return
  savingAvatar.value = true
  try {
    await api.patch(`/api/children/${auth.childId}/avatar`, { color: selectedColor.value, icon: selectedIcon.value })
    auth.setAvatar(selectedColor.value, selectedIcon.value)
    emit('close')
  } finally {
    savingAvatar.value = false
  }
}

const oldPin = ref('')
const newPin = ref('')
const newPinConfirm = ref('')
const pinError = ref<string | null>(null)
const pinSuccess = ref(false)
const savingPin = ref(false)

async function savePin() {
  pinError.value = null
  if (!/^\d{4}$/.test(oldPin.value) || !/^\d{4}$/.test(newPin.value)) {
    pinError.value = t('common.pinInvalid')
    return
  }
  if (newPin.value !== newPinConfirm.value) {
    pinError.value = t('common.pinMismatch')
    return
  }
  savingPin.value = true
  try {
    await api.patch('/api/users/me/pin', { oldPin: oldPin.value, newPin: newPin.value })
    pinSuccess.value = true
    oldPin.value = ''
    newPin.value = ''
    newPinConfirm.value = ''
  } catch (err) {
    pinError.value = apiErrorMessage(err)
  } finally {
    savingPin.value = false
  }
}
</script>

<template>
  <div class="account-modal-overlay" @click.self="emit('close')">
    <div class="account-modal">
      <div class="account-modal__preview">
        <ChildAvatar :color="selectedColor" :icon="selectedIcon" :name="auth.displayName ?? ''" />
      </div>

      <div class="account-modal__sections">
        <button type="button" class="account-modal__section" :class="{ active: section === 'avatar' }" @click="section = 'avatar'">{{ t('avatar.title') }}</button>
        <button type="button" class="account-modal__section" :class="{ active: section === 'pin' }" @click="section = 'pin'">{{ t('avatar.pinSection') }}</button>
      </div>

      <template v-if="section === 'avatar'">
        <p class="account-modal__subtitle">{{ t('avatar.subtitle') }}</p>
        <div class="account-modal__tabs">
          <button type="button" class="account-modal__tab" :class="{ active: avatarTab === 'color' }" @click="avatarTab = 'color'">{{ t('avatar.colorTab') }}</button>
          <button type="button" class="account-modal__tab" :class="{ active: avatarTab === 'icon' }" @click="avatarTab = 'icon'">{{ t('avatar.iconTab') }}</button>
        </div>

        <div v-if="avatarTab === 'color'" class="account-modal__swatches">
          <button
            v-for="color in CHILD_COLORS"
            :key="color"
            type="button"
            class="account-modal__swatch"
            :class="{ active: selectedColor === color }"
            :style="{ background: color }"
            @click="selectedColor = color"
          />
        </div>
        <div v-else class="account-modal__icons">
          <button
            type="button"
            class="account-modal__icon-cell"
            :class="{ active: selectedIcon === null }"
            :style="{ background: selectedColor }"
            @click="selectedIcon = null"
          >
            {{ (auth.displayName ?? '').charAt(0).toUpperCase() }}
          </button>
          <ChildAvatar
            v-for="icon in AVATAR_ICONS"
            :key="icon"
            :color="selectedColor"
            :icon="icon"
            :name="auth.displayName ?? ''"
            class="account-modal__icon-cell"
            :class="{ active: selectedIcon === icon }"
            @click="selectedIcon = icon"
          />
        </div>
        <p v-if="avatarTab === 'color'" class="account-modal__hint">{{ t('avatar.colorHint') }}</p>

        <div class="account-modal__actions">
          <BaseButton type="button" variant="ghost" :disabled="savingAvatar" @click="emit('close')">{{ t('common.cancel') }}</BaseButton>
          <BaseButton type="button" variant="primary" :disabled="savingAvatar" @click="saveAvatar">{{ savingAvatar ? t('common.saving') : t('common.save') }}</BaseButton>
        </div>
      </template>

      <template v-else>
        <p v-if="pinSuccess" class="account-modal__success">{{ t('avatar.pinUpdated') }}</p>
        <form v-else class="account-modal__pin-form" @submit.prevent="savePin">
          <label>
            {{ t('avatar.pinCurrentLabel') }}
            <input v-model="oldPin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
          </label>
          <label>
            {{ t('avatar.pinNewLabel') }}
            <input v-model="newPin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
          </label>
          <label>
            {{ t('common.pinConfirmLabel') }}
            <input v-model="newPinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
          </label>
          <p v-if="pinError" class="account-modal__error">{{ pinError }}</p>
          <div class="account-modal__actions">
            <BaseButton type="button" variant="ghost" :disabled="savingPin" @click="emit('close')">{{ t('common.cancel') }}</BaseButton>
            <BaseButton type="submit" variant="primary" :disabled="savingPin">{{ savingPin ? t('common.saving') : t('common.save') }}</BaseButton>
          </div>
        </form>
        <div v-if="pinSuccess" class="account-modal__actions">
          <BaseButton type="button" variant="primary" @click="emit('close')">{{ t('common.close') }}</BaseButton>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.account-modal-overlay {
  position: fixed;
  inset: 0;
  background: color-mix(in srgb, var(--text) 55%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.account-modal {
  background: white;
  border-radius: 20px;
  padding: 1.75rem 1.5rem;
  width: 100%;
  max-width: 360px;
  text-align: center;
}

.account-modal__preview {
  display: flex;
  justify-content: center;
  margin-bottom: 0.75rem;
}

.account-modal__preview :deep(.child-avatar) {
  width: 56px;
  height: 56px;
  font-size: 1.4rem;
}

.account-modal__preview :deep(.child-avatar svg) {
  width: 28px;
  height: 28px;
}

.account-modal__sections {
  display: flex;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.2rem;
  margin-bottom: 1rem;
}

.account-modal__section {
  flex: 1;
  border: none;
  background: none;
  padding: 0.45rem 0.5rem;
  border-radius: 999px;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.72rem;
  color: var(--muted);
  cursor: pointer;
}

.account-modal__section.active {
  background: var(--primary);
  color: white;
}

.account-modal__subtitle {
  font-size: 0.78rem;
  color: var(--muted);
  margin: 0 0 1rem;
}

.account-modal__tabs {
  display: inline-flex;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.2rem;
  margin-bottom: 1rem;
}

.account-modal__tab {
  border: none;
  background: none;
  padding: 0.4rem 1.1rem;
  border-radius: 999px;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.8rem;
  color: var(--muted);
  cursor: pointer;
}

.account-modal__tab.active {
  background: var(--primary);
  color: white;
}

.account-modal__swatches,
.account-modal__icons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  justify-items: center;
  margin-bottom: 0.5rem;
}

.account-modal__swatch {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 3px solid transparent;
  cursor: pointer;
}

.account-modal__swatch.active {
  border-color: var(--text);
}

.account-modal__icon-cell {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 3px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
}

.account-modal__icon-cell.active {
  border-color: var(--text);
}

.account-modal__hint {
  font-size: 0.7rem;
  color: var(--muted);
  margin: 0.5rem 0 0;
}

.account-modal__pin-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  text-align: left;
}

.account-modal__pin-form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.account-modal__pin-form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.account-modal__error {
  color: var(--error);
  font-size: 0.82rem;
  font-weight: 700;
  margin: 0;
}

.account-modal__success {
  color: var(--success);
  font-weight: 700;
  font-size: 0.9rem;
  margin: 1rem 0;
}

.account-modal__actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1.25rem;
}

.account-modal__actions :deep(.base-button) {
  flex: 1;
}
</style>
