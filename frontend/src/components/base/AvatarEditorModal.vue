<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import { AVATAR_ICONS } from '@/utils/avatarIcons'
import { CHILD_COLORS } from '@/utils/childColors'

const { t } = useI18n()
const auth = useAuthStore()
const emit = defineEmits<{ close: [] }>()

const tab = ref<'color' | 'icon'>('color')
const selectedColor = ref(auth.avatarColor ?? CHILD_COLORS[0]!)
const selectedIcon = ref<string | null>(auth.avatarIcon)
const saving = ref(false)

async function save() {
  if (!auth.childId || saving.value) return
  saving.value = true
  try {
    await api.patch(`/api/children/${auth.childId}/avatar`, { color: selectedColor.value, icon: selectedIcon.value })
    auth.setAvatar(selectedColor.value, selectedIcon.value)
    emit('close')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="avatar-modal-overlay" @click.self="emit('close')">
    <div class="avatar-modal">
      <div class="avatar-modal__preview">
        <ChildAvatar :color="selectedColor" :icon="selectedIcon" :name="auth.displayName ?? ''" />
      </div>
      <b class="avatar-modal__title">{{ t('avatar.title') }}</b>
      <p class="avatar-modal__subtitle">{{ t('avatar.subtitle') }}</p>

      <div class="avatar-modal__tabs">
        <button type="button" class="avatar-modal__tab" :class="{ active: tab === 'color' }" @click="tab = 'color'">{{ t('avatar.colorTab') }}</button>
        <button type="button" class="avatar-modal__tab" :class="{ active: tab === 'icon' }" @click="tab = 'icon'">{{ t('avatar.iconTab') }}</button>
      </div>

      <div v-if="tab === 'color'" class="avatar-modal__swatches">
        <button
          v-for="color in CHILD_COLORS"
          :key="color"
          type="button"
          class="avatar-modal__swatch"
          :class="{ active: selectedColor === color }"
          :style="{ background: color }"
          @click="selectedColor = color"
        />
      </div>
      <div v-else class="avatar-modal__icons">
        <button
          type="button"
          class="avatar-modal__icon-cell"
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
          class="avatar-modal__icon-cell"
          :class="{ active: selectedIcon === icon }"
          @click="selectedIcon = icon"
        />
      </div>
      <p v-if="tab === 'color'" class="avatar-modal__hint">{{ t('avatar.colorHint') }}</p>

      <div class="avatar-modal__actions">
        <BaseButton type="button" variant="ghost" :disabled="saving" @click="emit('close')">{{ t('common.cancel') }}</BaseButton>
        <BaseButton type="button" variant="primary" :disabled="saving" @click="save">{{ saving ? t('common.saving') : t('common.save') }}</BaseButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.avatar-modal-overlay {
  position: fixed;
  inset: 0;
  background: color-mix(in srgb, var(--text) 55%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.avatar-modal {
  background: white;
  border-radius: 20px;
  padding: 1.75rem 1.5rem;
  width: 100%;
  max-width: 360px;
  text-align: center;
}

.avatar-modal__preview {
  display: flex;
  justify-content: center;
  margin-bottom: 0.75rem;
}

.avatar-modal__preview :deep(.child-avatar) {
  width: 64px;
  height: 64px;
  font-size: 1.6rem;
}

.avatar-modal__preview :deep(.child-avatar svg) {
  width: 32px;
  height: 32px;
}

.avatar-modal__title {
  font-family: var(--font-heading);
  font-size: 1rem;
}

.avatar-modal__subtitle {
  font-size: 0.78rem;
  color: var(--muted);
  margin: 0.25rem 0 1rem;
}

.avatar-modal__tabs {
  display: inline-flex;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.2rem;
  margin-bottom: 1rem;
}

.avatar-modal__tab {
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

.avatar-modal__tab.active {
  background: var(--primary);
  color: white;
}

.avatar-modal__swatches,
.avatar-modal__icons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  justify-items: center;
  margin-bottom: 0.5rem;
}

.avatar-modal__swatch {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 3px solid transparent;
  cursor: pointer;
}

.avatar-modal__swatch.active {
  border-color: var(--text);
}

.avatar-modal__icon-cell {
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

.avatar-modal__icon-cell.active {
  border-color: var(--text);
}

.avatar-modal__hint {
  font-size: 0.7rem;
  color: var(--muted);
  margin: 0.5rem 0 0;
}

.avatar-modal__actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1.25rem;
}

.avatar-modal__actions :deep(.base-button) {
  flex: 1;
}
</style>
