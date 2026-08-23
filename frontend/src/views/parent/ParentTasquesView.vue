<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { apiErrorMessage } from '@/utils/apiError'
import type { RecurrenceType, TaskManagementResponse, TaskRequest } from '@/types/parent'
import type { ChildSummary } from '@/types/nfc'
import type { TaskType } from '@/types/child'

const { t } = useI18n()
const auth = useAuthStore()

const tasks = ref<TaskManagementResponse[]>([])
const children = ref<ChildSummary[]>([])
const loading = ref(true)
const typeFilter = ref<'ALL' | TaskType>('ALL')

const editingId = ref<string | null>(null)
const formOpen = ref(false)
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
  rewardSavings: 0,
  rewardScreenMinutes: 0,
  childIds: [],
})

const visibleTasks = computed(() =>
  typeFilter.value === 'ALL' ? tasks.value : tasks.value.filter((task) => task.taskType === typeFilter.value),
)

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const [tasksRes, childrenRes] = await Promise.all([
    api.get<TaskManagementResponse[]>('/api/tasks'),
    api.get<ChildSummary[]>(`/api/families/${familyId}/children`),
  ])
  tasks.value = tasksRes.data
  children.value = childrenRes.data
  loading.value = false
}

function resetForm() {
  Object.assign(form, {
    name: '', description: '', taskType: 'RESPONSIBILITY', icon: null, requiresApproval: true,
    recurrenceType: 'NONE', rewardMoney: 0, rewardSavings: 0, rewardScreenMinutes: 0, childIds: [],
  })
}

function startCreate() {
  editingId.value = null
  resetForm()
  formError.value = null
  formOpen.value = true
}

function startEdit(task: TaskManagementResponse) {
  editingId.value = task.id
  Object.assign(form, {
    name: task.name, description: task.description ?? '', taskType: task.taskType, icon: task.icon,
    requiresApproval: task.requiresApproval, recurrenceType: task.recurrenceType,
    rewardMoney: task.rewardMoney, rewardSavings: task.rewardSavings, rewardScreenMinutes: task.rewardScreenMinutes,
    childIds: task.assignedChildren.map((c) => c.childId),
  })
  formError.value = null
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
    return
  }
  if (form.rewardMoney <= 0 && form.rewardSavings <= 0 && form.rewardScreenMinutes <= 0) {
    formError.value = t('tasques.missingReward')
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
      <button type="button" :class="{ active: typeFilter === 'ALL' }" @click="typeFilter = 'ALL'">{{ t('tasques.filterAll') }}</button>
      <button type="button" :class="{ active: typeFilter === 'RESPONSIBILITY' }" @click="typeFilter = 'RESPONSIBILITY'">{{ t('tasques.typeResponsibility') }}</button>
      <button type="button" :class="{ active: typeFilter === 'EXTRA' }" @click="typeFilter = 'EXTRA'">{{ t('tasques.typeExtra') }}</button>
    </div>

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
        <span v-if="task.rewardSavings > 0">+ <AmountDisplay :value="task.rewardSavings" unit="€ estalvi" /></span>
        <span v-if="task.rewardScreenMinutes > 0">+{{ task.rewardScreenMinutes }} {{ t('common.minutesAbbr') }}</span>
      </div>
      <div class="task-card__children">
        {{ task.assignedChildren.length ? task.assignedChildren.map((c) => c.displayName).join(', ') : t('tasques.noneAssigned') }}
      </div>
    </BaseCard>

    <BaseCard v-if="formOpen" class="task-form-card">
      <form class="task-form" @submit.prevent="submitForm">
        <label>
          {{ t('tasques.nameLabel') }}
          <input v-model="form.name" type="text" required autofocus />
        </label>
        <label>
          {{ t('tasques.descriptionLabel') }}
          <textarea v-model="form.description" rows="2" />
        </label>
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
        <label class="task-form__checkbox">
          <input v-model="form.requiresApproval" type="checkbox" />
          {{ t('tasques.requiresApprovalLabel') }}
        </label>
        <div class="task-form__reward-grid">
          <label>
            {{ t('tasques.rewardMoneyLabel') }}
            <input v-model.number="form.rewardMoney" type="number" min="0" step="0.5" />
          </label>
          <label>
            {{ t('tasques.rewardSavingsLabel') }}
            <input v-model.number="form.rewardSavings" type="number" min="0" step="0.5" />
          </label>
          <label>
            {{ t('tasques.rewardMinutesLabel') }}
            <input v-model.number="form.rewardScreenMinutes" type="number" min="0" step="5" />
          </label>
        </div>
        <div class="task-form__children">
          <span class="task-form__children-label">{{ t('tasques.assignLabel') }}</span>
          <button
            v-for="child in children"
            :key="child.id"
            type="button"
            class="task-form__child-chip"
            :class="{ active: form.childIds.includes(child.id) }"
            @click="toggleChild(child.id)"
          >
            {{ child.displayName }}
          </button>
        </div>
        <p v-if="formError" class="tasques-parent__error">{{ formError }}</p>
        <div class="task-form__actions">
          <BaseButton type="submit" variant="primary" :disabled="saving">
            {{ saving ? t('common.saving') : t('common.save') }}
          </BaseButton>
          <BaseButton type="button" variant="danger" :disabled="saving" @click="formOpen = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="tasques-parent__add" @click="startCreate">+ {{ t('tasques.newTask') }}</BaseButton>
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

.task-card__children {
  font-size: 0.78rem;
  color: var(--muted);
  margin-top: 0.3rem;
}

.task-form-card {
  margin-top: 0.5rem;
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

.task-form__checkbox {
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
}

.task-form__reward-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.6rem;
}

.task-form__children {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  align-items: center;
}

.task-form__children-label {
  font-weight: 700;
  font-size: 0.82rem;
  width: 100%;
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
}
</style>
