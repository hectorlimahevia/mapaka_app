<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useCountUp } from '@/composables/useCountUp'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import type { ChildTaskResponse, MoneyTransactionResponse, WalletResponse } from '@/types/child'

const { t } = useI18n()
const auth = useAuthStore()
const { value: balanceDisplay, animateTo } = useCountUp()

const savingsBalance = ref(0)
const transactions = ref<MoneyTransactionResponse[]>([])
const pendingTaskCount = ref(0)
const loading = ref(true)

onMounted(async () => {
  const childId = auth.childId
  if (!childId) return

  const [walletRes, transactionsRes, tasksRes] = await Promise.all([
    api.get<WalletResponse>(`/api/children/${childId}/wallet`),
    api.get<MoneyTransactionResponse[]>(`/api/children/${childId}/money-transactions`),
    api.get<ChildTaskResponse[]>(`/api/children/${childId}/tasks`),
  ])

  animateTo(walletRes.data.spendingBalance)
  savingsBalance.value = walletRes.data.savingsBalance
  transactions.value = transactionsRes.data.slice(0, 5)
  pendingTaskCount.value = tasksRes.data.filter((t) => t.status === 'PENDING').length
  loading.value = false
})
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
      <div class="balance-card__chip">{{ t('inici.savingsLabel') }} <AmountDisplay :value="savingsBalance" unit="€" /></div>
    </div>

    <div class="section-label">{{ t('inici.recentMovements') }}</div>
    <div v-if="!loading && transactions.length === 0" class="inici__empty">{{ t('inici.noMovements') }}</div>
    <div v-for="t in transactions" :key="t.id" class="mrow">
      <span>{{ t.description || t.sourceType }}</span>
      <span class="mrow__amt" :class="t.transactionType === 'CREDIT' ? 'mrow__amt--pos' : 'mrow__amt--neg'">
        {{ t.transactionType === 'CREDIT' ? '+' : '-' }}<AmountDisplay :value="t.amount" unit="€" />
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
