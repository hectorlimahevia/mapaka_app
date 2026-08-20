<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import type { SavingsGoalResponse } from '@/types/child'

const auth = useAuthStore()
const goals = ref<SavingsGoalResponse[]>([])
const loading = ref(true)
const animated = ref(false)

function percentOf(goal: SavingsGoalResponse) {
  if (goal.targetAmount <= 0) return 0
  return Math.min(100, (goal.currentAmount / goal.targetAmount) * 100)
}

onMounted(async () => {
  const childId = auth.childId
  if (!childId) return
  const { data } = await api.get<SavingsGoalResponse[]>(`/api/children/${childId}/savings-goals`)
  goals.value = data
  loading.value = false

  requestAnimationFrame(() => requestAnimationFrame(() => (animated.value = true)))
})
</script>

<template>
  <div class="objectius">
    <h1>Objectius d'estalvi</h1>
    <p class="objectius__sub">El que estalvies avança el teu objectiu</p>

    <p v-if="!loading && goals.length === 0" class="objectius__empty">Encara no tens cap objectiu d'estalvi.</p>

    <div v-for="goal in goals" :key="goal.id" class="goal-card">
      <div class="goal-card__top">
        <span class="goal-card__name">{{ goal.name }}</span>
        <span class="goal-card__amt">
          <AmountDisplay :value="goal.currentAmount" :decimals="0" /> /
          <AmountDisplay :value="goal.targetAmount" unit="€" :decimals="0" />
        </span>
      </div>
      <div class="goal-card__bar-bg">
        <div class="goal-card__bar-fill" :style="{ width: (animated ? percentOf(goal) : 0) + '%' }" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.objectius {
  max-width: 480px;
  margin: 0 auto;
  padding: 1.5rem 1.25rem 2rem;
}

.objectius__sub {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0 0 1.1rem;
}

.objectius__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.goal-card {
  background: white;
  border-radius: 16px;
  padding: 1rem 1.1rem;
  margin-bottom: 0.85rem;
  box-shadow: 0 2px 8px -2px color-mix(in srgb, var(--text) 12%, transparent);
}

.goal-card__top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.5rem;
}

.goal-card__name {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.92rem;
}

.goal-card__amt {
  font-size: 0.78rem;
  color: var(--muted);
}

.goal-card__bar-bg {
  height: 10px;
  background: color-mix(in srgb, var(--primary) 12%, transparent);
  border-radius: 999px;
  overflow: hidden;
}

.goal-card__bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--secondary));
  border-radius: 999px;
  transition: width 1.1s cubic-bezier(0.2, 0.8, 0.2, 1);
}
</style>
