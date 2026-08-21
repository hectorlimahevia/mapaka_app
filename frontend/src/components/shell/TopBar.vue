<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NavIcon from './NavIcon.vue'

const router = useRouter()
const auth = useAuthStore()

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
    <button type="button" class="top-bar__logout" aria-label="Tancar sessió" @click="logout">
      <NavIcon name="logout" />
    </button>
  </header>
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
