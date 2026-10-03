<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import type { MovementGroup, MovementLike } from '@/utils/groupMovements'

const props = defineProps<{
  group: MovementGroup<MovementLike>
  title: string
}>()

const { t } = useI18n()

const expanded = ref(false)
const expandable = computed(() => props.group.items.length > 1)
const isCredit = computed(() => props.group.transactionType === 'CREDIT')

function partLabel(item: MovementLike) {
  if (item.walletType === 'GOAL') return item.goalName ?? t('resum.movementGoalFallback')
  return item.walletType === 'SPENDING' ? t('resum.statSpending') : t('resum.statSavings')
}
</script>

<template>
  <div class="movement">
    <component
      :is="expandable ? 'button' : 'div'"
      class="movement__row"
      :class="{ 'movement__row--toggle': expandable }"
      :type="expandable ? 'button' : undefined"
      :aria-expanded="expandable ? expanded : undefined"
      @click="expandable && (expanded = !expanded)"
    >
      <span class="movement__title">
        {{ title }}
        <svg v-if="expandable" class="movement__chevron" :class="{ 'movement__chevron--open': expanded }" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 9l6 6 6-6" />
        </svg>
      </span>
      <span class="movement__amt" :class="isCredit ? 'movement__amt--pos' : 'movement__amt--neg'">
        {{ isCredit ? '+' : '-' }}<AmountDisplay :value="group.total" unit="€" />
      </span>
    </component>

    <ul v-if="expandable && expanded" class="movement__parts">
      <li v-for="item in group.items" :key="item.id" class="movement__part">
        <span>{{ partLabel(item) }}</span>
        <AmountDisplay :value="item.amount" unit="€" />
      </li>
    </ul>
  </div>
</template>

<style scoped>
.movement {
  border-bottom: 1px dashed color-mix(in srgb, var(--primary) 15%, transparent);
}

.movement:last-child {
  border-bottom: none;
}

.movement__row {
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.55rem 0;
  font-size: 0.9rem;
}

.movement__row--toggle {
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.movement__row--toggle:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
  border-radius: 6px;
}

.movement__chevron {
  width: 14px;
  height: 14px;
  margin-left: 0.3rem;
  vertical-align: -2px;
  fill: none;
  stroke: var(--muted);
  stroke-width: 2.4;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 0.15s ease;
}

.movement__chevron--open {
  transform: rotate(180deg);
}

.movement__amt {
  display: inline-flex;
  gap: 0.1rem;
  flex-shrink: 0;
}

.movement__amt--pos {
  color: var(--success);
}

.movement__amt--neg {
  color: var(--error);
}

.movement__parts {
  list-style: none;
  margin: 0;
  padding: 0 0 0.6rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.movement__part {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--muted);
}

@media (prefers-reduced-motion: reduce) {
  .movement__chevron {
    transition: none;
  }
}
</style>
