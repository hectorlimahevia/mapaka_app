<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import ChildScreenHeader from '@/components/base/ChildScreenHeader.vue'
import { apiErrorMessage } from '@/utils/apiError'
import { AVATAR_ICON_PATHS, AVATAR_ICON_VIEWBOX } from '@/utils/avatarIcons'
import { CHILD_COLORS } from '@/utils/childColors'
import { formatDate } from '@/utils/date'
import type { AppLocale } from '@/i18n'
import type { SavingsGoalResponse, WalletResponse } from '@/types/child'

const { t, locale } = useI18n()
const auth = useAuthStore()
const goals = ref<SavingsGoalResponse[]>([])
const spendingBalance = ref(0)
const spendingPercentage = ref(0)
const allocatedGoalPercentage = ref(0)
const loading = ref(true)
const animated = ref(false)

const editingGoalId = ref<string | null>(null)
const addingGoal = ref(false)
const savingGoal = ref(false)
const addGoalError = ref<string | null>(null)
const newGoal = reactive({ name: '', targetAmount: 0, allocationPercentage: 0 })

const contributingGoalId = ref<string | null>(null)
const contributionAmount = ref(0)
const contributionError = ref<string | null>(null)
const savingContribution = ref(false)

// Confeti + vora daurada quan el fill arriba a un objectiu amb la seva pròpia aportació
// (ajust posterior) — cada peça calcula el seu propi angle/distància un cop, en disparar
// la celebració, perquè totes les repeticions no quedin idèntiques.
const celebratingGoalId = ref<string | null>(null)
const confettiPieces = ref<{ dx: string; dy: string; rot: string; color: string; delay: string }[]>([])
let celebrationTimeout: ReturnType<typeof setTimeout> | undefined

function triggerCelebration(goalId: string) {
  clearTimeout(celebrationTimeout)
  confettiPieces.value = Array.from({ length: 18 }, (_, i) => {
    const angle = Math.random() * Math.PI * 2
    const dist = 60 + Math.random() * 70
    return {
      dx: `${Math.cos(angle) * dist}px`,
      dy: `${Math.sin(angle) * dist - 20}px`,
      rot: `${Math.random() * 360}deg`,
      color: CHILD_COLORS[i % CHILD_COLORS.length]!,
      delay: `${Math.random() * 0.12}s`,
    }
  })
  celebratingGoalId.value = goalId
  celebrationTimeout = setTimeout(() => {
    celebratingGoalId.value = null
  }, 1600)
}

// Un objectiu COMPLETED es queda a la llista activa mentre dura la seva pròpia celebració
// (perquè el confeti i la vora daurada es vegin al mateix lloc on s'ha completat) i només
// "cau" a l'historial quan `celebratingGoalId` es buida sol als 1.6s (ajust posterior).
const activeGoals = computed(() =>
  goals.value.filter((g) => g.status === 'ACTIVE' || g.id === celebratingGoalId.value),
)
const completedGoals = computed(() =>
  goals.value
    .filter((g) => g.status === 'COMPLETED' && g.id !== celebratingGoalId.value)
    .sort((a, b) => new Date(b.completedAt ?? 0).getTime() - new Date(a.completedAt ?? 0).getTime()),
)

const deletingGoalId = ref<string | null>(null)
const deletingInProgress = ref(false)
const deleteError = ref<string | null>(null)

function startDelete(goalId: string) {
  deletingGoalId.value = goalId
  deleteError.value = null
}

function cancelDelete() {
  deletingGoalId.value = null
}

async function confirmDelete(goalId: string) {
  const childId = auth.childId
  if (!childId) return
  deleteError.value = null
  deletingInProgress.value = true
  try {
    await api.delete(`/api/children/${childId}/savings-goals/${goalId}`)
    deletingGoalId.value = null
    await load()
  } catch (err) {
    deleteError.value = apiErrorMessage(err)
  } finally {
    deletingInProgress.value = false
  }
}

function daysToComplete(goal: SavingsGoalResponse) {
  if (!goal.completedAt) return 0
  const start = new Date(goal.createdAt).getTime()
  const end = new Date(goal.completedAt).getTime()
  return Math.max(1, Math.round((end - start) / (1000 * 60 * 60 * 24)))
}

function completedDateLabel(goal: SavingsGoalResponse) {
  if (!goal.completedAt) return ''
  return formatDate(goal.completedAt, locale.value as AppLocale)
}

// Marge disponible = percentatge de gastar vigent menys el que ja ocupen ELS ALTRES
// objectius actius (exclou el que s'està editant, perquè pugui conservar el seu propi
// tram) — mateixa regla que valida el backend (Prompt 15, secció 8.1).
const othersAllocatedPercentage = computed(() => {
  if (!editingGoalId.value) return allocatedGoalPercentage.value
  const editing = goals.value.find((g) => g.id === editingGoalId.value)
  return allocatedGoalPercentage.value - (editing?.allocationPercentage ?? 0)
})
const availableMargin = computed(() => Math.max(0, spendingPercentage.value - othersAllocatedPercentage.value))
const effectiveSpendAfterGoal = computed(() =>
  Math.max(0, spendingPercentage.value - othersAllocatedPercentage.value - newGoal.allocationPercentage),
)
const percentageExceedsAvailable = computed(() => newGoal.allocationPercentage > availableMargin.value)

function percentOf(goal: SavingsGoalResponse) {
  if (goal.targetAmount <= 0) return 0
  return Math.min(100, (goal.currentAmount / goal.targetAmount) * 100)
}

async function load() {
  const childId = auth.childId
  if (!childId) return
  const [goalsRes, walletRes] = await Promise.all([
    api.get<SavingsGoalResponse[]>(`/api/children/${childId}/savings-goals`),
    api.get<WalletResponse>(`/api/children/${childId}/wallet`),
  ])
  goals.value = goalsRes.data
  spendingBalance.value = walletRes.data.spendingBalance
  spendingPercentage.value = walletRes.data.spendingPercentage
  allocatedGoalPercentage.value = walletRes.data.allocatedGoalPercentage
  loading.value = false
  requestAnimationFrame(() => requestAnimationFrame(() => (animated.value = true)))
}

function startContribute(goal: SavingsGoalResponse) {
  contributingGoalId.value = goal.id
  contributionAmount.value = 0
  contributionError.value = null
}

async function submitContribution(goalId: string) {
  contributionError.value = null
  const childId = auth.childId
  if (!childId || contributionAmount.value <= 0) {
    contributionError.value = t('objectius.missingContributionAmount')
    return
  }
  if (contributionAmount.value > spendingBalance.value) {
    contributionError.value = t('errors.CONTRIBUTION_EXCEEDS_SPENDING_BALANCE')
    return
  }
  savingContribution.value = true
  try {
    await api.post(`/api/children/${childId}/savings-goals/${goalId}/contributions`, {
      amount: contributionAmount.value,
    })
    contributingGoalId.value = null
    await load()
    if (goals.value.find((g) => g.id === goalId)?.status === 'COMPLETED') {
      triggerCelebration(goalId)
    }
  } catch (err) {
    contributionError.value = apiErrorMessage(err)
  } finally {
    savingContribution.value = false
  }
}

function startAddGoal() {
  editingGoalId.value = null
  addingGoal.value = true
  addGoalError.value = null
  newGoal.name = ''
  newGoal.targetAmount = 0
  newGoal.allocationPercentage = 0
}

function startEditGoal(goal: SavingsGoalResponse) {
  editingGoalId.value = goal.id
  addingGoal.value = true
  addGoalError.value = null
  newGoal.name = goal.name
  newGoal.targetAmount = goal.targetAmount
  newGoal.allocationPercentage = goal.allocationPercentage
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
    const payload = {
      name: newGoal.name,
      targetAmount: newGoal.targetAmount,
      allocationPercentage: newGoal.allocationPercentage,
    }
    if (editingGoalId.value) {
      await api.patch(`/api/children/${childId}/savings-goals/${editingGoalId.value}`, payload)
    } else {
      await api.post(`/api/children/${childId}/savings-goals`, payload)
    }
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
    <ChildScreenHeader :title="t('objectius.title')" />
    <p class="objectius__sub">{{ t('objectius.subtitle') }}</p>

    <p v-if="!loading && activeGoals.length === 0 && completedGoals.length === 0" class="objectius__empty">{{ t('objectius.empty') }}</p>

    <div v-for="goal in activeGoals" :key="goal.id" class="goal-card" :class="{ celebrate: celebratingGoalId === goal.id }">
      <template v-if="celebratingGoalId === goal.id">
        <span
          v-for="(piece, i) in confettiPieces"
          :key="i"
          class="confetti-piece"
          :style="{ '--dx': piece.dx, '--dy': piece.dy, '--rot': piece.rot, background: piece.color, animationDelay: piece.delay }"
        />
      </template>
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
      <p v-if="celebratingGoalId === goal.id" class="goal-card__celebrate-status">{{ t('objectius.goalCompletedStatus') }}</p>

      <div v-if="goal.status === 'ACTIVE' && deletingGoalId !== goal.id" class="goal-card__actions">
        <button type="button" class="goal-card__edit" @click="startEditGoal(goal)">{{ t('objectius.edit') }}</button>
        <button type="button" class="goal-card__contribute" @click="startContribute(goal)">{{ t('objectius.contributeButton') }}</button>
        <button type="button" class="goal-card__delete" @click="startDelete(goal.id)">{{ t('objectius.deleteButton') }}</button>
      </div>

      <Transition name="confirm-pop">
        <div v-if="deletingGoalId === goal.id" class="goal-card__confirm">
          <p>{{ t('objectius.deleteConfirm', { name: goal.name }) }}</p>
          <p v-if="deleteError" class="objectius__error">{{ deleteError }}</p>
          <div class="goal-form__actions">
            <BaseButton type="button" variant="danger" :disabled="deletingInProgress" @click="confirmDelete(goal.id)">
              {{ deletingInProgress ? t('common.saving') : t('objectius.deleteConfirmYes') }}
            </BaseButton>
            <BaseButton type="button" variant="ghost" :disabled="deletingInProgress" @click="cancelDelete">
              {{ t('common.cancel') }}
            </BaseButton>
          </div>
        </div>
      </Transition>

      <form
        v-if="contributingGoalId === goal.id"
        class="contribute-form"
        @submit.prevent="submitContribution(goal.id)"
      >
        <label>
          {{ t('objectius.contributeAmountLabel') }}
          <input v-model.number="contributionAmount" type="number" min="0.01" step="0.01" required autofocus />
        </label>
        <p class="contribute-form__hint">
          {{ t('objectius.contributeAvailable', { n: spendingBalance }) }}
        </p>
        <p v-if="contributionError" class="objectius__error">{{ contributionError }}</p>
        <div class="goal-form__actions">
          <BaseButton type="submit" variant="primary" :disabled="savingContribution">
            {{ savingContribution ? t('common.saving') : t('objectius.contributeSubmit') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingContribution" @click="contributingGoalId = null">
            {{ t('common.cancel') }}
          </BaseButton>
        </div>
      </form>
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
        <label>
          {{ t('objectius.percentageLabel') }}
          <input v-model.number="newGoal.allocationPercentage" type="number" min="0" max="100" step="1" />
        </label>
        <div class="split-preview">
          <div class="split-preview__row">
            <span>{{ t('objectius.splitSpendLabel', { n: spendingPercentage }) }}</span>
            <b>{{ effectiveSpendAfterGoal }}%</b>
          </div>
          <div class="split-preview__row">
            <span>{{ t('objectius.splitSaveLabel') }}</span>
            <b>{{ 100 - spendingPercentage }}%</b>
          </div>
          <div class="split-preview__row">
            <span>{{ t('objectius.splitGoalLabel') }}</span>
            <b class="split-preview__goal">{{ newGoal.allocationPercentage }}%</b>
          </div>
        </div>
        <p v-if="percentageExceedsAvailable" class="objectius__warning">{{ t('objectius.percentageWarning') }}</p>
        <p v-if="addGoalError" class="objectius__error">{{ addGoalError }}</p>
        <div class="goal-form__actions">
          <BaseButton type="submit" variant="primary" :disabled="savingGoal">
            {{ savingGoal ? t('objectius.adding') : editingGoalId ? t('common.save') : t('objectius.addGoal') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingGoal" @click="addingGoal = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="objectius__add" @click="startAddGoal">+ {{ t('objectius.addGoal') }}</BaseButton>

    <div v-if="completedGoals.length > 0" class="history">
      <h2 class="history__title">{{ t('objectius.historyTitle') }}</h2>
      <div v-for="goal in completedGoals" :key="goal.id" class="history-card">
        <div class="history-card__top">
          <span class="history-card__icon">
            <svg :viewBox="AVATAR_ICON_VIEWBOX" fill="currentColor"><path :d="AVATAR_ICON_PATHS.trophy" /></svg>
          </span>
          <span class="history-card__name">{{ goal.name }}</span>
          <span class="history-card__amt"><AmountDisplay :value="goal.targetAmount" unit="€" :decimals="0" /></span>
        </div>

        <div v-if="deletingGoalId !== goal.id" class="history-card__bottom">
          <span class="history-card__meta">
            {{ t('objectius.historyCompletedOn', { date: completedDateLabel(goal) }) }} ·
            {{ daysToComplete(goal) === 1 ? t('objectius.historyDurationOne') : t('objectius.historyDurationMany', { n: daysToComplete(goal) }) }}
          </span>
          <button type="button" class="goal-card__delete" @click="startDelete(goal.id)">{{ t('objectius.deleteButton') }}</button>
        </div>
        <Transition name="confirm-pop">
          <div v-if="deletingGoalId === goal.id" class="goal-card__confirm">
            <p>{{ t('objectius.deleteConfirm', { name: goal.name }) }}</p>
            <p v-if="deleteError" class="objectius__error">{{ deleteError }}</p>
            <div class="goal-form__actions">
              <BaseButton type="button" variant="danger" :disabled="deletingInProgress" @click="confirmDelete(goal.id)">
                {{ deletingInProgress ? t('common.saving') : t('objectius.deleteConfirmYes') }}
              </BaseButton>
              <BaseButton type="button" variant="ghost" :disabled="deletingInProgress" @click="cancelDelete">
                {{ t('common.cancel') }}
              </BaseButton>
            </div>
          </div>
        </Transition>
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
  position: relative;
  background: white;
  border-radius: 16px;
  padding: 1rem 1.1rem;
  margin-bottom: 0.85rem;
  box-shadow: 0 2px 8px -2px color-mix(in srgb, var(--text) 12%, transparent);
}

.goal-card.celebrate {
  animation: goal-card-glow 1.4s ease;
}

@keyframes goal-card-glow {
  0%,
  100% {
    box-shadow: 0 2px 8px -2px color-mix(in srgb, var(--text) 12%, transparent);
  }
  30%,
  70% {
    box-shadow:
      0 0 0 3px var(--accent),
      0 8px 24px -6px color-mix(in srgb, var(--accent) 60%, transparent);
  }
}

.confetti-piece {
  position: absolute;
  top: 40%;
  left: 50%;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  opacity: 0;
  pointer-events: none;
  animation: confetti-burst 1.1s cubic-bezier(0.2, 0.7, 0.4, 1) forwards;
}

@keyframes confetti-burst {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) rotate(0deg) scale(0.6);
  }
  100% {
    opacity: 0;
    transform: translate(calc(-50% + var(--dx)), calc(-50% + var(--dy))) rotate(var(--rot)) scale(1);
  }
}

.goal-card__celebrate-status {
  font-size: 0.72rem;
  color: var(--success);
  font-weight: 700;
  margin: 0.35rem 0 0;
}

@media (prefers-reduced-motion: reduce) {
  .goal-card.celebrate {
    animation: none;
  }
  .confetti-piece {
    display: none;
  }
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

.goal-card__actions {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
}

.goal-card__edit,
.goal-card__contribute {
  border: none;
  background: none;
  padding: 0;
  color: var(--primary);
  font-weight: 700;
  font-size: 0.76rem;
  cursor: pointer;
  transition: color 0.15s ease;
}

.goal-card__edit:hover,
.goal-card__contribute:hover {
  color: color-mix(in srgb, var(--primary) 75%, var(--text));
}

.goal-card__delete {
  border: none;
  background: none;
  padding: 0;
  margin-left: auto;
  color: var(--error);
  font-weight: 700;
  font-size: 0.76rem;
  cursor: pointer;
  transition: color 0.15s ease;
}

.goal-card__delete:hover {
  color: color-mix(in srgb, var(--error) 75%, var(--text));
}

@media (prefers-reduced-motion: reduce) {
  .goal-card__edit,
  .goal-card__contribute,
  .goal-card__delete {
    transition: none;
  }
}

.goal-card__confirm {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.6rem;
  padding-top: 0.6rem;
  border-top: 1px dashed color-mix(in srgb, var(--error) 25%, transparent);
}

.confirm-pop-enter-active,
.confirm-pop-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.confirm-pop-enter-from,
.confirm-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (prefers-reduced-motion: reduce) {
  .confirm-pop-enter-active,
  .confirm-pop-leave-active {
    transition: none;
  }
}

.goal-card__confirm p {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 600;
}

.contribute-form {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px dashed color-mix(in srgb, var(--primary) 20%, transparent);
}

.contribute-form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.contribute-form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.contribute-form__hint {
  font-size: 0.7rem;
  color: var(--muted);
  margin: 0;
}

.split-preview {
  background: color-mix(in srgb, var(--primary) 6%, transparent);
  border-radius: 12px;
  padding: 0.65rem 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.split-preview__row {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
}

.split-preview__goal {
  color: var(--primary);
}

.objectius__warning {
  color: var(--warning);
  font-size: 0.8rem;
  font-weight: 700;
  margin: 0;
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

.history {
  margin-top: 1.75rem;
}

.history__title {
  font-family: var(--font-heading);
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0 0 0.75rem;
}

.history-card {
  background: color-mix(in srgb, var(--accent) 10%, white);
  border: 1px solid color-mix(in srgb, var(--accent) 35%, transparent);
  border-radius: 16px;
  padding: 0.85rem 1.1rem;
  margin-bottom: 0.7rem;
}

.history-card__top {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.history-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent) 22%, transparent);
  color: color-mix(in srgb, var(--accent) 70%, var(--text));
  flex-shrink: 0;
}

.history-card__icon svg {
  width: 18px;
  height: 18px;
}

.history-card__name {
  flex: 1;
  min-width: 0;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-card__amt {
  font-weight: 700;
  font-size: 0.85rem;
  color: color-mix(in srgb, var(--accent) 65%, var(--text));
  flex-shrink: 0;
}

/* El padding-left alinea aquesta fila amb el títol, no amb la icona — perquè
   les "dades" (data/durada) i el link d'eliminar quedin sota el text, no
   flotant soles a la dreta (ajust posterior, feedback de disseny). */
.history-card__bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.7rem;
  margin-top: 0.35rem;
  padding-left: calc(34px + 0.7rem);
}

.history-card__meta {
  font-size: 0.72rem;
  color: var(--muted);
}
</style>
