<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '@/utils/money'

const props = withDefaults(
  defineProps<{
    value: number
    unit?: string
    decimals?: number
  }>(),
  { unit: '', decimals: 2 },
)

const formatted = computed(() => formatMoney(props.value, props.decimals))
</script>

<template>
  <span class="amount-display">{{ formatted
    }}<span v-if="unit" class="amount-display__unit">{{ unit }}</span></span>
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
