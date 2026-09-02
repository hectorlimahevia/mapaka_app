<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavIcon from './NavIcon.vue'
import BadgeCounter from '@/components/base/BadgeCounter.vue'

type IconName = 'home' | 'tasks' | 'target' | 'device' | 'family' | 'check' | 'child' | 'settings'

const props = defineProps<{
  items: { name: string; label: string; icon: IconName; badge?: number; badgeColor?: string | null }[]
}>()

const route = useRoute()
const activeIndex = computed(() => {
  const index = props.items.findIndex((item) => item.name === route.name)
  return index === -1 ? 0 : index
})
</script>

<template>
  <nav class="bottom-nav" :style="{ '--count': items.length, '--active': activeIndex }">
    <span class="bottom-nav__indicator"><span class="bottom-nav__indicator-pill" /></span>
    <RouterLink
      v-for="item in items"
      :key="item.name"
      :to="{ name: item.name }"
      class="bottom-nav__item"
      :class="{ 'bottom-nav__item--active': route.name === item.name }"
    >
      <span class="bottom-nav__icon">
        <NavIcon :name="item.icon" />
        <BadgeCounter v-if="item.badge" :count="item.badge" :color="item.badgeColor" class="bottom-nav__badge" />
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

/* Sized to exactly one column so `translateX(N * 100%)` — a percentage of this
   element's OWN box, per the CSS transform spec — lands on column N exactly. The
   previous version shrank this box by 0.5rem for the visual inset, so each 100%
   step fell short of a real column width and the miss compounded with every tab;
   the inset now lives on the inner pill instead, which doesn't affect the math. */
.bottom-nav__indicator {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: calc(100% / var(--count));
  transform: translateX(calc(var(--active) * 100%));
  transition: transform 0.32s cubic-bezier(0.3, 0.8, 0.3, 1);
  pointer-events: none;
}

.bottom-nav__indicator-pill {
  position: absolute;
  inset: 0.35rem 0.25rem;
  border-radius: 16px;
  background: color-mix(in srgb, var(--primary) 12%, transparent);
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

.bottom-nav__item--active .bottom-nav__icon {
  animation: bottom-nav-bounce 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes bottom-nav-bounce {
  0% {
    transform: scale(1);
  }
  40% {
    transform: scale(1.18);
  }
  100% {
    transform: scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .bottom-nav__item--active .bottom-nav__icon {
    animation: none;
  }
}

.bottom-nav__badge {
  position: absolute;
  top: -0.3rem;
  right: -0.5rem;
}
</style>
