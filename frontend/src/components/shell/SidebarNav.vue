<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import NavIcon from './NavIcon.vue'
import BadgeCounter from '@/components/base/BadgeCounter.vue'
import { useAuthStore } from '@/stores/auth'

type IconName = 'home' | 'tasks' | 'target' | 'device' | 'family' | 'check' | 'child' | 'settings'

defineProps<{
  items: { name: string; label: string; icon: IconName; badge?: number }[]
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

async function logout() {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <nav class="sidebar">
    <RouterLink :to="{ name: 'parent-resum' }" class="sidebar__brand">
      <img src="@/assets/mapaka-logo.svg" alt="" width="26" height="26" />
      <span>Mapaka</span>
    </RouterLink>

    <div class="sidebar__items">
      <RouterLink
        v-for="item in items"
        :key="item.name"
        :to="{ name: item.name }"
        class="sidebar__item"
        :class="{ 'sidebar__item--active': route.name === item.name }"
      >
        <NavIcon :name="item.icon" />
        <span>{{ item.label }}</span>
        <BadgeCounter v-if="item.badge" :count="item.badge" class="sidebar__badge" />
      </RouterLink>
    </div>

    <button type="button" class="sidebar__logout" @click="logout">
      <NavIcon name="logout" />
      <span>{{ t('nav.logout') }}</span>
    </button>
  </nav>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 240px;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem 1rem;
  background: white;
  border-right: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  z-index: 20;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-family: var(--font-heading);
  font-weight: 800;
  font-size: 1.1rem;
  color: var(--primary);
  padding: 0 0.5rem;
  text-decoration: none;
}

.sidebar__items {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.sidebar__item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.7rem 0.75rem;
  border-radius: 12px;
  color: #5b5580;
  text-decoration: none;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.88rem;
  transition:
    background 0.2s ease,
    color 0.2s ease;
}

.sidebar__item--active {
  background: color-mix(in srgb, var(--primary) 9%, transparent);
  color: var(--primary);
}

.sidebar__badge {
  margin-left: auto;
}

.sidebar__logout {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-top: auto;
  border: none;
  background: none;
  padding: 0.7rem 0.75rem;
  border-radius: 12px;
  color: var(--muted);
  cursor: pointer;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.88rem;
  transition: color 0.2s ease;
}

.sidebar__logout:hover {
  color: var(--error);
}
</style>
