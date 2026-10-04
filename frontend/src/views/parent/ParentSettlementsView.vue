<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import { formatDate } from '@/utils/date'
import type { AppLocale } from '@/i18n'
import type { MonthlySummary } from '@/types/parent'

const { t, locale } = useI18n()
const summaries = ref<MonthlySummary[]>([])
const loading = ref(true)

async function load() {
  const { data } = await api.get<MonthlySummary[]>('/api/monthly-summaries')
  summaries.value = data
  loading.value = false
}

// El backend ja els entrega ordenats (mes recent primer, fill gran primer dins del mes);
// aquí només s'agrupen per mes mantenint aquest ordre.
const months = computed(() => {
  const groups: { key: string; year: number; month: number; current: boolean; items: MonthlySummary[] }[] = []
  for (const s of summaries.value) {
    const key = `${s.year}-${s.month}`
    let group = groups.find((g) => g.key === key)
    if (!group) {
      group = { key, year: s.year, month: s.month, current: s.current, items: [] }
      groups.push(group)
    }
    group.items.push(s)
  }
  return groups
})

function monthLabel(year: number, month: number) {
  return formatDate(new Date(year, month - 1, 1), locale.value as AppLocale, { month: 'long', year: 'numeric' })
}

onMounted(load)
</script>

<template>
  <div class="settlements">
    <h1>{{ t('resum.settlementsTitle') }}</h1>
    <p class="settlements__sub">{{ t('resum.settlementsSubtitle') }}</p>

    <p v-if="!loading && summaries.length === 0" class="settlements__empty">{{ t('resum.settlementsEmpty') }}</p>

    <section v-for="group in months" :key="group.key" class="month">
      <h2 class="month__title">
        {{ monthLabel(group.year, group.month) }}
        <span v-if="group.current" class="month__chip">{{ t('resum.monthInProgress') }}</span>
      </h2>

      <div
        v-for="s in group.items"
        :key="s.childId"
        class="kid-card"
        :style="{ '--child-color': s.avatarColor ?? 'var(--primary)' }"
      >
        <div class="kid-card__head">
          <ChildAvatar :color="null" :icon="s.avatarIcon" :name="s.childDisplayName" size="small" class="kid-card__avatar" />
          <span class="kid-card__name">{{ s.childDisplayName }}</span>
          <span class="kid-card__total"><AmountDisplay :value="s.total" unit="€" /></span>
        </div>

        <div class="kid-card__body">
          <div class="kid-card__secondary">
            <div class="kid-card__secondary-item">
              <span class="kid-card__secondary-label">{{ t('resum.statSpending') }}</span>
              <span class="kid-card__secondary-value"><AmountDisplay :value="s.spending" unit="€" /></span>
            </div>
            <div class="kid-card__secondary-item">
              <span class="kid-card__secondary-label">{{ t('resum.savingsPortion') }}</span>
              <span class="kid-card__secondary-value"><AmountDisplay :value="s.savings" unit="€" /></span>
            </div>
            <div v-if="s.goals !== 0" class="kid-card__secondary-item">
              <span class="kid-card__secondary-label">{{ t('resum.goalsPortion') }}</span>
              <span class="kid-card__secondary-value"><AmountDisplay :value="s.goals" unit="€" /></span>
            </div>
          </div>

          <div class="detail">
            <div class="detail__row"><span>{{ t('resum.baseAllowance') }}</span><AmountDisplay :value="s.baseAllowance" unit="€" /></div>
            <p v-if="s.allowancePending" class="detail__note">{{ t('resum.monthAllowancePending') }}</p>
            <div v-if="s.extraEarnings !== 0" class="detail__row"><span>{{ t('resum.extraEarnings') }}</span><AmountDisplay :value="s.extraEarnings" unit="€" /></div>
            <div v-if="s.bonuses !== 0" class="detail__row"><span>{{ t('resum.bonuses') }}</span><AmountDisplay :value="s.bonuses" unit="€" /></div>
            <div v-if="s.penalties !== 0" class="detail__row"><span>{{ t('resum.penalties') }}</span><span>-<AmountDisplay :value="s.penalties" unit="€" /></span></div>
          </div>
        </div>
      </div>
    </section>
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

.month {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.75rem;
}

.month__title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0;
  font-size: 1rem;
  text-transform: capitalize;
}

.month__chip {
  font-family: var(--font-body);
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: none;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 12%, transparent);
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
}

/* Mateixa targeta que el Resum del tauler (capçalera amb el color del fill i el total). */
.kid-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  box-shadow: 0 2px 12px -4px color-mix(in srgb, var(--text) 12%, transparent);
}

.kid-card__head {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.85rem 1rem;
  background: var(--child-color, var(--primary));
}

.kid-card__avatar {
  background: rgba(255, 255, 255, 0.28) !important;
  flex-shrink: 0;
}

.kid-card__name {
  flex: 1;
  min-width: 0;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.85rem;
  color: white;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kid-card__total {
  font-weight: 800;
  font-size: 1.05rem;
  color: white;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.kid-card__body {
  padding: 0.85rem 1rem;
}

.kid-card__secondary {
  display: flex;
  justify-content: space-between;
}

.kid-card__secondary-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.kid-card__secondary-label {
  font-size: 0.68rem;
  color: var(--muted);
}

.kid-card__secondary-value {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text);
  margin-top: 0.1rem;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.detail {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px dashed color-mix(in srgb, var(--text) 15%, transparent);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.detail__row {
  display: flex;
  justify-content: space-between;
  font-size: 0.82rem;
  color: var(--muted);
}

.detail__note {
  margin: 0;
  font-size: 0.74rem;
  color: var(--muted);
  font-style: italic;
}
</style>
