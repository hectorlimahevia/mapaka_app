<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseSwitch from '@/components/base/BaseSwitch.vue'
import FormRow from '@/components/base/FormRow.vue'
import MinutesInput from '@/components/base/MinutesInput.vue'
import { apiErrorMessage } from '@/utils/apiError'
import { formatMoney } from '@/utils/money'
import type { ChildDetailResponse, IncompleteTaskResponse, RecurrenceType, TaskManagementResponse, TaskRequest } from '@/types/parent'
import type { TaskType } from '@/types/child'

const { t } = useI18n()
const auth = useAuthStore()

const tasks = ref<TaskManagementResponse[]>([])
const children = ref<ChildDetailResponse[]>([])
const loading = ref(true)
const typeFilter = ref<'ALL' | TaskType>('ALL')
const viewMode = ref<'tasks' | 'incomplete'>('tasks')
const incompleteTasks = ref<IncompleteTaskResponse[]>([])
const applyingPenaltyKey = ref<string | null>(null)

const editingId = ref<string | null>(null)
const formOpen = ref(false)
const activeStep = ref(0)
const ALL_STEPS = ['tasques.stepBasic', 'tasques.stepReward', 'tasques.stepAssign'] as const
const saving = ref(false)
const formError = ref<string | null>(null)

const RECURRENCES: RecurrenceType[] = ['NONE', 'DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM']

const form = reactive<TaskRequest>({
  name: '',
  description: '',
  taskType: 'RESPONSIBILITY',
  icon: null,
  requiresApproval: true,
  recurrenceType: 'NONE',
  rewardMoney: 0,
  rewardScreenMinutes: 0,
  penaltyMoneyAmount: 0,
  penaltyScreenMinutes: 0,
  childIds: [],
})

// Cada tipus d'import té el seu propi interruptor (Prompt 15) — apagat força el valor a 0.
const moneyRewardOn = ref(false)
const minutesRewardOn = ref(false)
const moneyPenaltyOn = ref(false)
const minutesPenaltyOn = ref(false)

watch(moneyRewardOn, (on) => { if (!on) form.rewardMoney = 0 })
watch(minutesRewardOn, (on) => { if (!on) form.rewardScreenMinutes = 0 })
watch(moneyPenaltyOn, (on) => { if (!on) form.penaltyMoneyAmount = 0 })
watch(minutesPenaltyOn, (on) => { if (!on) form.penaltyScreenMinutes = 0 })

// Una tasca Extra no té pas d'assignació (Prompt 15): visible per a tota la família.
const visibleSteps = computed(() => (form.taskType === 'EXTRA' ? ALL_STEPS.slice(0, 2) : ALL_STEPS))
watch(() => form.taskType, () => {
  if (activeStep.value >= visibleSteps.value.length) activeStep.value = visibleSteps.value.length - 1
})

const visibleTasks = computed(() =>
  typeFilter.value === 'ALL' ? tasks.value : tasks.value.filter((task) => task.taskType === typeFilter.value),
)

const assignedChildren = computed(() => children.value.filter((c) => form.childIds.includes(c.childId)))
const previewPercentages = computed(() => [...new Set(assignedChildren.value.map((c) => c.allowanceSpendingPercentage ?? 100))])
const previewSplit = computed(() => {
  if (form.rewardMoney <= 0 || previewPercentages.value.length === 0) return null
  const spendingPercentage = previewPercentages.value[0]!
  const spend = Math.round(form.rewardMoney * spendingPercentage) / 100
  const save = Math.round((form.rewardMoney - spend) * 100) / 100
  return { spendingPercentage, savingsPercentage: 100 - spendingPercentage, spend, save }
})

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const [tasksRes, childrenRes] = await Promise.all([
    api.get<TaskManagementResponse[]>('/api/tasks'),
    api.get<ChildDetailResponse[]>(`/api/families/${familyId}/children/detail`),
  ])
  tasks.value = tasksRes.data
  children.value = childrenRes.data
  loading.value = false
}

async function loadIncomplete() {
  const { data } = await api.get<IncompleteTaskResponse[]>('/api/tasks/incomplete')
  incompleteTasks.value = data
}

async function showIncomplete() {
  viewMode.value = 'incomplete'
  formOpen.value = false
  await loadIncomplete()
}

async function applyPenalty(item: IncompleteTaskResponse) {
  const key = `${item.taskId}-${item.childId}`
  if (applyingPenaltyKey.value) return
  applyingPenaltyKey.value = key
  try {
    await api.post(`/api/tasks/${item.taskId}/children/${item.childId}/apply-penalty`)
    incompleteTasks.value = incompleteTasks.value.filter((i) => `${i.taskId}-${i.childId}` !== key)
  } finally {
    applyingPenaltyKey.value = null
  }
}

function resetForm() {
  Object.assign(form, {
    name: '', description: '', taskType: 'RESPONSIBILITY', icon: null, requiresApproval: true,
    recurrenceType: 'NONE', rewardMoney: 0, rewardScreenMinutes: 0,
    penaltyMoneyAmount: 0, penaltyScreenMinutes: 0, childIds: [],
  })
  moneyRewardOn.value = false
  minutesRewardOn.value = false
  moneyPenaltyOn.value = false
  minutesPenaltyOn.value = false
}

function startCreate() {
  viewMode.value = 'tasks'
  editingId.value = null
  resetForm()
  formError.value = null
  activeStep.value = 0
  formOpen.value = true
}

function startEdit(task: TaskManagementResponse) {
  viewMode.value = 'tasks'
  editingId.value = task.id
  Object.assign(form, {
    name: task.name, description: task.description ?? '', taskType: task.taskType, icon: task.icon,
    requiresApproval: task.requiresApproval, recurrenceType: task.recurrenceType,
    rewardMoney: task.rewardMoney, rewardScreenMinutes: task.rewardScreenMinutes,
    penaltyMoneyAmount: task.penaltyMoneyAmount, penaltyScreenMinutes: task.penaltyScreenMinutes,
    childIds: task.assignedChildren.map((c) => c.childId),
  })
  moneyRewardOn.value = task.rewardMoney > 0
  minutesRewardOn.value = task.rewardScreenMinutes > 0
  moneyPenaltyOn.value = task.penaltyMoneyAmount > 0
  minutesPenaltyOn.value = task.penaltyScreenMinutes > 0
  formError.value = null
  activeStep.value = 0
  formOpen.value = true
}

function toggleChild(childId: string) {
  const index = form.childIds.indexOf(childId)
  if (index === -1) form.childIds.push(childId)
  else form.childIds.splice(index, 1)
}

async function submitForm() {
  formError.value = null
  if (!form.name.trim()) {
    formError.value = t('tasques.missingName')
    activeStep.value = 0
    return
  }
  if (form.taskType === 'EXTRA') {
    if (form.rewardMoney <= 0 && form.rewardScreenMinutes <= 0) {
      formError.value = t('tasques.missingReward')
      activeStep.value = 1
      return
    }
  } else if (form.penaltyMoneyAmount <= 0 && form.penaltyScreenMinutes <= 0) {
    // Responsabilitat: la recompensa és opcional, però mai sense penalització — sense
    // penalització no hi ha conseqüència real de no fer la tasca.
    formError.value = t('tasques.missingPenalty')
    activeStep.value = 1
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.patch(`/api/tasks/${editingId.value}`, form)
    } else {
      await api.post('/api/tasks', form)
    }
    formOpen.value = false
    await load()
  } catch (err) {
    formError.value = apiErrorMessage(err)
  } finally {
    saving.value = false
  }
}

async function deactivate(task: TaskManagementResponse) {
  await api.delete(`/api/tasks/${task.id}`)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="tasques-parent">
    <h1>{{ t('nav.tasques') }}</h1>
    <p class="tasques-parent__sub">{{ t('tasques.parentSubtitle') }}</p>

    <div class="tasques-parent__filters">
      <button type="button" :class="{ active: viewMode === 'tasks' && typeFilter === 'ALL' }" @click="viewMode = 'tasks'; typeFilter = 'ALL'">{{ t('tasques.filterAll') }}</button>
      <button type="button" :class="{ active: viewMode === 'tasks' && typeFilter === 'RESPONSIBILITY' }" @click="viewMode = 'tasks'; typeFilter = 'RESPONSIBILITY'">{{ t('tasques.typeResponsibility') }}</button>
      <button type="button" :class="{ active: viewMode === 'tasks' && typeFilter === 'EXTRA' }" @click="viewMode = 'tasks'; typeFilter = 'EXTRA'">{{ t('tasques.typeExtra') }}</button>
      <button type="button" :class="{ active: viewMode === 'incomplete' }" @click="showIncomplete">{{ t('tasques.filterIncomplete') }}</button>
    </div>

    <template v-if="viewMode === 'incomplete'">
      <p v-if="incompleteTasks.length === 0" class="tasques-parent__empty">{{ t('tasques.incompleteEmpty') }}</p>
      <BaseCard v-for="item in incompleteTasks" :key="`${item.taskId}-${item.childId}`" class="task-card">
        <div class="task-card__head">
          <div>
            <div class="task-card__name">{{ item.taskName }}</div>
            <div class="task-card__type">{{ item.childDisplayName }}</div>
          </div>
          <BaseButton
            variant="danger"
            :disabled="!!applyingPenaltyKey"
            @click="applyPenalty(item)"
          >
            {{ applyingPenaltyKey === `${item.taskId}-${item.childId}` ? t('common.saving') : t('tasques.applyPenalty') }}
          </BaseButton>
        </div>
        <div class="task-card__reward">
          <AmountDisplay v-if="item.penaltyMoneyAmount > 0" :value="item.penaltyMoneyAmount" unit="€" />
          <span v-if="item.penaltyScreenMinutes > 0">-{{ item.penaltyScreenMinutes }} {{ t('common.minutesAbbr') }}</span>
        </div>
      </BaseCard>
    </template>

    <template v-else>
      <p v-if="!loading && visibleTasks.length === 0" class="tasques-parent__empty">{{ t('tasques.parentEmpty') }}</p>

      <BaseCard v-for="task in visibleTasks" :key="task.id" class="task-card" :class="{ 'task-card--inactive': !task.active }">
        <div class="task-card__head">
          <div>
            <div class="task-card__name">{{ task.name }}</div>
            <div class="task-card__type">{{ task.taskType === 'RESPONSIBILITY' ? t('tasques.typeResponsibility') : t('tasques.typeExtra') }}</div>
          </div>
          <div class="task-card__actions">
            <BaseButton variant="accent" @click="startEdit(task)">{{ t('fills.edit') }}</BaseButton>
            <BaseButton v-if="task.active" variant="danger" @click="deactivate(task)">{{ t('tasques.deactivate') }}</BaseButton>
          </div>
        </div>
        <div class="task-card__reward">
          <AmountDisplay v-if="task.rewardMoney > 0" :value="task.rewardMoney" unit="€" />
          <span v-if="task.rewardScreenMinutes > 0">+{{ task.rewardScreenMinutes }} {{ t('common.minutesAbbr') }}</span>
        </div>
        <div v-if="task.taskType === 'RESPONSIBILITY' && (task.penaltyMoneyAmount > 0 || task.penaltyScreenMinutes > 0)" class="task-card__penalty">
          {{ t('tasques.penaltyLabel') }}
          <AmountDisplay v-if="task.penaltyMoneyAmount > 0" :value="task.penaltyMoneyAmount" unit="€" />
          <span v-if="task.penaltyScreenMinutes > 0">-{{ task.penaltyScreenMinutes }} {{ t('common.minutesAbbr') }}</span>
        </div>
        <div class="task-card__children">
          {{ task.taskType === 'EXTRA' ? t('tasques.openToAll') : (task.assignedChildren.length ? task.assignedChildren.map((c) => c.displayName).join(', ') : t('tasques.noneAssigned')) }}
        </div>
      </BaseCard>

      <BaseCard v-if="formOpen" class="task-form-card">
        <div class="step-tabs">
          <div class="step-indicator" :style="{ width: `calc(${100 / visibleSteps.length}% - ${8 / visibleSteps.length}px)`, transform: `translateX(${activeStep * 100}%)` }" />
          <button
            v-for="(step, index) in visibleSteps"
            :key="step"
            type="button"
            class="step-tab"
            :class="{ active: activeStep === index }"
            @click="activeStep = index"
          >
            {{ index + 1 }}. {{ t(step) }}
          </button>
        </div>

        <form class="task-form" @submit.prevent="submitForm">
          <div class="step-panel" :class="{ active: activeStep === 0 }">
            <label>
              {{ t('tasques.nameLabel') }}
              <input v-model="form.name" type="text" required autofocus />
            </label>
            <label>
              {{ t('tasques.descriptionLabel') }}
              <textarea v-model="form.description" rows="2" />
            </label>
            <FormRow>
              <label>
                {{ t('tasques.typeLabel') }}
                <select v-model="form.taskType">
                  <option value="RESPONSIBILITY">{{ t('tasques.typeResponsibility') }}</option>
                  <option value="EXTRA">{{ t('tasques.typeExtra') }}</option>
                </select>
              </label>
              <label>
                {{ t('tasques.recurrenceLabel') }}
                <select v-model="form.recurrenceType">
                  <option v-for="r in RECURRENCES" :key="r" :value="r">{{ t(`tasques.recurrence${r}`) }}</option>
                </select>
              </label>
            </FormRow>
          </div>

          <div class="step-panel" :class="{ active: activeStep === 1 }">
            <div class="switch-field">
              <div class="task-form__switch-row">
                <span>{{ t('tasques.rewardMoneyLabel') }}</span>
                <BaseSwitch v-model="moneyRewardOn" />
              </div>
              <input v-model.number="form.rewardMoney" type="number" min="0" step="0.5" :disabled="!moneyRewardOn" />
            </div>
            <div class="switch-field">
              <div class="task-form__switch-row">
                <span>{{ t('tasques.rewardMinutesLabel') }}</span>
                <BaseSwitch v-model="minutesRewardOn" />
              </div>
              <MinutesInput v-model="form.rewardScreenMinutes" :disabled="!minutesRewardOn" />
            </div>
            <div v-if="previewSplit" class="split-preview">
              <span>
                {{ t('tasques.splitPreviewIntro', { spending: previewSplit.spendingPercentage, savings: previewSplit.savingsPercentage }) }}
                <template v-if="previewPercentages.length > 1"> {{ t('tasques.splitPreviewVaries') }}</template>
              </span>
              <span class="split-preview__values">{{ t('tasques.splitPreviewValues', { spend: formatMoney(previewSplit.spend), save: formatMoney(previewSplit.save) }) }}</span>
            </div>
            <p v-if="form.taskType === 'EXTRA'" class="task-form__hint">{{ t('tasques.rewardHint') }}</p>
            <p v-else class="task-form__hint">{{ t('tasques.rewardOptionalHint') }}</p>
            <div class="task-form__switch-row">
              <span>{{ t('tasques.requiresApprovalLabel') }}</span>
              <BaseSwitch v-model="form.requiresApproval" />
            </div>

            <template v-if="form.taskType === 'RESPONSIBILITY'">
              <div class="section-divider">{{ t('tasques.penaltySection') }}</div>
              <div class="switch-field">
                <div class="task-form__switch-row">
                  <span>{{ t('tasques.penaltyMoneyLabel') }}</span>
                  <BaseSwitch v-model="moneyPenaltyOn" />
                </div>
                <input v-model.number="form.penaltyMoneyAmount" type="number" min="0" step="0.5" :disabled="!moneyPenaltyOn" />
              </div>
              <div class="switch-field">
                <div class="task-form__switch-row">
                  <span>{{ t('tasques.penaltyMinutesLabel') }}</span>
                  <BaseSwitch v-model="minutesPenaltyOn" />
                </div>
                <MinutesInput v-model="form.penaltyScreenMinutes" :disabled="!minutesPenaltyOn" />
              </div>
              <p class="task-form__hint">{{ t('tasques.penaltyRequiredHint') }}</p>
              <p class="task-form__hint">{{ t('tasques.penaltyHint') }}</p>
            </template>
          </div>

          <div v-if="form.taskType !== 'EXTRA'" class="step-panel" :class="{ active: activeStep === 2 }">
            <span class="task-form__children-label">{{ t('tasques.assignLabel') }}</span>
            <div class="task-form__children">
              <button
                v-for="child in children"
                :key="child.childId"
                type="button"
                class="task-form__child-chip"
                :class="{ active: form.childIds.includes(child.childId) }"
                @click="toggleChild(child.childId)"
              >
                {{ child.displayName }}
              </button>
            </div>
          </div>

          <p v-if="formError" class="tasques-parent__error">{{ formError }}</p>
          <div class="task-form__actions">
            <BaseButton type="button" variant="ghost" :disabled="saving" @click="formOpen = false">{{ t('common.cancel') }}</BaseButton>
            <BaseButton type="submit" variant="primary" :disabled="saving">
              {{ saving ? t('common.saving') : t('common.save') }}
            </BaseButton>
          </div>
        </form>
      </BaseCard>
      <BaseButton v-else variant="accent" class="tasques-parent__add" @click="startCreate">+ {{ t('tasques.newTask') }}</BaseButton>
    </template>
  </div>
</template>

<style scoped>
.tasques-parent {
  max-width: 680px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.tasques-parent__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1.1rem;
}

.tasques-parent__filters {
  display: inline-flex;
  flex-wrap: wrap;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.2rem;
  margin-bottom: 1.1rem;
}

.tasques-parent__filters button {
  border: none;
  background: transparent;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.78rem;
  color: var(--muted);
  cursor: pointer;
}

.tasques-parent__filters button.active {
  background: var(--primary);
  color: white;
}

.tasques-parent__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.tasques-parent__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.tasques-parent__add {
  margin-top: 0.5rem;
}

.task-card {
  margin-bottom: 0.75rem;
}

.task-card--inactive {
  opacity: 0.55;
}

.task-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.task-card__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.95rem;
}

.task-card__type {
  font-size: 0.72rem;
  color: var(--muted);
  text-transform: uppercase;
  font-weight: 700;
  margin-top: 0.1rem;
}

.task-card__actions {
  display: flex;
  gap: 0.4rem;
  flex-shrink: 0;
}

.task-card__reward {
  display: flex;
  gap: 0.6rem;
  align-items: baseline;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--primary);
  margin-top: 0.5rem;
}

.task-card__penalty {
  display: flex;
  gap: 0.6rem;
  align-items: baseline;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--error);
  margin-top: 0.3rem;
}

.task-card__children {
  font-size: 0.78rem;
  color: var(--muted);
  margin-top: 0.3rem;
}

.task-form-card {
  margin-top: 0.5rem;
}

.step-tabs {
  position: relative;
  display: flex;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 1.1rem;
}

.step-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  height: calc(100% - 8px);
  background: white;
  border-radius: 11px;
  box-shadow: 0 2px 6px rgba(42, 33, 69, 0.12);
  transition: transform 0.3s cubic-bezier(0.3, 0.8, 0.3, 1);
}

.step-tab {
  flex: 1;
  position: relative;
  z-index: 1;
  border: none;
  background: none;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.78rem;
  padding: 0.6rem 0.4rem;
  border-radius: 11px;
  color: var(--muted);
  cursor: pointer;
  text-align: center;
}

.step-tab.active {
  color: var(--primary);
}

.step-panel {
  display: none;
  flex-direction: column;
  gap: 0.85rem;
}

.step-panel.active {
  display: flex;
  animation: step-fade-in 0.25s ease;
}

@keyframes step-fade-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.task-form {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.task-form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.task-form input,
.task-form select,
.task-form textarea {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
  resize: vertical;
}

.task-form input:disabled {
  opacity: 0.5;
  background: color-mix(in srgb, var(--text) 4%, transparent);
}

.switch-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.task-form__switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.section-divider {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.76rem;
  color: var(--error);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin-top: 0.3rem;
  padding-top: 0.6rem;
  border-top: 1px dashed color-mix(in srgb, var(--text) 15%, transparent);
}

.split-preview {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0.3rem 0.75rem;
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  border-radius: 10px;
  padding: 0.6rem 0.75rem;
  font-size: 0.76rem;
  color: var(--primary);
}

.split-preview__values {
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.task-form__hint {
  font-size: 0.72rem;
  color: var(--muted);
  margin: -0.4rem 0 0;
}

.task-form__children-label {
  font-weight: 700;
  font-size: 0.82rem;
}

.task-form__children {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: 0.5rem;
}

.task-form__child-chip {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.8rem;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  border: 2px solid color-mix(in srgb, var(--primary) 15%, transparent);
  background: white;
  cursor: pointer;
  color: var(--text);
}

.task-form__child-chip.active {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, white);
  color: var(--primary);
}

.task-form__actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.3rem;
}
</style>
