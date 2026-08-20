<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavIcon from './NavIcon.vue'

const items = [
  { name: 'child-inici', label: 'Inici', icon: 'home' as const },
  { name: 'child-tasques', label: 'Tasques', icon: 'tasks' as const },
  { name: 'child-objectius', label: 'Objectius', icon: 'target' as const },
  { name: 'child-pantalla', label: 'Pantalla', icon: 'device' as const },
]

const route = useRoute()
const activeIndex = computed(() => {
  const index = items.findIndex((item) => item.name === route.name)
  return index === -1 ? 0 : index
})
</script>

<template>
  <nav class="bottom-nav" :style="{ '--count': items.length, '--active': activeIndex }">
    <span class="bottom-nav__indicator" />
    <RouterLink
      v-for="item in items"
      :key="item.name"
      :to="{ name: item.name }"
      class="bottom-nav__item"
      :class="{ 'bottom-nav__item--active': route.name === item.name }"
    >
      <NavIcon :name="item.icon" />
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: grid;
  grid-template-columns: repeat(var(--count), 1fr);
  background: color-mix(in srgb, var(--bg) 85%, white);
  backdrop-filter: blur(12px);
  border-top: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  padding: 0.5rem 0 calc(0.5rem + env(safe-area-inset-bottom));
  z-index: 20;
}

.bottom-nav__indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: calc(100% / var(--count));
  height: 3px;
  border-radius: 0 0 999px 999px;
  background: var(--primary);
  transform: translateX(calc(var(--active) * 100%));
  transition: transform 0.35s cubic-bezier(0.65, 0, 0.35, 1);
}

.bottom-nav__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 0.25rem 0;
  color: var(--muted);
  text-decoration: none;
  font-size: 0.7rem;
  font-weight: 700;
  transition:
    color 0.2s ease,
    transform 0.2s ease;
}

.bottom-nav__item--active {
  color: var(--primary);
  transform: translateY(-2px);
}
</style>
