<script setup lang="ts">
const props = withDefaults(defineProps<{ count: number }>(), { count: 0 })
</script>

<template>
  <span v-if="props.count > 0" class="badge-counter">{{ props.count > 9 ? '9+' : props.count }}</span>
</template>

<style scoped>
.badge-counter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  background: var(--secondary);
  color: white;
  font-family: var(--font-body);
  font-size: 0.75rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
</style>
