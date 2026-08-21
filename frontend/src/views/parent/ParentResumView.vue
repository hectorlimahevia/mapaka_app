<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import type { ChildFamilySummary, FamilyMoneyTransactionResponse } from '@/types/parent'

const { t } = useI18n()
const auth = useAuthStore()
const children = ref<ChildFamilySummary[]>([])
const movements = ref<FamilyMoneyTransactionResponse[]>([])
const loading = ref(true)

onMounted(async () => {
  const familyId = auth.familyId
  if (!familyId) return
  const [summaryRes, movementsRes] = await Promise.all([
    api.get<ChildFamilySummary[]>(`/api/families/${familyId}/summary`),
    api.get<FamilyMoneyTransactionResponse[]>(`/api/families/${familyId}/money-transactions`),
  ])
  children.value = summaryRes.data
  movements.value = movementsRes.data.slice(0, 8)
  loading.value = false
})
</script>

<template>
  <div class="resum">
    <h1>{{ t('resum.title') }}</h1>
    <p class="resum__sub">{{ t('resum.subtitle') }}</p>

    <p v-if="!loading && children.length === 0" class="resum__empty">{{ t('resum.emptyChildren') }}</p>

    <div class="kids-grid">
      <div v-for="child in children" :key="child.childId" class="kid-card">
        <div class="kid-card__name">{{ child.displayName }}</div>
        <div class="kid-card__amount"><AmountDisplay :value="child.spendingBalance" unit="€" /></div>
        <div class="kid-card__sub">
          <template v-if="child.pendingApprovalsCount > 0">
            {{ t('resum.pendingApprovals', { n: child.pendingApprovalsCount }, child.pendingApprovalsCount) }}
          </template>
          <template v-else>{{ t('resum.noPendingApprovals') }}</template>
        </div>
      </div>
    </div>

    <div class="section-label">{{ t('resum.recentFamilyMovements') }}</div>
    <div v-if="!loading && movements.length === 0" class="resum__empty">{{ t('resum.noMovements') }}</div>
    <div v-for="m in movements" :key="m.id" class="mrow">
      <span>{{ m.childDisplayName }} — {{ m.description || m.sourceType }}</span>
      <span class="mrow__amt" :class="m.transactionType === 'CREDIT' ? 'mrow__amt--pos' : 'mrow__amt--neg'">
        {{ m.transactionType === 'CREDIT' ? '+' : '-' }}<AmountDisplay :value="m.amount" unit="€" />
      </span>
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

.kids-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.9rem;
  margin-bottom: 1.5rem;
}

.kid-card {
  background: white;
  border-radius: 14px;
  padding: 1rem 1.1rem;
  box-shadow: 0 2px 8px -2px color-mix(in srgb, var(--text) 12%, transparent);
}

.kid-card__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
  margin-bottom: 0.4rem;
}

.kid-card__amount {
  font-size: 1.35rem;
  color: var(--primary);
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
