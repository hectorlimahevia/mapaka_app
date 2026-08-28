<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import { formatDate } from '@/utils/date'
import { apiErrorMessage } from '@/utils/apiError'
import type { AppLocale } from '@/i18n'
import type { AllowanceStatusResponse, ChildFamilySummary, FamilyMoneyTransactionResponse } from '@/types/parent'
import type { MonthlyAllowanceResponse } from '@/types/parent'

const { t, locale } = useI18n()
const auth = useAuthStore()
const children = ref<ChildFamilySummary[]>([])
const movements = ref<FamilyMoneyTransactionResponse[]>([])
const loading = ref(true)

const generating = ref(false)
const pendingAllowances = ref<MonthlyAllowanceResponse[]>([])
const resolvingAllowanceId = ref<string | null>(null)
const allowanceGeneratedThisMonth = ref(true)

type Period = 'week' | 'month' | 'custom'
const period = ref<Period>('week')
const customFrom = ref('')
const customTo = ref('')
const selectedChildId = ref<string | null>(null)

const GOAL_COLORS = ['#F59E0B', '#8B5CF6', '#EC4899', '#0EA5E9']

function periodRange(): { from: Date | null; to: Date | null } {
  const now = new Date()
  if (period.value === 'week') {
    const from = new Date(now)
    from.setDate(from.getDate() - 7)
    return { from, to: now }
  }
  if (period.value === 'month') {
    return { from: new Date(now.getFullYear(), now.getMonth(), 1), to: now }
  }
  return {
    from: customFrom.value ? new Date(customFrom.value) : null,
    to: customTo.value ? new Date(customTo.value) : null,
  }
}

async function loadMovements() {
  const familyId = auth.familyId
  if (!familyId) return
  const { from, to } = periodRange()
  const { data } = await api.get<FamilyMoneyTransactionResponse[]>(`/api/families/${familyId}/money-transactions`, {
    params: {
      childId: selectedChildId.value ?? undefined,
      from: from ? from.toISOString() : undefined,
      to: to ? to.toISOString() : undefined,
      size: 50,
    },
  })
  movements.value = data
}

async function loadSummary() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<ChildFamilySummary[]>(`/api/families/${familyId}/summary`)
  children.value = data
}

async function loadAllowanceStatus() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<AllowanceStatusResponse>(`/api/families/${familyId}/allowance-status`)
  allowanceGeneratedThisMonth.value = data.generatedThisMonth
}

async function load() {
  await Promise.all([loadSummary(), loadMovements(), loadAllowanceStatus()])
  loading.value = false
}

watch([period, selectedChildId], () => loadMovements())

async function generateAllowances() {
  generating.value = true
  try {
    const { data } = await api.post<MonthlyAllowanceResponse[]>('/api/allowances/generate')
    pendingAllowances.value = data
    await loadAllowanceStatus()
  } finally {
    generating.value = false
  }
}

async function resolveAllowance(allowance: MonthlyAllowanceResponse, action: 'confirm' | 'cancel') {
  if (resolvingAllowanceId.value) return
  resolvingAllowanceId.value = allowance.id
  try {
    await api.post(`/api/allowances/${allowance.id}/${action}`)
    pendingAllowances.value = pendingAllowances.value.filter((a) => a.id !== allowance.id)
    if (action === 'confirm') await load()
  } finally {
    resolvingAllowanceId.value = null
  }
}

function segmentPercent(amount: number, total: number) {
  if (total <= 0) return 0
  return Math.max(0, (amount / total) * 100)
}

function goalColor(index: number) {
  return GOAL_COLORS[index % GOAL_COLORS.length]
}

const donatingGoal = ref<{ goalId: string; name: string } | null>(null)
const donationAmount = ref(0)
const donationDonorName = ref('')
const donationMessage = ref('')
const donatingSaving = ref(false)
const donationError = ref<string | null>(null)

function openDonate(goalId: string, name: string) {
  donatingGoal.value = { goalId, name }
  donationAmount.value = 0
  donationDonorName.value = ''
  donationMessage.value = ''
  donationError.value = null
}

function closeDonate() {
  donatingGoal.value = null
}

async function submitDonation() {
  if (!donatingGoal.value) return
  donationError.value = null
  if (donationAmount.value <= 0) {
    donationError.value = t('resum.donationMissingAmount')
    return
  }
  donatingSaving.value = true
  try {
    await api.post(`/api/savings-goals/${donatingGoal.value.goalId}/donations`, {
      amount: donationAmount.value,
      donorName: donationDonorName.value || null,
      message: donationMessage.value || null,
    })
    donatingGoal.value = null
    await Promise.all([loadSummary(), loadMovements()])
  } catch (err) {
    donationError.value = apiErrorMessage(err)
  } finally {
    donatingSaving.value = false
  }
}

const groupedMovements = computed(() => {
  const shown = period.value === 'week' ? movements.value.slice(0, 10) : movements.value
  const groups: { dateKey: string; label: string; items: FamilyMoneyTransactionResponse[] }[] = []
  for (const m of shown) {
    const dateKey = m.createdAt.slice(0, 10)
    let group = groups.find((g) => g.dateKey === dateKey)
    if (!group) {
      group = { dateKey, label: formatDate(m.createdAt, locale.value as AppLocale), items: [] }
      groups.push(group)
    }
    group.items.push(m)
  }
  return groups
})

onMounted(load)
</script>

<template>
  <div class="resum">
    <h1>{{ t('resum.title') }}</h1>
    <p class="resum__sub">{{ t('resum.subtitle') }}</p>

    <div v-if="!allowanceGeneratedThisMonth" class="allowance-reminder">{{ t('resum.allowanceReminder') }}</div>

    <p v-if="!loading && children.length === 0" class="resum__empty">{{ t('resum.emptyChildren') }}</p>

    <div class="kids-grid">
      <BaseCard
        v-for="child in children"
        :key="child.childId"
        class="kid-card"
        :style="{ '--child-color': child.avatarColor ?? 'var(--primary)' }"
      >
        <div class="kid-card__head">
          <ChildAvatar :color="child.avatarColor" :icon="child.avatarIcon" :name="child.displayName" size="small" />
          <div class="kid-card__name">{{ child.displayName }}</div>
        </div>

        <div class="kid-card__stats">
          <div class="kid-card__stat">
            <div class="kid-card__stat-label">{{ t('resum.statTotal') }}</div>
            <div class="kid-card__stat-value"><AmountDisplay :value="child.totalBalance" unit="€" /></div>
          </div>
          <div class="kid-card__stat">
            <div class="kid-card__stat-label">{{ t('resum.statSpending') }}</div>
            <div class="kid-card__stat-value"><AmountDisplay :value="child.spendingBalance" unit="€" /></div>
          </div>
          <div class="kid-card__stat">
            <div class="kid-card__stat-label">{{ t('resum.statSavings') }}</div>
            <div class="kid-card__stat-value"><AmountDisplay :value="child.savingsBalance" unit="€" /></div>
          </div>
        </div>

        <div class="kid-card__bar">
          <span
            class="kid-card__bar-seg"
            :style="{ width: segmentPercent(child.spendingBalance, child.totalBalance) + '%', background: 'var(--primary)' }"
          />
          <span
            class="kid-card__bar-seg"
            :style="{ width: segmentPercent(child.savingsBalance, child.totalBalance) + '%', background: 'var(--success)' }"
          />
          <span
            v-for="(goal, i) in child.goals"
            :key="goal.goalId"
            class="kid-card__bar-seg"
            :style="{ width: segmentPercent(goal.currentAmount, child.totalBalance) + '%', background: goalColor(i) }"
          />
        </div>

        <div class="kid-card__legend">
          <span class="kid-card__legend-item">
            <span class="kid-card__dot" style="background: var(--primary)" />{{ t('resum.statSpending') }}
          </span>
          <span class="kid-card__legend-item">
            <span class="kid-card__dot" style="background: var(--success)" />{{ t('resum.statSavings') }}
          </span>
          <span v-for="(goal, i) in child.goals" :key="goal.goalId" class="kid-card__legend-item">
            <span class="kid-card__dot" :style="{ background: goalColor(i) }" />{{ goal.name }}
            <button type="button" class="kid-card__donate-btn" @click="openDonate(goal.goalId, goal.name)">🎁</button>
          </span>
        </div>

        <div class="kid-card__sub">
          <template v-if="child.pendingApprovalsCount > 0">
            {{ t('resum.pendingApprovals', { n: child.pendingApprovalsCount }, child.pendingApprovalsCount) }}
          </template>
          <template v-else>{{ t('resum.noPendingApprovals') }}</template>
        </div>
      </BaseCard>
    </div>

    <div class="resum__actions">
      <BaseButton variant="primary" :disabled="generating" @click="generateAllowances">
        {{ generating ? t('resum.generating') : t('resum.generateAllowances') }}
      </BaseButton>
      <RouterLink :to="{ name: 'parent-settlements' }" class="resum__settlements-link text-link-underline">{{ t('resum.settlementsLink') }}</RouterLink>
    </div>

    <template v-if="pendingAllowances.length > 0">
      <div class="section-label">{{ t('resum.pendingAllowancesTitle') }}</div>
      <BaseCard v-for="allowance in pendingAllowances" :key="allowance.id" class="allowance-row">
        <div>
          <div class="allowance-row__name">{{ allowance.childDisplayName }}</div>
          <div class="allowance-row__amount">
            <AmountDisplay :value="allowance.grossAmount" unit="€" />
            — {{ t('resum.allowanceSplit') }}
            <AmountDisplay :value="allowance.spendingAmount" unit="€" /> /
            <AmountDisplay :value="allowance.savingsAmount" unit="€" />
          </div>
        </div>
        <div class="allowance-row__actions">
          <BaseButton variant="primary" :disabled="!!resolvingAllowanceId" @click="resolveAllowance(allowance, 'confirm')">
            {{ t('resum.confirm') }}
          </BaseButton>
          <BaseButton variant="ghost" :disabled="!!resolvingAllowanceId" @click="resolveAllowance(allowance, 'cancel')">
            {{ t('common.cancel') }}
          </BaseButton>
        </div>
      </BaseCard>
    </template>

    <div class="movements-head">
      <div class="section-label">{{ t('resum.recentFamilyMovements') }}</div>
      <RouterLink :to="{ name: 'parent-movements' }" class="resum__all-link text-link-underline">{{ t('resum.viewAllMovements') }}</RouterLink>
    </div>

    <div class="filter-row">
      <button type="button" class="filter-chip" :class="{ active: period === 'week' }" @click="period = 'week'">
        {{ t('resum.periodWeek') }}
      </button>
      <button type="button" class="filter-chip" :class="{ active: period === 'month' }" @click="period = 'month'">
        {{ t('resum.periodMonth') }}
      </button>
      <button type="button" class="filter-chip" :class="{ active: period === 'custom' }" @click="period = 'custom'">
        {{ t('resum.periodCustom') }}
      </button>
    </div>

    <div v-if="period === 'custom'" class="custom-range">
      <label>
        {{ t('resum.customFromLabel') }}
        <input v-model="customFrom" type="date" @change="loadMovements" />
      </label>
      <label>
        {{ t('resum.customToLabel') }}
        <input v-model="customTo" type="date" @change="loadMovements" />
      </label>
    </div>

    <div class="filter-row">
      <button type="button" class="filter-chip filter-chip--child" :class="{ active: selectedChildId === null }" @click="selectedChildId = null">
        {{ t('resum.filterAll') }}
      </button>
      <button
        v-for="child in children"
        :key="child.childId"
        type="button"
        class="filter-chip filter-chip--child"
        :class="{ active: selectedChildId === child.childId }"
        :style="{ '--child-color': child.avatarColor ?? 'var(--primary)' }"
        @click="selectedChildId = child.childId"
      >
        <span class="filter-chip__dot" />{{ child.displayName }}
      </button>
    </div>

    <p v-if="!loading && movements.length === 0" class="resum__empty">{{ t('resum.noMovements') }}</p>
    <template v-for="group in groupedMovements" :key="group.dateKey">
      <div class="date-label">{{ group.label }}</div>
      <div v-for="m in group.items" :key="m.id" class="mrow">
        <span>{{ m.childDisplayName }} — {{ m.description || m.sourceType }}</span>
        <span class="mrow__amt" :class="m.transactionType === 'CREDIT' ? 'mrow__amt--pos' : 'mrow__amt--neg'">
          {{ m.transactionType === 'CREDIT' ? '+' : '-' }}<AmountDisplay :value="m.amount" unit="€" />
        </span>
      </div>
    </template>

    <div v-if="donatingGoal" class="donate-overlay" @click.self="closeDonate">
      <div class="donate-modal">
        <h2 class="donate-modal__title">{{ t('resum.donateTitle', { name: donatingGoal.name }) }}</h2>
        <form class="donate-form" @submit.prevent="submitDonation">
          <label>
            {{ t('resum.donationAmountLabel') }}
            <input v-model.number="donationAmount" type="number" min="0.01" step="0.01" required autofocus />
          </label>
          <label>
            {{ t('resum.donorNameLabel') }}
            <input v-model="donationDonorName" type="text" />
          </label>
          <label>
            {{ t('resum.donationMessageLabel') }}
            <input v-model="donationMessage" type="text" />
          </label>
          <p v-if="donationError" class="donate-form__error">{{ donationError }}</p>
          <div class="donate-form__actions">
            <BaseButton type="button" variant="ghost" :disabled="donatingSaving" @click="closeDonate">{{ t('common.cancel') }}</BaseButton>
            <BaseButton type="submit" variant="primary" :disabled="donatingSaving">
              {{ donatingSaving ? t('common.saving') : t('resum.donationSubmit') }}
            </BaseButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.resum {
  max-width: 760px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.resum__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1.25rem;
}

.resum__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.allowance-reminder {
  background: color-mix(in srgb, var(--warning) 15%, white);
  color: color-mix(in srgb, var(--warning) 70%, black);
  border-radius: 12px;
  padding: 0.65rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  margin-bottom: 1.25rem;
}

.kids-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 0.9rem;
  margin-bottom: 1.5rem;
}

.kid-card {
  border-left: 4px solid var(--child-color, var(--primary));
}

.kid-card__head {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.75rem;
}

.kid-card__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
}

.kid-card__stats {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.6rem;
}

.kid-card__stat {
  flex: 1;
  text-align: center;
}

.kid-card__stat-label {
  font-size: 0.62rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--muted);
  margin-bottom: 0.1rem;
}

.kid-card__stat-value {
  font-size: 1rem;
  font-weight: 700;
  color: var(--child-color, var(--primary));
}

.kid-card__bar {
  display: flex;
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: color-mix(in srgb, var(--text) 8%, transparent);
  margin-bottom: 0.6rem;
}

.kid-card__bar-seg {
  height: 100%;
}

.kid-card__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.75rem;
  margin-bottom: 0.5rem;
}

.kid-card__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.68rem;
  color: var(--muted);
}

.kid-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.kid-card__donate-btn {
  border: none;
  background: none;
  padding: 0;
  margin-left: 0.1rem;
  font-size: 0.72rem;
  line-height: 1;
  cursor: pointer;
  display: inline-block;
  transition: transform 0.15s ease;
}

.kid-card__donate-btn:hover {
  transform: scale(1.2);
}

@media (prefers-reduced-motion: reduce) {
  .kid-card__donate-btn {
    transition: none;
  }

  .kid-card__donate-btn:hover {
    transform: none;
  }
}

.kid-card__sub {
  font-size: 0.72rem;
  color: var(--muted);
  margin-top: 0.2rem;
}

.section-label {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.8rem;
  color: var(--muted);
  margin: 0.25rem 0 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.movements-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
}

.resum__all-link {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--primary);
  text-decoration: none;
  margin-bottom: 0.75rem;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.9rem;
}

.filter-chip {
  border: none;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.4rem 0.9rem;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.78rem;
  color: var(--muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.filter-chip.active {
  background: var(--primary);
  color: white;
}

.filter-chip--child.active {
  background: var(--child-color, var(--primary));
}

.filter-chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--child-color, var(--primary));
  flex-shrink: 0;
}

.filter-chip--child.active .filter-chip__dot {
  background: white;
}

.custom-range {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.9rem;
}

.custom-range label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--muted);
}

.custom-range input {
  font: inherit;
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.date-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin: 0.9rem 0 0.4rem;
}

.mrow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.55rem 0;
  border-bottom: 1px dashed color-mix(in srgb, var(--primary) 15%, transparent);
  font-size: 0.9rem;
}

.mrow:last-child {
  border-bottom: none;
}

.mrow__amt {
  display: inline-flex;
  gap: 0.1rem;
}

.mrow__amt--pos {
  color: var(--success);
}

.mrow__amt--neg {
  color: var(--error);
}

.resum__actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.resum__settlements-link {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--primary);
  text-decoration: none;
}

.allowance-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.6rem;
}

.allowance-row__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
}

.allowance-row__amount {
  font-size: 0.78rem;
  color: var(--muted);
  margin-top: 0.2rem;
}

.allowance-row__actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.donate-overlay {
  position: fixed;
  inset: 0;
  background: color-mix(in srgb, var(--text) 55%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.donate-modal {
  background: white;
  border-radius: 20px;
  padding: 1.75rem 1.5rem;
  width: 100%;
  max-width: 360px;
}

.donate-modal__title {
  font-size: 1.05rem;
  margin: 0 0 1rem;
}

.donate-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.donate-form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.donate-form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.donate-form__error {
  color: var(--error);
  font-size: 0.82rem;
  font-weight: 700;
  margin: 0;
}

.donate-form__actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.donate-form__actions :deep(.base-button) {
  flex: 1;
}
</style>
