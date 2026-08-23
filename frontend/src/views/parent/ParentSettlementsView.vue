<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { formatDate } from '@/utils/date'
import type { AppLocale } from '@/i18n'
import type { MonthlySettlementResponse } from '@/types/parent'

const { t, locale } = useI18n()
const settlements = ref<MonthlySettlementResponse[]>([])
const loading = ref(true)
const expandedId = ref<string | null>(null)
const detail = ref<MonthlySettlementResponse | null>(null)

async function load() {
  const { data } = await api.get<MonthlySettlementResponse[]>('/api/settlements')
  settlements.value = data
  loading.value = false
}

async function toggle(settlement: MonthlySettlementResponse) {
  if (expandedId.value === settlement.id) {
    expandedId.value = null
    detail.value = null
    return
  }
  expandedId.value = settlement.id
  const { data } = await api.get<MonthlySettlementResponse>(`/api/settlements/${settlement.id}`)
  detail.value = data
}

function periodLabel(settlement: MonthlySettlementResponse) {
  return formatDate(new Date(settlement.year, settlement.month - 1, 1), locale.value as AppLocale, {
    month: 'long',
    year: 'numeric',
  })
}

onMounted(load)
</script>

<template>
  <div class="settlements">
    <h1>{{ t('resum.settlementsTitle') }}</h1>
    <p class="settlements__sub">{{ t('resum.settlementsSubtitle') }}</p>

    <p v-if="!loading && settlements.length === 0" class="settlements__empty">{{ t('resum.settlementsEmpty') }}</p>

    <BaseCard v-for="settlement in settlements" :key="settlement.id" class="settlement-card" @click="toggle(settlement)">
      <div class="settlement-card__row">
        <div>
          <div class="settlement-card__name">{{ settlement.childDisplayName }}</div>
          <div class="settlement-card__period">{{ periodLabel(settlement) }}</div>
        </div>
        <AmountDisplay :value="settlement.payableAmount" unit="€" class="settlement-card__amount" />
      </div>

      <div v-if="expandedId === settlement.id && detail" class="settlement-detail">
        <div class="settlement-detail__row"><span>{{ t('resum.baseAllowance') }}</span><AmountDisplay :value="detail.baseAllowance" unit="€" /></div>
        <div class="settlement-detail__row"><span>{{ t('resum.extraEarnings') }}</span><AmountDisplay :value="detail.extraEarnings" unit="€" /></div>
        <div class="settlement-detail__row"><span>{{ t('resum.bonuses') }}</span><AmountDisplay :value="detail.bonuses" unit="€" /></div>
        <div class="settlement-detail__row"><span>{{ t('resum.penalties') }}</span>-<AmountDisplay :value="detail.penalties" unit="€" /></div>
        <div class="settlement-detail__row"><span>{{ t('resum.savingsPortion') }}</span><AmountDisplay :value="detail.savings" unit="€" /></div>
      </div>
    </BaseCard>
  </div>
</template>

<style scoped>
.settlements {
  max-width: 640px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.settlements__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1.25rem;
}

.settlements__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.settlement-card {
  margin-bottom: 0.6rem;
  cursor: pointer;
}

.settlement-card__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.settlement-card__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
}

.settlement-card__period {
  font-size: 0.76rem;
  color: var(--muted);
}

.settlement-card__amount {
  font-size: 1.1rem;
  color: var(--primary);
}

.settlement-detail {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px dashed color-mix(in srgb, var(--primary) 15%, transparent);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.settlement-detail__row {
  display: flex;
  justify-content: space-between;
  font-size: 0.82rem;
  color: var(--muted);
}
</style>
