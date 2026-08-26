<script setup lang="ts">
const props = withDefaults(defineProps<{ count: number; color?: string | null }>(), { count: 0, color: null })
</script>

<template>
  <span
    v-if="props.count > 0"
    class="badge-counter"
    :class="{ 'badge-counter--custom': props.color }"
    :style="props.color ? { background: props.color } : undefined"
  >
    {{ props.count > 9 ? '9+' : props.count }}
  </span>
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

.badge-counter--custom {
  border: 2px solid white;
}
</style>
