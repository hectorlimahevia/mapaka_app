<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseSwitch from '@/components/base/BaseSwitch.vue'
import BirthDateInput from '@/components/base/BirthDateInput.vue'
import MinutesInput from '@/components/base/MinutesInput.vue'
import { AVATAR_ICON_PATHS, AVATAR_ICON_VIEWBOX } from '@/utils/avatarIcons'
import { CHILD_COLORS } from '@/utils/childColors'
import { apiErrorMessage } from '@/utils/apiError'
import { formatMoney } from '@/utils/money'
import { i18n } from '@/i18n'
import type {
  AdjustmentDestination,
  ChildDetailResponse,
  MoneyAdjustmentRequest,
  MoneySplitPreview,
  MoneySplitPreviewPart,
} from '@/types/parent'

function avatarIconPath(child: ChildDetailResponse) {
  return child.avatarIcon ? AVATAR_ICON_PATHS[child.avatarIcon] : null
}

const { t } = useI18n()
const auth = useAuthStore()
const children = ref<ChildDetailResponse[]>([])
const loading = ref(true)
const editingId = ref<string | null>(null)
const saving = ref(false)

const form = reactive({ customAllowance: false, monthlyAmount: 0, spendingPercentage: 70, baseMinutes: 0 })

const adjustingId = ref<string | null>(null)
const savingAdjustment = ref(false)
const adjustmentError = ref<string | null>(null)
const adjustmentNotice = ref<{ childId: string; text: string } | null>(null)
let adjustmentNoticeTimer: ReturnType<typeof setTimeout> | undefined
const adjustment = reactive({
  type: 'BONUS' as 'BONUS' | 'PENALTY',
  category: 'MONEY' as 'MONEY' | 'SCREEN_TIME',
  value: 0,
  reason: '',
  destination: 'RULE' as AdjustmentDestination,
  spendingShare: 50,
})

const adjustmentTypeOptions = [
  { value: 'BONUS', label: 'fills.adjustmentBonus' },
  { value: 'PENALTY', label: 'fills.adjustmentPenalty' },
] as const

const adjustmentCategoryOptions = [
  { value: 'MONEY', label: 'fills.adjustmentCategoryMoney' },
  { value: 'SCREEN_TIME', label: 'fills.adjustmentCategoryScreenTime' },
] as const

const destinationOptions = [
  { value: 'RULE', name: 'fills.destRule', hint: 'fills.destRuleHint' },
  { value: 'SPENDING', name: 'fills.destSpending', hint: 'fills.destSpendingHint' },
  { value: 'SAVINGS', name: 'fills.destSavings', hint: 'fills.destSavingsHint' },
  { value: 'CUSTOM', name: 'fills.destCustom', hint: 'fills.destCustomHint' },
] as const

// Només una bonificació en diners tria destí: la penalització i el temps de pantalla
// segueixen el seu camí de sempre.
const isMoneyBonus = computed(() => adjustment.type === 'BONUS' && adjustment.category === 'MONEY')

const round2 = (n: number) => Math.round(n * 100) / 100

// Repartiment "com sempre": el calcula el backend (mateix càlcul que en desar), perquè la
// vista prèvia no es pugui desviar de la regla ni dels objectius vigents del fill.
const rulePreview = ref<MoneySplitPreviewPart[]>([])

const previewParts = computed<MoneySplitPreviewPart[]>(() => {
  const amount = Number(adjustment.value)
  if (!isMoneyBonus.value || !(amount > 0)) return []
  const direct = (wallet: 'SPENDING' | 'SAVINGS', value: number): MoneySplitPreviewPart => ({ wallet, goalName: null, amount: value })
  let parts: MoneySplitPreviewPart[]
  if (adjustment.destination === 'SPENDING') {
    parts = [direct('SPENDING', amount)]
  } else if (adjustment.destination === 'SAVINGS') {
    parts = [direct('SAVINGS', amount)]
  } else if (adjustment.destination === 'CUSTOM') {
    const spending = round2((amount * adjustment.spendingShare) / 100)
    parts = [direct('SPENDING', spending), direct('SAVINGS', round2(amount - spending))]
  } else {
    parts = rulePreview.value
  }
  return parts.filter((part) => part.amount > 0)
})

function partLabel(part: MoneySplitPreviewPart) {
  if (part.wallet === 'GOAL') return part.goalName ?? ''
  return part.wallet === 'SPENDING' ? t('resum.statSpending') : t('resum.statSavings')
}

let previewTimer: ReturnType<typeof setTimeout> | undefined
let previewRequest = 0
watch(
  () => [adjustingId.value, isMoneyBonus.value, adjustment.destination, Number(adjustment.value)] as const,
  ([childId, moneyBonus, destination, amount]) => {
    clearTimeout(previewTimer)
    previewRequest++
    if (!childId || !moneyBonus || destination !== 'RULE' || !(amount > 0)) {
      rulePreview.value = []
      return
    }
    const request = previewRequest
    previewTimer = setTimeout(async () => {
      try {
        const { data } = await api.get<MoneySplitPreview>(`/api/children/${childId}/money-adjustments/preview`, { params: { amount } })
        if (request === previewRequest) rulePreview.value = data.parts
      } catch {
        if (request === previewRequest) rulePreview.value = []
      }
    }, 250)
  },
)

function startAdjustment(child: ChildDetailResponse) {
  adjustingId.value = child.childId
  adjustmentError.value = null
  adjustmentNotice.value = null
  rulePreview.value = []
  Object.assign(adjustment, { type: 'BONUS', category: 'MONEY', value: 0, reason: '', destination: 'RULE', spendingShare: 50 })
}

async function submitAdjustment(childId: string) {
  adjustmentError.value = null
  if (!adjustment.reason.trim()) {
    adjustmentError.value = t('fills.missingReason')
    return
  }
  if (adjustment.value <= 0) {
    adjustmentError.value = t('fills.missingAdjustmentValue')
    return
  }
  savingAdjustment.value = true
  try {
    let noticeText = t('fills.adjustmentSaved')
    if (adjustment.category === 'MONEY') {
      const payload: MoneyAdjustmentRequest = { type: adjustment.type, amount: adjustment.value, reason: adjustment.reason }
      if (isMoneyBonus.value) {
        payload.destination = adjustment.destination
        if (adjustment.destination === 'CUSTOM') {
          payload.spendingAmount = round2((adjustment.value * adjustment.spendingShare) / 100)
        }
        if (previewParts.value.length) {
          const parts = previewParts.value.map((part) => `${partLabel(part)} +${formatMoney(part.amount)} €`).join(' · ')
          noticeText = t('fills.adjustmentSavedBonus', { parts })
        }
      }
      await api.post(`/api/children/${childId}/money-adjustments`, payload)
    } else {
      await api.post(`/api/children/${childId}/screen-time/adjustments`, {
        type: adjustment.type, minutes: adjustment.value, reason: adjustment.reason,
      })
    }
    adjustingId.value = null
    adjustmentNotice.value = { childId, text: noticeText }
    clearTimeout(adjustmentNoticeTimer)
    adjustmentNoticeTimer = setTimeout(() => {
      adjustmentNotice.value = null
    }, 6000)
  } catch (err) {
    adjustmentError.value = apiErrorMessage(err)
  } finally {
    savingAdjustment.value = false
  }
}

const expensingId = ref<string | null>(null)
const savingExpense = ref(false)
const expenseError = ref<string | null>(null)
const expense = reactive({ amount: 0, reason: '' })

function startExpense(child: ChildDetailResponse) {
  expensingId.value = child.childId
  expenseError.value = null
  Object.assign(expense, { amount: 0, reason: '' })
}

async function submitExpense(childId: string) {
  expenseError.value = null
  if (expense.amount <= 0 || !expense.reason.trim()) {
    expenseError.value = t('fills.missingExpenseFields')
    return
  }
  savingExpense.value = true
  try {
    await api.post(`/api/children/${childId}/expenses`, { amount: expense.amount, reason: expense.reason })
    expensingId.value = null
    await load()
  } catch (err) {
    expenseError.value = apiErrorMessage(err)
  } finally {
    savingExpense.value = false
  }
}

async function toggleCanLogExpenses(child: ChildDetailResponse) {
  const next = !child.canLogExpenses
  child.canLogExpenses = next
  try {
    await api.patch(`/api/children/${child.childId}/can-log-expenses`, { canLogExpenses: next })
  } catch (err) {
    child.canLogExpenses = !next
    statusError.value = apiErrorMessage(err)
  }
}

const COLORS = CHILD_COLORS
const addingChild = ref(false)
const savingChild = ref(false)
const addChildError = ref<string | null>(null)
const newChild = reactive({ displayName: '', birthDate: '', colorTheme: COLORS[0], pin: '', pinConfirm: '' })

function startAddChild() {
  addingChild.value = true
  addChildError.value = null
  Object.assign(newChild, { displayName: '', birthDate: '', colorTheme: COLORS[0], pin: '', pinConfirm: '' })
}

async function submitAddChild() {
  addChildError.value = null
  if (!newChild.displayName.trim() || !newChild.birthDate) {
    addChildError.value = t('fills.missingChildFields')
    return
  }
  if (!/^\d{4}$/.test(newChild.pin)) {
    addChildError.value = t('common.pinInvalid')
    return
  }
  if (newChild.pin !== newChild.pinConfirm) {
    addChildError.value = t('common.pinMismatch')
    return
  }
  savingChild.value = true
  try {
    await api.post('/api/children', {
      displayName: newChild.displayName,
      birthDate: newChild.birthDate,
      avatar: null,
      colorTheme: newChild.colorTheme,
      pin: newChild.pin,
      locale: i18n.global.locale.value,
    })
    addingChild.value = false
    await load()
  } catch (err) {
    addChildError.value = apiErrorMessage(err)
  } finally {
    savingChild.value = false
  }
}

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<ChildDetailResponse[]>(`/api/families/${familyId}/children/detail`)
  children.value = data
  loading.value = false
}

const statusChangingId = ref<string | null>(null)
const statusError = ref<string | null>(null)
const confirmingDeleteId = ref<string | null>(null)

async function deactivateChild(childId: string) {
  statusChangingId.value = childId
  statusError.value = null
  try {
    await api.post(`/api/children/${childId}/deactivate`)
    await load()
  } catch (err) {
    statusError.value = apiErrorMessage(err)
  } finally {
    statusChangingId.value = null
  }
}

async function reactivateChild(childId: string) {
  statusChangingId.value = childId
  statusError.value = null
  try {
    await api.post(`/api/children/${childId}/reactivate`)
    await load()
  } catch (err) {
    statusError.value = apiErrorMessage(err)
  } finally {
    statusChangingId.value = null
  }
}

function askDeleteConfirmation(childId: string) {
  confirmingDeleteId.value = childId
  statusError.value = null
}

async function deleteChild(childId: string) {
  statusChangingId.value = childId
  statusError.value = null
  try {
    await api.delete(`/api/children/${childId}`)
    confirmingDeleteId.value = null
    await load()
  } catch (err) {
    statusError.value = apiErrorMessage(err)
    confirmingDeleteId.value = null
  } finally {
    statusChangingId.value = null
  }
}

function startEdit(child: ChildDetailResponse) {
  editingId.value = child.childId
  form.customAllowance = child.hasCustomAllowance
  form.monthlyAmount = child.allowanceMonthlyAmount ?? 0
  form.spendingPercentage = child.allowanceSpendingPercentage ?? 70
  form.baseMinutes = child.screenBaseMinutes ?? 0
}

function cancelEdit() {
  editingId.value = null
}

async function save(childId: string) {
  saving.value = true
  try {
    const calls = [api.patch(`/api/children/${childId}/screen-time-rule`, { baseMinutes: form.baseMinutes })]
    if (form.customAllowance) {
      calls.push(api.patch(`/api/children/${childId}/allowance-rule`, {
        monthlyAmount: form.monthlyAmount,
        spendingPercentage: form.spendingPercentage,
        savingsPercentage: 100 - form.spendingPercentage,
      }))
    } else {
      calls.push(api.delete(`/api/children/${childId}/allowance-rule`))
    }
    await Promise.all(calls)
    editingId.value = null
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="fills">
    <h1>{{ t('fills.title') }}</h1>
    <p class="fills__sub">{{ t('fills.subtitle') }}</p>

    <p v-if="!loading && children.length === 0" class="fills__empty">{{ t('fills.empty') }}</p>

    <BaseCard v-if="addingChild" class="add-child-card">
      <form class="child-card__form" @submit.prevent="submitAddChild">
        <label>
          {{ t('fills.childNameLabel') }}
          <input v-model="newChild.displayName" type="text" required autofocus />
        </label>
        <label>
          {{ t('fills.birthDateLabel') }}
          <BirthDateInput v-model="newChild.birthDate" required />
        </label>
        <label>
          {{ t('common.pinLabel') }}
          <input v-model="newChild.pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <label>
          {{ t('common.pinConfirmLabel') }}
          <input v-model="newChild.pinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <div class="child-card__colors">
          <button
            v-for="color in COLORS"
            :key="color"
            type="button"
            class="child-card__color"
            :class="{ active: newChild.colorTheme === color }"
            :style="{ background: color }"
            @click="newChild.colorTheme = color"
          />
        </div>
        <p v-if="addChildError" class="fills__error">{{ addChildError }}</p>
        <div class="child-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="savingChild">
            {{ savingChild ? t('fills.adding') : t('fills.addChild') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingChild" @click="addingChild = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="fills__add" @click="startAddChild">+ {{ t('fills.addChild') }}</BaseButton>

    <p v-if="statusError" class="fills__error">{{ statusError }}</p>

    <div
      v-for="child in children"
      :key="child.childId"
      class="child-card"
      :class="{ 'child-card--inactive': !child.active }"
      :style="{ '--child-color': child.avatarColor ?? 'var(--primary)' }"
    >
      <div class="child-card__band">
        <span class="child-card__avatar">
          <svg v-if="avatarIconPath(child)" :viewBox="AVATAR_ICON_VIEWBOX" fill="white"><path :d="avatarIconPath(child)!" /></svg>
          <span v-else>{{ child.displayName.charAt(0).toUpperCase() }}</span>
        </span>
        <span class="child-card__name">
          {{ child.displayName }}
          <span v-if="!child.active" class="child-card__inactive-badge">{{ t('fills.inactiveLabel') }}</span>
        </span>
        <span class="child-card__age">{{ t('fills.age', { n: child.age }) }}</span>
      </div>

      <div class="child-card__body">
        <div v-if="editingId !== child.childId" class="child-card__info">
          <span v-if="child.allowanceMonthlyAmount !== null">
            {{ child.hasCustomAllowance ? t('fills.allowancePrefix') : t('fills.allowanceGeneralPrefix') }}
            <AmountDisplay :value="child.allowanceMonthlyAmount" :unit="t('common.perMonthUnit')" />
            {{ t('fills.allowanceDetail', { spending: child.allowanceSpendingPercentage, savings: child.allowanceSavingsPercentage }) }}
          </span>
          <span v-else>{{ t('fills.noAllowance') }}</span>
          <span v-if="child.screenBaseMinutes !== null">{{ t('fills.screenTimeLabel', { minutes: child.screenBaseMinutes }) }}</span>
          <span v-else>{{ t('fills.noScreenTime') }}</span>
        </div>

        <form v-else class="child-card__form" @submit.prevent="save(child.childId)">
          <div class="child-card__switch-row">
            <span>{{ t('fills.customAllowanceLabel') }}</span>
            <BaseSwitch v-model="form.customAllowance" />
          </div>
          <template v-if="form.customAllowance">
            <label>
              {{ t('fills.monthlyAmountLabel') }}
              <input v-model.number="form.monthlyAmount" type="number" min="0" step="0.5" required />
            </label>
            <label>
              {{ t('fills.spendingPercentageLabel') }}
              <input v-model.number="form.spendingPercentage" type="number" min="0" max="100" required />
            </label>
          </template>
          <p v-else class="child-card__readonly-hint">
            <template v-if="child.allowanceMonthlyAmount !== null">
              {{ t('fills.allowanceGeneralPrefix') }} <AmountDisplay :value="child.allowanceMonthlyAmount" :unit="t('common.perMonthUnit')" />
              {{ t('fills.allowanceDetail', { spending: child.allowanceSpendingPercentage, savings: child.allowanceSavingsPercentage }) }}
            </template>
            <template v-else>{{ t('fills.noAllowance') }}</template>
          </p>
          <label>
            {{ t('fills.screenMinutesLabel') }}
            <MinutesInput v-model="form.baseMinutes" />
          </label>
          <div class="child-card__form-actions">
            <BaseButton type="submit" variant="primary" :disabled="saving">{{ saving ? t('common.saving') : t('common.save') }}</BaseButton>
            <BaseButton type="button" variant="ghost" :disabled="saving" @click="cancelEdit">{{ t('common.cancel') }}</BaseButton>
          </div>
        </form>

        <div class="child-card__actions">
          <template v-if="child.active">
            <BaseButton v-if="editingId !== child.childId" variant="accent" @click="startEdit(child)">
              <svg class="btn-icon" viewBox="0 0 24 24"><path d="M12 20h9M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z" /></svg>
              {{ t('fills.edit') }}
            </BaseButton>
            <BaseButton v-if="adjustingId !== child.childId" variant="accent" @click="startAdjustment(child)">
              <svg class="btn-icon" viewBox="0 0 24 24"><path d="M7 9l5-5 5 5M7 15l5 5 5-5" /></svg>
              {{ t('fills.manualAdjustment') }}
            </BaseButton>
            <BaseButton v-if="expensingId !== child.childId" variant="secondary" @click="startExpense(child)">
              <svg class="btn-icon" viewBox="0 0 24 24"><rect x="3" y="6" width="18" height="13" rx="2.2" /><path d="M3 10.5h18M7 15.2h3.2" /></svg>
              {{ t('fills.expenseButton') }}
            </BaseButton>
            <BaseButton variant="ghost" :disabled="statusChangingId === child.childId" @click="deactivateChild(child.childId)">
              <svg class="btn-icon" viewBox="0 0 24 24"><path d="M12 2.5v8" /><path d="M18.4 6.6a9 9 0 11-12.77 0" /></svg>
              {{ t('fills.deactivate') }}
            </BaseButton>
          </template>
          <template v-else>
            <BaseButton variant="accent" :disabled="statusChangingId === child.childId" @click="reactivateChild(child.childId)">
              <svg class="btn-icon" viewBox="0 0 24 24"><path d="M12 2.5v8" /><path d="M18.4 6.6a9 9 0 11-12.77 0" /></svg>
              {{ t('fills.reactivate') }}
            </BaseButton>
            <BaseButton v-if="child.deletable" variant="danger" :disabled="statusChangingId === child.childId" @click="askDeleteConfirmation(child.childId)">
              <svg class="btn-icon" viewBox="0 0 24 24"><path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m2 0l-1 13a1 1 0 01-1 1H8a1 1 0 01-1-1L6 7" /></svg>
              {{ t('fills.delete') }}
            </BaseButton>
          </template>
        </div>

        <div v-if="child.active" class="child-card__switch-row">
          <span>{{ t('fills.canLogExpensesLabel') }}</span>
          <BaseSwitch :model-value="child.canLogExpenses" @update:model-value="toggleCanLogExpenses(child)" />
        </div>

        <div v-if="confirmingDeleteId === child.childId" class="child-card__delete-confirm">
          <p>{{ t('fills.deleteConfirm', { name: child.displayName }) }}</p>
          <div class="child-card__form-actions">
            <BaseButton variant="danger" :disabled="statusChangingId === child.childId" @click="deleteChild(child.childId)">
              {{ t('fills.deleteConfirmYes') }}
            </BaseButton>
            <BaseButton variant="ghost" :disabled="statusChangingId === child.childId" @click="confirmingDeleteId = null">
              {{ t('common.cancel') }}
            </BaseButton>
          </div>
        </div>

        <form v-if="expensingId === child.childId" class="child-card__form" @submit.prevent="submitExpense(child.childId)">
          <p class="child-card__form-title">{{ t('fills.expenseFormTitle', { name: child.displayName }) }}</p>
          <label>
            {{ t('fills.expenseAmountLabel') }}
            <input v-model.number="expense.amount" type="number" min="0.01" step="0.01" required autofocus />
          </label>
          <label>
            {{ t('fills.expenseReasonLabel') }}
            <input v-model="expense.reason" type="text" required />
          </label>
          <p v-if="expenseError" class="fills__error">{{ expenseError }}</p>
          <div class="child-card__form-actions">
            <BaseButton type="submit" variant="primary" :disabled="savingExpense">
              {{ savingExpense ? t('common.saving') : t('fills.expenseSubmit') }}
            </BaseButton>
            <BaseButton type="button" variant="ghost" :disabled="savingExpense" @click="expensingId = null">{{ t('common.cancel') }}</BaseButton>
          </div>
        </form>

        <p v-if="adjustmentNotice?.childId === child.childId" class="adj-notice" role="status">{{ adjustmentNotice.text }}</p>

        <form v-if="adjustingId === child.childId" class="child-card__form" @submit.prevent="submitAdjustment(child.childId)">
          <p class="child-card__form-title">{{ t('fills.adjustmentFormTitle', { name: child.displayName }) }}</p>

          <div class="adj-field">
            <span :id="`adj-type-${child.childId}`" class="adj-field__label">{{ t('fills.adjustmentTypeLabel') }}</span>
            <div class="adj-seg" role="group" :aria-labelledby="`adj-type-${child.childId}`">
              <button
                v-for="option in adjustmentTypeOptions"
                :key="option.value"
                type="button"
                class="adj-seg__option"
                :class="{ 'adj-seg__option--on': adjustment.type === option.value }"
                :aria-pressed="adjustment.type === option.value"
                @click="adjustment.type = option.value"
              >
                {{ t(option.label) }}
              </button>
            </div>
          </div>

          <div class="adj-field">
            <span :id="`adj-category-${child.childId}`" class="adj-field__label">{{ t('fills.adjustmentCategoryLabel') }}</span>
            <div class="adj-seg" role="group" :aria-labelledby="`adj-category-${child.childId}`">
              <button
                v-for="option in adjustmentCategoryOptions"
                :key="option.value"
                type="button"
                class="adj-seg__option"
                :class="{ 'adj-seg__option--on': adjustment.category === option.value }"
                :aria-pressed="adjustment.category === option.value"
                @click="adjustment.category = option.value"
              >
                {{ t(option.label) }}
              </button>
            </div>
          </div>

          <label v-if="adjustment.category === 'MONEY'">
            {{ t('fills.adjustmentValueMoneyLabel') }}
            <input v-model.number="adjustment.value" type="number" min="0" step="0.5" />
          </label>
          <label v-else>
            {{ t('fills.adjustmentValueMinutesLabel') }}
            <MinutesInput v-model="adjustment.value" />
          </label>

          <div v-if="isMoneyBonus" class="adj-field">
            <span :id="`adj-dest-${child.childId}`" class="adj-field__label">{{ t('fills.adjustmentDestinationLabel') }}</span>
            <div class="adj-dest" role="radiogroup" :aria-labelledby="`adj-dest-${child.childId}`">
              <label
                v-for="option in destinationOptions"
                :key="option.value"
                class="adj-dest__option"
                :class="{ 'adj-dest__option--on': adjustment.destination === option.value }"
              >
                <input v-model="adjustment.destination" type="radio" :name="`adj-dest-${child.childId}`" :value="option.value" />
                <span class="adj-dest__text">
                  <span class="adj-dest__name">{{ t(option.name) }}</span>
                  <span class="adj-dest__hint">{{ t(option.hint) }}</span>
                </span>
              </label>
            </div>
            <div v-if="adjustment.destination === 'CUSTOM'" class="adj-custom">
              <input v-model.number="adjustment.spendingShare" type="range" min="0" max="100" step="5" :aria-label="t('fills.destCustomSliderLabel')" />
              <div class="adj-custom__values">
                <span>{{ t('fills.destCustomSpending', { pct: adjustment.spendingShare }) }}</span>
                <span>{{ t('fills.destCustomSavings', { pct: 100 - adjustment.spendingShare }) }}</span>
              </div>
            </div>
          </div>

          <p v-if="adjustment.type === 'PENALTY' && adjustment.category === 'MONEY'" class="adj-note">{{ t('fills.adjustmentPenaltyNote') }}</p>
          <p v-else-if="adjustment.category === 'SCREEN_TIME'" class="adj-note">{{ t('fills.adjustmentScreenTimeNote') }}</p>

          <div v-if="previewParts.length" class="adj-preview">
            <span class="adj-preview__label">{{ t('fills.adjustmentPreviewLabel', { name: child.displayName }) }}</span>
            <div class="adj-preview__chips">
              <span v-for="part in previewParts" :key="part.wallet + (part.goalName ?? '')" class="adj-chip">
                <i class="adj-chip__dot" :class="`adj-chip__dot--${part.wallet.toLowerCase()}`" />
                {{ partLabel(part) }}
                <span class="adj-chip__amount">+{{ formatMoney(part.amount) }} €</span>
              </span>
            </div>
          </div>

          <label>
            {{ t('fills.adjustmentReasonLabel') }}
            <input v-model="adjustment.reason" type="text" required :placeholder="t('fills.adjustmentReasonPlaceholder')" />
          </label>
          <p v-if="adjustmentError" class="fills__error">{{ adjustmentError }}</p>
          <div class="child-card__form-actions">
            <BaseButton type="submit" variant="primary" :disabled="savingAdjustment">
              {{ savingAdjustment ? t('common.saving') : t('common.save') }}
            </BaseButton>
            <BaseButton type="button" variant="ghost" :disabled="savingAdjustment" @click="adjustingId = null">{{ t('common.cancel') }}</BaseButton>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fills {
  max-width: 640px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.fills__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1.25rem;
}

.fills__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.fills__add {
  margin-bottom: 1rem;
}

.fills__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.child-card__colors {
  display: flex;
  gap: 0.5rem;
}

.child-card__color {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
}

.child-card__color.active {
  border-color: var(--text);
}

.add-child-card {
  margin-bottom: 0.9rem;
}

.child-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 0.9rem;
  border: 1px solid color-mix(in srgb, var(--text) 8%, transparent);
  box-shadow: 0 2px 12px -4px color-mix(in srgb, var(--text) 12%, transparent);
}

.child-card--inactive {
  opacity: 0.6;
}

.child-card__inactive-badge {
  display: inline-block;
  margin-left: 0.4rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.28);
  color: white;
  font-size: 0.62rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  vertical-align: middle;
}

.child-card__delete-confirm {
  margin-top: 0.75rem;
  padding: 0.75rem 0.9rem;
  border-radius: 12px;
  background: color-mix(in srgb, var(--error) 10%, white);
}

.child-card__delete-confirm p {
  margin: 0 0 0.6rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text);
}

.child-card__band {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.7rem 0.9rem;
  background: var(--child-color, var(--primary));
}

.child-card__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.28);
  color: white;
  font-family: var(--font-heading);
  font-weight: 800;
  font-size: 0.82rem;
  flex-shrink: 0;
}

.child-card__avatar svg {
  width: 16px;
  height: 16px;
}

.child-card__name {
  flex: 1;
  min-width: 0;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
  color: white;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.child-card__age {
  font-size: 0.78rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  flex-shrink: 0;
}

.child-card__body {
  padding: 0.85rem 1rem;
}

.child-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.6rem;
}

.btn-icon {
  width: 13px;
  height: 13px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}

.child-card__form-title {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.85rem;
  margin: 0;
}

.child-card__info {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.85rem;
  color: var(--text);
}

.child-card__form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.child-card__form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.child-card__form input,
.child-card__form select {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.child-card__form-actions {
  display: flex;
  gap: 0.5rem;
}

.child-card__switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.child-card__readonly-hint {
  font-size: 0.82rem;
  color: var(--muted);
  margin: 0;
}

.adj-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.adj-field__label {
  font-weight: 700;
  font-size: 0.82rem;
}

.adj-seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px;
  padding: 3px;
  border-radius: 11px;
  background: color-mix(in srgb, var(--text) 6%, transparent);
}

.adj-seg__option {
  border: none;
  background: transparent;
  padding: 0.5rem 0.4rem;
  border-radius: 9px;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.8rem;
  color: var(--muted);
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.adj-seg__option--on {
  background: white;
  color: var(--primary);
  box-shadow: 0 1px 4px color-mix(in srgb, var(--text) 15%, transparent);
}

.adj-dest {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.child-card__form .adj-dest__option {
  flex-direction: row;
  align-items: flex-start;
  gap: 0.6rem;
  padding: 0.6rem 0.75rem;
  border: 2px solid color-mix(in srgb, var(--primary) 14%, transparent);
  border-radius: 13px;
  background: white;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.child-card__form .adj-dest__option--on {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 6%, white);
}

.child-card__form .adj-dest__option input {
  flex-shrink: 0;
  margin: 0.2rem 0 0;
  padding: 0;
  border: none;
  accent-color: var(--primary);
}

.adj-dest__text {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.adj-dest__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.85rem;
  line-height: 1.2;
}

.adj-dest__hint {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--muted);
}

.adj-custom {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0 0.2rem;
}

.child-card__form .adj-custom input {
  width: 100%;
  padding: 0;
  border: none;
  accent-color: var(--primary);
}

.adj-custom__values {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.adj-note {
  margin: 0;
  padding: 0.55rem 0.75rem;
  border-radius: 10px;
  background: color-mix(in srgb, var(--text) 4%, transparent);
  font-size: 0.74rem;
  line-height: 1.5;
  color: var(--muted);
}

.adj-preview {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.6rem 0.75rem;
  border-radius: 10px;
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.adj-preview__label {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--primary);
}

.adj-preview__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.adj-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  background: white;
  box-shadow: 0 1px 3px color-mix(in srgb, var(--text) 12%, transparent);
  font-size: 0.78rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.adj-chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.adj-chip__dot--spending {
  background: var(--primary);
}

.adj-chip__dot--savings {
  background: var(--accent);
}

.adj-chip__dot--goal {
  background: var(--child-color, var(--primary));
}

.adj-chip__amount {
  font-weight: 600;
  color: var(--muted);
}

.adj-notice {
  margin: 0.75rem 0 0;
  padding: 0.6rem 0.8rem;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--success) 40%, transparent);
  background: color-mix(in srgb, var(--success) 12%, transparent);
  font-size: 0.8rem;
  font-weight: 700;
  color: color-mix(in srgb, var(--success) 55%, black);
}

@media (prefers-reduced-motion: reduce) {
  .adj-seg__option,
  .child-card__form .adj-dest__option {
    transition: none;
  }
}
</style>
