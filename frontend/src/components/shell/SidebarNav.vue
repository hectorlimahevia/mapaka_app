<script setup lang="ts">
import { useRoute } from 'vue-router'
import NavIcon from './NavIcon.vue'
import BadgeCounter from '@/components/base/BadgeCounter.vue'

type IconName = 'home' | 'tasks' | 'target' | 'device' | 'family' | 'check' | 'child' | 'settings'

defineProps<{
  items: { name: string; label: string; icon: IconName; badge?: number }[]
}>()

const route = useRoute()
</script>

<template>
  <nav class="sidebar">
    <div class="sidebar__brand">
      <img src="@/assets/mapaka-logo.svg" alt="" width="26" height="26" />
      <span>Mapaka</span>
    </div>

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
</style>
