<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavIcon from './NavIcon.vue'

type IconName = 'home' | 'tasks' | 'target' | 'device' | 'family' | 'check' | 'child' | 'settings'

const props = defineProps<{
  items: { name: string; label: string; icon: IconName }[]
}>()

const route = useRoute()
const activeIndex = computed(() => {
  const index = props.items.findIndex((item) => item.name === route.name)
  return index === -1 ? 0 : index
})
</script>

<template>
  <nav class="sidebar" :style="{ '--count': items.length, '--active': activeIndex }">
    <div class="sidebar__brand">
      <img src="@/assets/mapaka-logo.svg" alt="" width="32" height="32" />
      <span>Mapaka</span>
    </div>

    <div class="sidebar__items">
      <span class="sidebar__indicator" />
      <RouterLink
        v-for="item in items"
        :key="item.name"
        :to="{ name: item.name }"
        class="sidebar__item"
        :class="{ 'sidebar__item--active': route.name === item.name }"
      >
        <NavIcon :name="item.icon" />
        <span>{{ item.label }}</span>
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
  gap: 2rem;
  padding: 1.5rem 1rem;
  background: color-mix(in srgb, var(--bg) 85%, white);
  border-right: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  z-index: 20;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--text);
  padding: 0 0.5rem;
}

.sidebar__items {
  position: relative;
  display: flex;
  flex-direction: column;
}

.sidebar__indicator {
  /* Alçada fixa (no percentual): l'indicador viu dins d'un flex sense alçada pròpia
     definida, i un % d'alçada contra un contenidor 'auto' col·lapsa a 0. */
  --item-height: 3.25rem;
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: var(--item-height);
  border-radius: 999px;
  background: var(--primary);
  transform: translateY(calc(var(--active) * var(--item-height)));
  transition: transform 0.35s cubic-bezier(0.65, 0, 0.35, 1);
}

.sidebar__item {
  --item-height: 3.25rem;
  display: flex;
  align-items: center;
  height: var(--item-height);
  gap: 0.75rem;
  padding: 0 0.75rem 0 1.25rem;
  border-radius: 14px;
  color: var(--muted);
  text-decoration: none;
  font-weight: 700;
  font-size: 0.95rem;
  transition:
    color 0.2s ease,
    background 0.2s ease;
}

.sidebar__item:hover {
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.sidebar__item--active {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 12%, transparent);
}
</style>
