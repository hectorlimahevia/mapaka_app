<script setup lang="ts"></script>

<template>
  <div class="base-card">
    <slot />
  </div>
</template>

<style scoped>
.base-card {
  background: color-mix(in srgb, var(--bg) 60%, white);
  border-radius: 20px;
  padding: 1.25rem;
  box-shadow: 0 2px 12px -4px color-mix(in srgb, var(--text) 12%, transparent);
}
</style>
