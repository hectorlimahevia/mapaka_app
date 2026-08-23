<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { apiErrorMessage } from '@/utils/apiError'
import type { SavingsGoalResponse } from '@/types/child'

const { t } = useI18n()
const auth = useAuthStore()
const goals = ref<SavingsGoalResponse[]>([])
const loading = ref(true)
const animated = ref(false)

const addingGoal = ref(false)
const savingGoal = ref(false)
const addGoalError = ref<string | null>(null)
const newGoal = reactive({ name: '', targetAmount: 0 })

function percentOf(goal: SavingsGoalResponse) {
  if (goal.targetAmount <= 0) return 0
  return Math.min(100, (goal.currentAmount / goal.targetAmount) * 100)
}

async function load() {
  const childId = auth.childId
  if (!childId) return
  const { data } = await api.get<SavingsGoalResponse[]>(`/api/children/${childId}/savings-goals`)
  goals.value = data
  loading.value = false
  requestAnimationFrame(() => requestAnimationFrame(() => (animated.value = true)))
}

function startAddGoal() {
  addingGoal.value = true
  addGoalError.value = null
  newGoal.name = ''
  newGoal.targetAmount = 0
}

async function submitAddGoal() {
  addGoalError.value = null
  const childId = auth.childId
  if (!childId || !newGoal.name.trim() || newGoal.targetAmount <= 0) {
    addGoalError.value = t('objectius.missingFields')
    return
  }
  savingGoal.value = true
  try {
    await api.post(`/api/children/${childId}/savings-goals`, {
      name: newGoal.name,
      targetAmount: newGoal.targetAmount,
    })
    addingGoal.value = false
    await load()
  } catch (err) {
    addGoalError.value = apiErrorMessage(err)
  } finally {
    savingGoal.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="objectius">
    <h1>{{ t('objectius.title') }}</h1>
    <p class="objectius__sub">{{ t('objectius.subtitle') }}</p>

    <p v-if="!loading && goals.length === 0" class="objectius__empty">{{ t('objectius.empty') }}</p>

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

    <BaseCard v-if="addingGoal" class="goal-form-card">
      <form class="goal-form" @submit.prevent="submitAddGoal">
        <label>
          {{ t('objectius.nameLabel') }}
          <input v-model="newGoal.name" type="text" required autofocus />
        </label>
        <label>
          {{ t('objectius.targetLabel') }}
          <input v-model.number="newGoal.targetAmount" type="number" min="0.01" step="0.01" required />
        </label>
        <p v-if="addGoalError" class="objectius__error">{{ addGoalError }}</p>
        <div class="goal-form__actions">
          <BaseButton type="submit" variant="primary" :disabled="savingGoal">
            {{ savingGoal ? t('objectius.adding') : t('objectius.addGoal') }}
          </BaseButton>
          <BaseButton type="button" variant="danger" :disabled="savingGoal" @click="addingGoal = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="objectius__add" @click="startAddGoal">+ {{ t('objectius.addGoal') }}</BaseButton>
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

.objectius__add {
  margin-top: 0.5rem;
}

.objectius__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.goal-form-card {
  margin-top: 0.5rem;
}

.goal-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.goal-form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.goal-form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.goal-form__actions {
  display: flex;
  gap: 0.5rem;
}
</style>
