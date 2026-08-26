<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseSwitch from '@/components/base/BaseSwitch.vue'
import BirthDateInput from '@/components/base/BirthDateInput.vue'
import ChildAvatar from '@/components/base/ChildAvatar.vue'
import MinutesInput from '@/components/base/MinutesInput.vue'
import { CHILD_COLORS } from '@/utils/childColors'
import { apiErrorMessage } from '@/utils/apiError'
import { i18n } from '@/i18n'
import type { ChildDetailResponse } from '@/types/parent'

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
const adjustment = reactive({
  type: 'BONUS' as 'BONUS' | 'PENALTY' | 'MANUAL',
  category: 'MONEY' as 'MONEY' | 'SCREEN_TIME',
  value: 0,
  reason: '',
})

function startAdjustment(child: ChildDetailResponse) {
  adjustingId.value = child.childId
  adjustmentError.value = null
  Object.assign(adjustment, { type: 'BONUS', category: 'MONEY', value: 0, reason: '' })
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
    if (adjustment.category === 'MONEY') {
      await api.post(`/api/children/${childId}/money-adjustments`, {
        type: adjustment.type, amount: adjustment.value, reason: adjustment.reason,
      })
    } else {
      await api.post(`/api/children/${childId}/screen-time/adjustments`, {
        type: adjustment.type, minutes: adjustment.value, reason: adjustment.reason,
      })
    }
    adjustingId.value = null
  } catch (err) {
    adjustmentError.value = apiErrorMessage(err)
  } finally {
    savingAdjustment.value = false
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

function startEdit(child: ChildDetailResponse) {
  editingId.value = child.childId
  form.customAllowance = child.hasCustomAllowance
  form.monthlyAmount = child.allowanceMonthlyAmount ?? 0
  form.spendingPercentage = child.allowanceSpendingPercentage ?? 70
  // El pare veu i edita els minuts per setmana; per sota es continua generant un cop al
  // mes (screen_minutes_monthly), sense cap canvi de calendari (Prompt 15, conversió ×4).
  form.baseMinutes = Math.round((child.screenBaseMinutes ?? 0) / 4)
}

function cancelEdit() {
  editingId.value = null
}

async function save(childId: string) {
  saving.value = true
  try {
    const calls = [api.patch(`/api/children/${childId}/screen-time-rule`, { baseMinutes: form.baseMinutes * 4 })]
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

    <BaseCard v-if="addingChild" class="child-card">
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

    <BaseCard
      v-for="child in children"
      :key="child.childId"
      class="child-card"
      :style="{ '--child-color': child.avatarColor ?? 'var(--primary)' }"
    >
      <div class="child-card__head">
        <div class="child-card__identity">
          <ChildAvatar :color="child.avatarColor" :icon="child.avatarIcon" :name="child.displayName" size="small" />
          <div>
            <div class="child-card__name">{{ child.displayName }}</div>
            <div class="child-card__age">{{ t('fills.age', { n: child.age }) }}</div>
          </div>
        </div>
        <div class="child-card__head-actions">
          <BaseButton v-if="editingId !== child.childId" variant="accent" @click="startEdit(child)">{{ t('fills.edit') }}</BaseButton>
          <BaseButton v-if="adjustingId !== child.childId" variant="accent" @click="startAdjustment(child)">{{ t('fills.manualAdjustment') }}</BaseButton>
        </div>
      </div>

      <div v-if="editingId !== child.childId" class="child-card__info">
        <span v-if="child.allowanceMonthlyAmount !== null">
          {{ child.hasCustomAllowance ? t('fills.allowancePrefix') : t('fills.allowanceGeneralPrefix') }}
          <AmountDisplay :value="child.allowanceMonthlyAmount" unit="€/mes" />
          {{ t('fills.allowanceDetail', { spending: child.allowanceSpendingPercentage, savings: child.allowanceSavingsPercentage }) }}
        </span>
        <span v-else>{{ t('fills.noAllowance') }}</span>
        <span v-if="child.screenBaseMinutes !== null">{{ t('fills.screenTimeLabel', { minutes: Math.round(child.screenBaseMinutes / 4) }) }}</span>
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
            {{ t('fills.allowanceGeneralPrefix') }} <AmountDisplay :value="child.allowanceMonthlyAmount" unit="€/mes" />
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

      <form v-if="adjustingId === child.childId" class="child-card__form" @submit.prevent="submitAdjustment(child.childId)">
        <label>
          {{ t('fills.adjustmentTypeLabel') }}
          <select v-model="adjustment.type">
            <option value="BONUS">{{ t('fills.adjustmentBonus') }}</option>
            <option value="PENALTY">{{ t('fills.adjustmentPenalty') }}</option>
            <option value="MANUAL">{{ t('fills.adjustmentManual') }}</option>
          </select>
        </label>
        <label>
          {{ t('fills.adjustmentCategoryLabel') }}
          <select v-model="adjustment.category">
            <option value="MONEY">{{ t('fills.adjustmentCategoryMoney') }}</option>
            <option value="SCREEN_TIME">{{ t('fills.adjustmentCategoryScreenTime') }}</option>
          </select>
        </label>
        <label v-if="adjustment.category === 'MONEY'">
          {{ t('fills.adjustmentValueMoneyLabel') }}
          <input v-model.number="adjustment.value" type="number" min="0" step="0.5" />
        </label>
        <label v-else>
          {{ t('fills.adjustmentValueMinutesLabel') }}
          <MinutesInput v-model="adjustment.value" />
        </label>
        <label>
          {{ t('fills.adjustmentReasonLabel') }}
          <input v-model="adjustment.reason" type="text" required />
        </label>
        <p v-if="adjustmentError" class="fills__error">{{ adjustmentError }}</p>
        <div class="child-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="savingAdjustment">
            {{ savingAdjustment ? t('common.saving') : t('common.save') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingAdjustment" @click="adjustingId = null">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
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

.child-card {
  margin-bottom: 0.9rem;
  border-left: 4px solid var(--child-color, var(--primary));
}

.child-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.child-card__identity {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.child-card__head-actions {
  display: flex;
  gap: 0.5rem;
}

.child-card__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.95rem;
}

.child-card__age {
  font-size: 0.76rem;
  color: var(--muted);
}

.child-card__info {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  margin-top: 0.6rem;
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
</style>
