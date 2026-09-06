<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useCountUp } from '@/composables/useCountUp'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import { apiErrorMessage } from '@/utils/apiError'
import type { ChildTaskResponse, ExpenseResponse, MoneyTransactionResponse, WalletResponse } from '@/types/child'

const { t } = useI18n()
const auth = useAuthStore()
const { value: balanceDisplay, animateTo } = useCountUp()

const savingsBalance = ref(0)
const totalBalance = ref(0)
const canLogExpenses = ref(false)
const transactions = ref<MoneyTransactionResponse[]>([])
const pendingExpenses = ref<ExpenseResponse[]>([])
const pendingTaskCount = ref(0)
const loading = ref(true)

interface MovementRow {
  key: string
  label: string
  amount: number
  createdAt: string
  isCredit: boolean
  pending: boolean
}

const movementRows = computed<MovementRow[]>(() => {
  const fromPending: MovementRow[] = pendingExpenses.value.map((e) => ({
    key: e.id, label: e.reason, amount: e.amount, createdAt: e.createdAt, isCredit: false, pending: true,
  }))
  const fromTransactions: MovementRow[] = transactions.value.map((tr) => ({
    key: tr.id, label: tr.description || tr.sourceType, amount: tr.amount,
    createdAt: tr.createdAt, isCredit: tr.transactionType === 'CREDIT', pending: false,
  }))
  return [...fromPending, ...fromTransactions]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5)
})

async function load() {
  const childId = auth.childId
  if (!childId) return

  const [walletRes, transactionsRes, tasksRes, pendingExpensesRes] = await Promise.all([
    api.get<WalletResponse>(`/api/children/${childId}/wallet`),
    api.get<MoneyTransactionResponse[]>(`/api/children/${childId}/money-transactions`),
    api.get<ChildTaskResponse[]>(`/api/children/${childId}/tasks`),
    api.get<ExpenseResponse[]>(`/api/children/${childId}/expenses/pending`),
  ])

  animateTo(walletRes.data.spendingBalance)
  savingsBalance.value = walletRes.data.savingsBalance
  totalBalance.value = walletRes.data.total
  canLogExpenses.value = walletRes.data.canLogExpenses
  transactions.value = transactionsRes.data.slice(0, 5)
  pendingTaskCount.value = tasksRes.data.filter((t) => t.status === 'PENDING').length
  pendingExpenses.value = pendingExpensesRes.data
  loading.value = false
}

const loggingExpense = ref(false)
const savingExpense = ref(false)
const expenseError = ref<string | null>(null)
const expense = reactive({ amount: 0, reason: '' })

function startExpense() {
  loggingExpense.value = true
  expenseError.value = null
  Object.assign(expense, { amount: 0, reason: '' })
}

async function submitExpense() {
  expenseError.value = null
  if (expense.amount <= 0 || !expense.reason.trim()) {
    expenseError.value = t('inici.missingExpenseFields')
    return
  }
  const childId = auth.childId
  if (!childId) return
  savingExpense.value = true
  try {
    await api.post(`/api/children/${childId}/expenses`, { amount: expense.amount, reason: expense.reason })
    loggingExpense.value = false
    await load()
  } catch (err) {
    expenseError.value = apiErrorMessage(err)
  } finally {
    savingExpense.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="inici">
    <div class="inici__head">
      <div class="inici__identity">
        <ChildAvatar :color="auth.avatarColor" :icon="auth.avatarIcon" :name="auth.displayName ?? ''" />
        <h1 class="inici__greeting">{{ t('inici.greeting', { name: auth.displayName }) }}</h1>
      </div>
    </div>

    <p class="inici__sub">
      <template v-if="pendingTaskCount > 0">
        {{ t('inici.pendingTasks', { n: pendingTaskCount }, pendingTaskCount) }}
      </template>
      <template v-else>{{ t('inici.welcomeBack') }}</template>
    </p>

    <div class="balance-card">
      <div class="balance-card__label">{{ t('inici.availableBalance') }}</div>
      <div class="balance-card__amount">
        <AmountDisplay :value="balanceDisplay" unit="€" />
      </div>
      <div class="balance-card__chips">
        <div class="balance-card__chip">{{ t('inici.savingsLabel') }} <AmountDisplay :value="savingsBalance" unit="€" /></div>
        <div class="balance-card__chip">{{ t('inici.totalLabel') }} <AmountDisplay :value="totalBalance" unit="€" /></div>
      </div>
    </div>

    <template v-if="canLogExpenses">
      <BaseButton v-if="!loggingExpense" variant="secondary" class="inici__expense-btn" @click="startExpense">
        <svg class="btn-icon" viewBox="0 0 24 24"><rect x="3" y="6" width="18" height="13" rx="2.2" /><path d="M3 10.5h18M7 15.2h3.2" /></svg>
        {{ t('inici.logExpenseButton') }}
      </BaseButton>
      <form v-else class="expense-form" @submit.prevent="submitExpense">
        <label>
          {{ t('inici.expenseAmountLabel') }}
          <input v-model.number="expense.amount" type="number" min="0.01" step="0.01" required autofocus />
        </label>
        <label>
          {{ t('inici.expenseReasonLabel') }}
          <input v-model="expense.reason" type="text" required />
        </label>
        <p v-if="expenseError" class="inici__error">{{ expenseError }}</p>
        <div class="expense-form__actions">
          <BaseButton type="submit" variant="primary" :disabled="savingExpense">
            {{ savingExpense ? t('common.saving') : t('inici.expenseSubmit') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingExpense" @click="loggingExpense = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </template>

    <div class="section-label">{{ t('inici.recentMovements') }}</div>
    <div v-if="!loading && movementRows.length === 0" class="inici__empty">{{ t('inici.noMovements') }}</div>
    <div v-for="row in movementRows" :key="row.key" class="mrow">
      <span>
        {{ row.label }}
        <span v-if="row.pending" class="mrow__pending-pill">{{ t('inici.pendingPill') }}</span>
      </span>
      <span class="mrow__amt" :class="row.isCredit ? 'mrow__amt--pos' : 'mrow__amt--neg'">
        {{ row.isCredit ? '+' : '-' }}<AmountDisplay :value="row.amount" unit="€" />
      </span>
    </div>
  </div>
</template>

<style scoped>
.inici {
  max-width: 480px;
  margin: 0 auto;
  padding: 1.5rem 1.25rem 2rem;
}

.inici__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.15rem;
}

.inici__identity {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.inici__greeting {
  margin-bottom: 0;
}

.inici__sub {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0 0 1.1rem;
}

.inici__empty {
  color: var(--muted);
  font-size: 0.85rem;
  padding: 0.5rem 0;
}

.balance-card {
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  border-radius: 20px;
  padding: 1.25rem 1.4rem;
  color: white;
  margin-bottom: 1.2rem;
  box-shadow: 0 10px 24px -8px color-mix(in srgb, var(--primary) 60%, transparent);
}

.balance-card__label {
  font-size: 0.7rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  opacity: 0.85;
  margin-bottom: 0.25rem;
}

.balance-card__amount {
  font-size: 2.2rem;
  margin-bottom: 0.75rem;
}

.balance-card__amount :deep(.amount-display) {
  color: white;
}

.balance-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.balance-card__chip {
  display: inline-block;
  background: rgba(255, 255, 255, 0.18);
  font-size: 0.72rem;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  font-weight: 700;
}

.section-label {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.8rem;
  color: var(--muted);
  margin: 0.25rem 0 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.inici__expense-btn {
  margin-bottom: 1.2rem;
}

.btn-icon {
  width: 14px;
  height: 14px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}

.expense-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  background: white;
  border-radius: 16px;
  padding: 1rem 1.1rem;
  margin-bottom: 1.2rem;
  box-shadow: 0 2px 8px -3px color-mix(in srgb, var(--text) 12%, transparent);
}

.expense-form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.expense-form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.expense-form__actions {
  display: flex;
  gap: 0.5rem;
}

.inici__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.mrow__pending-pill {
  display: inline-block;
  margin-left: 0.35rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--warning) 20%, transparent);
  color: color-mix(in srgb, var(--warning) 60%, var(--text));
  font-size: 0.62rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  vertical-align: middle;
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
</style>
