<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import ChildScreenHeader from '@/components/base/ChildScreenHeader.vue'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import type { ChildTaskResponse } from '@/types/child'
import type { ChildSummary } from '@/types/nfc'

const { t } = useI18n()
const auth = useAuthStore()
const tasks = ref<ChildTaskResponse[]>([])
const siblings = ref<ChildSummary[]>([])
const loading = ref(true)
const completingId = ref<string | null>(null)

const statusLabel = computed<Record<ChildTaskResponse['status'], string>>(() => ({
  AVAILABLE: '',
  PENDING: t('tasques.statusPending'),
  APPROVED: t('tasques.statusApproved'),
  REJECTED: t('tasques.statusRejected'),
  CLAIMED_BY_OTHERS: t('tasques.statusClaimedByOthers'),
}))

async function loadTasks() {
  const childId = auth.childId
  if (!childId) return
  const { data } = await api.get<ChildTaskResponse[]>(`/api/children/${childId}/tasks`)
  tasks.value = data
  loading.value = false
}

async function loadSiblings() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<ChildSummary[]>(`/api/families/${familyId}/children`)
  siblings.value = data.filter((c) => c.id !== auth.childId)
}

// Assistent de col·laboració (Prompt 15) — només per a tasques Extra sense assignació fixa.
const collabTask = ref<ChildTaskResponse | null>(null)
const collabStep = ref<1 | 2>(1)
const collabSelected = ref<string[]>([])
const submittingCollab = ref(false)
const collabDone = ref(false)

const collabParticipantCount = computed(() => 1 + collabSelected.value.length)
const collabShareMoney = computed(() =>
  collabTask.value ? Math.round((collabTask.value.rewardMoney / collabParticipantCount.value) * 100) / 100 : 0,
)
const collabShareMinutes = computed(() =>
  collabTask.value ? Math.floor(collabTask.value.rewardScreenMinutes / collabParticipantCount.value) : 0,
)

function toggleSibling(id: string) {
  const i = collabSelected.value.indexOf(id)
  if (i === -1) collabSelected.value.push(id)
  else collabSelected.value.splice(i, 1)
}

function openTask(task: ChildTaskResponse) {
  if (task.status !== 'AVAILABLE' || completingId.value) return
  if (task.taskType === 'EXTRA') {
    collabTask.value = task
    collabStep.value = 1
    collabSelected.value = []
    collabDone.value = false
    return
  }
  markDoneDirect(task, [])
}

async function markDoneDirect(task: ChildTaskResponse, collaboratorChildIds: string[]) {
  completingId.value = task.id
  try {
    await api.post(`/api/tasks/${task.id}/complete`, { collaboratorChildIds })
    await loadTasks()
  } finally {
    completingId.value = null
  }
}

function collabAlone() {
  if (!collabTask.value) return
  const task = collabTask.value
  closeCollab()
  markDoneDirect(task, [])
}

function collabNeedsHelp() {
  collabStep.value = 2
}

function collabBack() {
  collabStep.value = 1
}

async function collabConfirm() {
  if (!collabTask.value || submittingCollab.value) return
  submittingCollab.value = true
  try {
    await api.post(`/api/tasks/${collabTask.value.id}/complete`, { collaboratorChildIds: collabSelected.value })
    collabDone.value = true
    await loadTasks()
  } finally {
    submittingCollab.value = false
  }
}

function closeCollab() {
  collabTask.value = null
  collabStep.value = 1
  collabSelected.value = []
  collabDone.value = false
}

function siblingName(id: string) {
  return siblings.value.find((s) => s.id === id)?.displayName ?? ''
}

onMounted(() => {
  loadTasks()
  loadSiblings()
})
</script>

<template>
  <div class="tasques">
    <ChildScreenHeader :title="t('tasques.title')" />
    <p class="tasques__sub">{{ t('tasques.subtitle') }}</p>

    <p v-if="!loading && tasks.length === 0" class="tasques__empty">{{ t('tasques.empty') }}</p>

    <div
      v-for="task in tasks"
      :key="task.id"
      class="task-row"
      :class="`task-row--${task.status.toLowerCase()}`"
      @click="openTask(task)"
    >
      <div class="task-row__check">
        <span v-if="task.status !== 'AVAILABLE' && task.status !== 'CLAIMED_BY_OTHERS'">✓</span>
      </div>
      <div class="task-row__body">
        <div class="task-row__title" :class="{ 'task-row__title--done': task.status !== 'AVAILABLE' }">
          {{ task.name }}
        </div>
        <div class="task-row__reward">
          {{ t('tasques.rewardLabel') }}
          <AmountDisplay v-if="task.rewardMoney > 0" :value="task.rewardMoney" unit="€" />
          <span v-if="task.rewardMoney > 0 && task.rewardScreenMinutes > 0"> · </span>
          <span v-if="task.rewardScreenMinutes > 0">+{{ task.rewardScreenMinutes }} {{ t('common.minutesAbbr') }}</span>
        </div>
        <div v-if="task.status === 'CLAIMED_BY_OTHERS' && task.participantNames.length" class="task-row__participants">
          {{ t('tasques.claimedByNames', { names: task.participantNames.join(', ') }) }}
        </div>
        <div v-else-if="task.status === 'PENDING' && task.participantNames.length > 1" class="task-row__participants">
          {{ t('tasques.collaboratedWith', { names: task.participantNames.filter((n) => n !== auth.displayName).join(', ') }) }}
        </div>
      </div>
      <div class="task-row__status">{{ statusLabel[task.status] }}</div>
    </div>

    <!-- Assistent de col·laboració — tasca Extra, 2 pantalles (Prompt 15) -->
    <div v-if="collabTask" class="collab-overlay">
      <div class="collab-card">
        <template v-if="collabDone">
          <div class="collab-title">{{ t('tasques.collabSentTitle') }}</div>
          <BaseButton variant="primary" @click="closeCollab">{{ t('common.close') }}</BaseButton>
        </template>
        <template v-else-if="collabStep === 1">
          <div class="collab-title">{{ t('tasques.collabAskHelp') }}</div>
          <div class="collab-actions collab-actions--stacked">
            <BaseButton variant="primary" :disabled="!!completingId" @click="collabAlone">{{ t('tasques.collabAlone') }}</BaseButton>
            <BaseButton variant="accent" @click="collabNeedsHelp">{{ t('tasques.collabHelped') }}</BaseButton>
          </div>
          <button type="button" class="collab-close" @click="closeCollab">{{ t('common.cancel') }}</button>
        </template>
        <template v-else>
          <div class="collab-title">{{ t('tasques.collabWhoHelped') }}</div>
          <div class="collab-chip-row">
            <button
              v-for="sibling in siblings"
              :key="sibling.id"
              type="button"
              class="collab-chip"
              :class="{ active: collabSelected.includes(sibling.id) }"
              @click="toggleSibling(sibling.id)"
            >
              {{ sibling.displayName }}
            </button>
          </div>
          <p class="collab-hint">
            {{ t('tasques.collabShareHint', { count: collabParticipantCount, money: collabShareMoney, minutes: collabShareMinutes }) }}
          </p>
          <div class="collab-actions">
            <BaseButton variant="ghost" @click="collabBack">{{ t('tasques.collabBack') }}</BaseButton>
            <BaseButton variant="primary" :disabled="submittingCollab" @click="collabConfirm">
              {{ submittingCollab ? t('common.saving') : t('tasques.collabConfirm') }}
            </BaseButton>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tasques {
  max-width: 480px;
  margin: 0 auto;
  padding: 1.5rem 1.25rem 2rem;
}

.tasques__sub {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0 0 1.1rem;
}

.tasques__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.85rem;
  background: white;
  border-radius: 14px;
  margin-bottom: 0.6rem;
  box-shadow: 0 2px 6px -2px color-mix(in srgb, var(--text) 12%, transparent);
  cursor: pointer;
  transition: background 0.3s ease;
}

.task-row--pending {
  background: #fff7e6;
  cursor: default;
}

.task-row--approved {
  cursor: default;
}

.task-row--rejected {
  cursor: default;
  opacity: 0.7;
}

.task-row--claimed_by_others {
  cursor: default;
  opacity: 0.7;
}

.task-row__check {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 2px solid var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 0.85rem;
  color: white;
  transition: all 0.25s ease;
}

.task-row--pending .task-row__check {
  border-color: var(--warning);
  background: var(--warning);
}

.task-row--approved .task-row__check {
  border-color: var(--success);
  background: var(--success);
}

.task-row--rejected .task-row__check {
  border-color: var(--muted);
  background: var(--muted);
}

.task-row--claimed_by_others .task-row__check {
  border-color: var(--muted);
}

.task-row__body {
  flex: 1;
  min-width: 0;
}

.task-row__title {
  font-weight: 700;
  font-size: 0.9rem;
}

.task-row__title--done {
  color: var(--muted);
}

.task-row__reward {
  font-size: 0.76rem;
  color: var(--muted);
  margin-top: 0.15rem;
  display: flex;
  gap: 0.15rem;
  align-items: baseline;
}

.task-row__participants {
  font-size: 0.72rem;
  color: var(--primary);
  margin-top: 0.2rem;
  font-weight: 700;
}

.task-row__status {
  margin-left: auto;
  font-size: 0.68rem;
  font-weight: 700;
  color: var(--warning);
  white-space: nowrap;
  flex-shrink: 0;
}

.task-row--approved .task-row__status {
  color: var(--success);
}

.task-row--rejected .task-row__status,
.task-row--claimed_by_others .task-row__status {
  color: var(--muted);
}

.collab-overlay {
  position: fixed;
  inset: 0;
  background: color-mix(in srgb, var(--text) 55%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.collab-card {
  background: white;
  border-radius: 20px;
  padding: 2rem 1.5rem;
  width: 100%;
  max-width: 360px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.collab-title {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 1.15rem;
}

.collab-actions {
  display: flex;
  gap: 0.6rem;
}

.collab-actions--stacked {
  flex-direction: column;
}

.collab-close {
  border: none;
  background: none;
  color: var(--muted);
  font-weight: 700;
  font-size: 0.82rem;
  cursor: pointer;
}

.collab-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.collab-chip {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.85rem;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  border: 2px solid color-mix(in srgb, var(--primary) 15%, transparent);
  background: white;
  cursor: pointer;
  color: var(--text);
}

.collab-chip.active {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, white);
  color: var(--primary);
}

.collab-hint {
  font-size: 0.78rem;
  color: var(--muted);
  margin: 0;
}
</style>
