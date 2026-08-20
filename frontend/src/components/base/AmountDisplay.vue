<script setup lang="ts">
withDefaults(
  defineProps<{
    value: number
    unit?: string
  }>(),
  { unit: '' },
)
</script>

<template>
  <span class="amount-display">{{ value }}<span v-if="unit" class="amount-display__unit">{{ unit }}</span></span>
</template>

<style scoped>
.amount-display {
  font-family: var(--font-body);
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.amount-display__unit {
  font-weight: 700;
  margin-left: 0.15em;
}
</style>
