<script setup lang="ts">
defineProps<{
  name: 'home' | 'tasks' | 'target' | 'device' | 'family' | 'check' | 'child' | 'settings'
}>()
</script>

<template>
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <template v-if="name === 'home'">
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5.5 10v9a1 1 0 0 0 1 1H10v-6h4v6h3.5a1 1 0 0 0 1-1v-9" />
    </template>
    <template v-else-if="name === 'tasks'">
      <rect x="4" y="4" width="16" height="16" rx="3" />
      <path d="m8 12 2.5 2.5L16 9" />
    </template>
    <template v-else-if="name === 'target'">
      <circle cx="12" cy="12" r="8" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="12" cy="12" r="0.5" fill="currentColor" />
    </template>
    <template v-else-if="name === 'device'">
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path d="M10.5 18h3" />
    </template>
    <template v-else-if="name === 'family'">
      <circle cx="8" cy="9" r="3" />
      <circle cx="17" cy="10" r="2.5" />
      <path d="M3 20c0-3 2.2-5 5-5s5 2 5 5" />
      <path d="M14 20c0-2.2 1.6-4 4-4s4 1.8 4 4" />
    </template>
    <template v-else-if="name === 'check'">
      <circle cx="12" cy="12" r="8.5" />
      <path d="m8.5 12 2.5 2.5L16 9.5" />
    </template>
    <template v-else-if="name === 'child'">
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20c0-3.5 3-6 7-6s7 2.5 7 6" />
    </template>
    <template v-else-if="name === 'settings'">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 3v2.5M12 18.5V21M4.9 4.9l1.8 1.8M17.3 17.3l1.8 1.8M3 12h2.5M18.5 12H21M4.9 19.1l1.8-1.8M17.3 6.7l1.8-1.8" />
    </template>
  </svg>
</template>

<style scoped>
svg {
  width: 1.4rem;
  height: 1.4rem;
}
</style>
