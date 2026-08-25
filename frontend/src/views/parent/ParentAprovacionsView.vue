<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useApprovalsStore } from '@/stores/approvals'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import type { NegativeBalanceSessionResponse, PendingApprovalResponse } from '@/types/parent'

const { t } = useI18n()
const auth = useAuthStore()
const approvalsStore = useApprovalsStore()

const approvals = ref<PendingApprovalResponse[]>([])
const negativeSessions = ref<NegativeBalanceSessionResponse[]>([])
const loading = ref(true)
const resolvingId = ref<string | null>(null)

// Totes les finalitzacions comparteixen un completionGroupId (individual o col·laboratiu,
// Prompt 15) — s'agrupen sempre en una sola fila i s'aproven/rebutgen amb l'endpoint de grup.
const groups = computed(() => {
  const byGroup = new Map<string, PendingApprovalResponse[]>()
  for (const item of approvals.value) {
    const list = byGroup.get(item.completionGroupId) ?? []
    list.push(item)
    byGroup.set(item.completionGroupId, list)
  }
  return [...byGroup.entries()].map(([completionGroupId, items]) => ({
    completionGroupId,
    taskName: items[0]!.taskName,
    childNames: items.map((i) => i.childName),
    rewardMoney: items[0]!.rewardMoney,
    rewardScreenMinutes: items[0]!.rewardScreenMinutes,
  }))
})

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

async function resolve(completionGroupId: string, action: 'approve' | 'reject') {
  if (resolvingId.value) return
  resolvingId.value = completionGroupId
  try {
    await api.post(`/api/task-completions/group/${completionGroupId}/${action}`)
    const removed = approvals.value.filter((a) => a.completionGroupId === completionGroupId).length
    approvals.value = approvals.value.filter((a) => a.completionGroupId !== completionGroupId)
    approvalsStore.decrement(removed)
  } finally {
    resolvingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="aprovacions">
    <h1>{{ t('aprovacions.title') }}</h1>
    <p class="aprovacions__sub">{{ t('aprovacions.subtitle') }}</p>

    <p v-if="!loading && approvals.length === 0" class="aprovacions__empty">{{ t('aprovacions.empty') }}</p>

    <TransitionGroup name="approval" tag="div">
      <div v-for="group in groups" :key="group.completionGroupId" class="approval-row">
        <div class="approval-row__info">
          <div class="approval-row__title">{{ group.childNames.join(' + ') }} — {{ group.taskName }}</div>
          <div class="approval-row__sub">
            {{ t('aprovacions.requestedReward') }}
            <template v-if="group.rewardMoney > 0"><AmountDisplay :value="group.rewardMoney" unit="€" /></template>
            <template v-if="group.rewardScreenMinutes > 0"> +{{ group.rewardScreenMinutes }} {{ t('common.minutesAbbr') }}</template>
            <span v-if="group.childNames.length > 1" class="approval-row__collab">{{ t('aprovacions.collaborative') }}</span>
          </div>
        </div>
        <div class="approval-row__actions">
          <BaseButton variant="primary" :disabled="!!resolvingId" @click="resolve(group.completionGroupId, 'approve')">{{ t('aprovacions.approve') }}</BaseButton>
          <BaseButton variant="danger" :disabled="!!resolvingId" @click="resolve(group.completionGroupId, 'reject')">{{ t('aprovacions.reject') }}</BaseButton>
        </div>
      </div>
    </TransitionGroup>

    <template v-if="negativeSessions.length > 0">
      <div class="section-label">{{ t('aprovacions.negativeBalanceSectionTitle') }}</div>
      <div v-for="(s, i) in negativeSessions" :key="i" class="negative-row">
        {{ t('aprovacions.negativeBalanceRow', { name: s.childName }) }}
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

.approval-row__collab {
  margin-left: 0.4rem;
  font-weight: 700;
  color: var(--primary);
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
