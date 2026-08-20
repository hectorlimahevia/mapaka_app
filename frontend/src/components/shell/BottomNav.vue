<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavIcon from './NavIcon.vue'
import BadgeCounter from '@/components/base/BadgeCounter.vue'

type IconName = 'home' | 'tasks' | 'target' | 'device' | 'family' | 'check' | 'child' | 'settings'

const props = defineProps<{
  items: { name: string; label: string; icon: IconName; badge?: number }[]
}>()

const route = useRoute()
const activeIndex = computed(() => {
  const index = props.items.findIndex((item) => item.name === route.name)
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
      <span class="bottom-nav__icon">
        <NavIcon :name="item.icon" />
        <BadgeCounter v-if="item.badge" :count="item.badge" class="bottom-nav__badge" />
      </span>
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
  background: white;
  border-top: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  padding: 0.6rem 0.4rem calc(0.6rem + env(safe-area-inset-bottom));
  z-index: 20;
}

.bottom-nav__indicator {
  position: absolute;
  top: 0.35rem;
  left: 0.25rem;
  height: calc(100% - 0.7rem);
  width: calc(100% / var(--count) - 0.5rem);
  border-radius: 16px;
  background: color-mix(in srgb, var(--primary) 12%, transparent);
  transform: translateX(calc(var(--active) * 100%));
  transition: transform 0.32s cubic-bezier(0.3, 0.8, 0.3, 1);
}

.bottom-nav__item {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 0.35rem 0;
  color: var(--muted);
  text-decoration: none;
  font-family: var(--font-heading);
  font-size: 0.68rem;
  font-weight: 700;
  transition: color 0.2s ease;
}

.bottom-nav__item--active {
  color: var(--primary);
}

.bottom-nav__icon {
  position: relative;
}

.bottom-nav__badge {
  position: absolute;
  top: -0.3rem;
  right: -0.5rem;
}
</style>
