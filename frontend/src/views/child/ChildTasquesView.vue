<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import type { ChildTaskResponse } from '@/types/child'

const auth = useAuthStore()
const tasks = ref<ChildTaskResponse[]>([])
const loading = ref(true)
const completingId = ref<string | null>(null)

const statusLabel: Record<ChildTaskResponse['status'], string> = {
  AVAILABLE: '',
  PENDING: "Pendent d'aprovació",
  APPROVED: 'Aprovada',
  REJECTED: 'Rebutjada',
}

async function loadTasks() {
  const childId = auth.childId
  if (!childId) return
  const { data } = await api.get<ChildTaskResponse[]>(`/api/children/${childId}/tasks`)
  tasks.value = data
  loading.value = false
}

async function markDone(task: ChildTaskResponse) {
  if (task.status !== 'AVAILABLE' || completingId.value) return
  completingId.value = task.id
  try {
    await api.post(`/api/tasks/${task.id}/complete`)
    await loadTasks()
  } finally {
    completingId.value = null
  }
}

onMounted(loadTasks)
</script>

<template>
  <div class="tasques">
    <h1>Les teves tasques</h1>
    <p class="tasques__sub">Toca el cercle per marcar-la com feta</p>

    <p v-if="!loading && tasks.length === 0" class="tasques__empty">Encara no tens cap tasca assignada.</p>

    <div
      v-for="task in tasks"
      :key="task.id"
      class="task-row"
      :class="`task-row--${task.status.toLowerCase()}`"
      @click="markDone(task)"
    >
      <div class="task-row__check">
        <span v-if="task.status !== 'AVAILABLE'">✓</span>
      </div>
      <div class="task-row__body">
        <div class="task-row__title" :class="{ 'task-row__title--done': task.status !== 'AVAILABLE' }">
          {{ task.name }}
        </div>
        <div class="task-row__reward">
          Recompensa:
          <AmountDisplay v-if="task.rewardMoney > 0" :value="task.rewardMoney" unit="€" />
          <span v-if="task.rewardMoney > 0 && task.rewardScreenMinutes > 0"> · </span>
          <span v-if="task.rewardScreenMinutes > 0">+{{ task.rewardScreenMinutes }} min</span>
        </div>
      </div>
      <div class="task-row__status">{{ statusLabel[task.status] }}</div>
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

.task-row--rejected .task-row__status {
  color: var(--muted);
}
</style>
