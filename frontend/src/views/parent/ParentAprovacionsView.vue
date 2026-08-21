<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useApprovalsStore } from '@/stores/approvals'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import type { NegativeBalanceSessionResponse, PendingApprovalResponse } from '@/types/parent'

const auth = useAuthStore()
const approvalsStore = useApprovalsStore()

const approvals = ref<PendingApprovalResponse[]>([])
const negativeSessions = ref<NegativeBalanceSessionResponse[]>([])
const loading = ref(true)
const resolvingId = ref<string | null>(null)

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const [approvalsRes, negativeRes] = await Promise.all([
    api.get<PendingApprovalResponse[]>(`/api/families/${familyId}/pending-approvals`),
    api.get<NegativeBalanceSessionResponse[]>(`/api/families/${familyId}/screen-sessions/negative-balance`),
  ])
  approvals.value = approvalsRes.data
  negativeSessions.value = negativeRes.data
  loading.value = false
}

async function resolve(item: PendingApprovalResponse, action: 'approve' | 'reject') {
  if (resolvingId.value) return
  resolvingId.value = item.taskCompletionId
  try {
    await api.post(`/api/task-completions/${item.taskCompletionId}/${action}`)
    approvals.value = approvals.value.filter((a) => a.taskCompletionId !== item.taskCompletionId)
    approvalsStore.decrement()
  } finally {
    resolvingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="aprovacions">
    <h1>Aprovacions pendents</h1>
    <p class="aprovacions__sub">Cap acció de recompensa és efectiva fins que l'aproves</p>

    <p v-if="!loading && approvals.length === 0" class="aprovacions__empty">No hi ha res pendent d'aprovació.</p>

    <TransitionGroup name="approval" tag="div">
      <div v-for="item in approvals" :key="item.taskCompletionId" class="approval-row">
        <div class="approval-row__info">
          <div class="approval-row__title">{{ item.childName }} — {{ item.taskName }}</div>
          <div class="approval-row__sub">
            Recompensa sol·licitada:
            <template v-if="item.rewardMoney > 0"><AmountDisplay :value="item.rewardMoney" unit="€" /></template>
            <template v-if="item.rewardScreenMinutes > 0"> +{{ item.rewardScreenMinutes }} min</template>
          </div>
        </div>
        <div class="approval-row__actions">
          <BaseButton variant="primary" :disabled="!!resolvingId" @click="resolve(item, 'approve')">Aprovar</BaseButton>
          <BaseButton variant="danger" :disabled="!!resolvingId" @click="resolve(item, 'reject')">Rebutjar</BaseButton>
        </div>
      </div>
    </TransitionGroup>

    <template v-if="negativeSessions.length > 0">
      <div class="section-label">Sessions de pantalla amb saldo negatiu</div>
      <div v-for="(s, i) in negativeSessions" :key="i" class="negative-row">
        {{ s.childName }} ha quedat en saldo negatiu de temps de pantalla — es recupera amb la propera paga de temps.
      </div>
    </template>
  </div>
</template>

<style scoped>
.aprovacions {
  max-width: 640px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.aprovacions__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1.25rem;
}

.aprovacions__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.approval-row {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  background: white;
  border-radius: 14px;
  padding: 0.85rem 1rem;
  margin-bottom: 0.6rem;
  box-shadow: 0 2px 8px -2px color-mix(in srgb, var(--text) 12%, transparent);
  max-height: 90px;
  overflow: hidden;
}

.approval-row__info {
  flex: 1;
  min-width: 0;
}

.approval-row__title {
  font-weight: 700;
  font-size: 0.88rem;
}

.approval-row__sub {
  font-size: 0.76rem;
  color: var(--muted);
  margin-top: 0.15rem;
}

.approval-row__actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.approval-row__actions :deep(.base-button) {
  padding: 0.5rem 0.9rem;
  font-size: 0.82rem;
}

.approval-leave-active {
  transition:
    opacity 0.35s ease,
    max-height 0.35s ease,
    margin 0.35s ease,
    padding 0.35s ease;
}

.approval-leave-to {
  opacity: 0;
  max-height: 0;
  margin: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.section-label {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.8rem;
  color: var(--muted);
  margin: 1.25rem 0 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.negative-row {
  background: color-mix(in srgb, var(--error) 8%, white);
  color: var(--error);
  border-radius: 12px;
  padding: 0.7rem 0.9rem;
  font-size: 0.82rem;
  margin-bottom: 0.5rem;
}
</style>
