<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import NavIcon from './NavIcon.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import ChildAccountModal from '@/components/base/ChildAccountModal.vue'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()

const showAccountModal = ref(false)

async function goHome() {
  await router.push({ name: auth.role === 'PARENT' ? 'parent-resum' : 'child-inici' })
}

async function logout() {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <header class="top-bar">
    <button type="button" class="top-bar__brand" @click="goHome">
      <img src="@/assets/mapaka-logo.svg" alt="" width="22" height="22" />
      <span>Mapaka</span>
    </button>
    <div class="top-bar__actions">
      <button
        v-if="auth.role === 'CHILD'"
        type="button"
        class="top-bar__avatar-btn"
        :aria-label="t('avatar.title')"
        @click="showAccountModal = true"
      >
        <ChildAvatar :color="auth.avatarColor" :icon="auth.avatarIcon" :name="auth.displayName ?? ''" size="small" />
        <span class="top-bar__pencil-badge">
          <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><path d="M12 20h9M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z" /></svg>
        </span>
      </button>
      <button type="button" class="top-bar__logout" :aria-label="t('nav.logout')" @click="logout">
        <NavIcon name="logout" />
      </button>
    </div>
  </header>

  <ChildAccountModal v-if="showAccountModal" @close="showAccountModal = false" />
</template>

<style scoped>
.top-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(0.6rem + env(safe-area-inset-top)) 1rem 0.6rem;
  background: white;
  border-bottom: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  z-index: 20;
}

.top-bar__brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  font-family: var(--font-heading);
  font-weight: 800;
  font-size: 0.95rem;
  color: var(--primary);
}

.top-bar__actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.top-bar__avatar-btn {
  position: relative;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  flex-shrink: 0;
}

.top-bar__pencil-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--primary);
  border: 2px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.top-bar__pencil-badge svg {
  width: 8px;
  height: 8px;
}

.top-bar__logout {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  padding: 0.35rem;
  cursor: pointer;
  color: var(--muted);
  transition: color 0.2s ease;
}

.top-bar__logout:hover {
  color: var(--error);
}
</style>
