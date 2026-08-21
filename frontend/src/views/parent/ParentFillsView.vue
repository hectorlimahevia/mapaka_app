<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { apiErrorMessage } from '@/utils/apiError'
import { i18n } from '@/i18n'
import type { ChildDetailResponse } from '@/types/parent'

const { t } = useI18n()
const auth = useAuthStore()
const children = ref<ChildDetailResponse[]>([])
const loading = ref(true)
const editingId = ref<string | null>(null)
const saving = ref(false)

const form = reactive({ monthlyAmount: 0, spendingPercentage: 70, baseMinutes: 0 })

const COLORS = ['#6C4DFF', '#FF5D8F', '#FFC93C', '#2ECC71', '#3AA0FF']
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
    await Promise.all([
      api.patch(`/api/children/${childId}/allowance-rule`, {
        monthlyAmount: form.monthlyAmount,
        spendingPercentage: form.spendingPercentage,
        savingsPercentage: 100 - form.spendingPercentage,
      }),
      api.patch(`/api/children/${childId}/screen-time-rule`, { baseMinutes: form.baseMinutes }),
    ])
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
          <input v-model="newChild.birthDate" type="date" required />
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
          <BaseButton type="button" variant="danger" :disabled="savingChild" @click="addingChild = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="fills__add" @click="startAddChild">+ {{ t('fills.addChild') }}</BaseButton>

    <BaseCard v-for="child in children" :key="child.childId" class="child-card">
      <div class="child-card__head">
        <div>
          <div class="child-card__name">{{ child.displayName }}</div>
          <div class="child-card__age">{{ t('fills.age', { n: child.age }) }}</div>
        </div>
        <BaseButton v-if="editingId !== child.childId" variant="accent" @click="startEdit(child)">{{ t('fills.edit') }}</BaseButton>
      </div>

      <div v-if="editingId !== child.childId" class="child-card__info">
        <span v-if="child.allowanceMonthlyAmount !== null">
          {{ t('fills.allowancePrefix') }} <AmountDisplay :value="child.allowanceMonthlyAmount" unit="€/mes" />
          {{ t('fills.allowanceDetail', { spending: child.allowanceSpendingPercentage, savings: child.allowanceSavingsPercentage }) }}
        </span>
        <span v-else>{{ t('fills.noAllowance') }}</span>
        <span v-if="child.screenBaseMinutes !== null">{{ t('fills.screenTimeLabel', { minutes: child.screenBaseMinutes }) }}</span>
        <span v-else>{{ t('fills.noScreenTime') }}</span>
      </div>

      <form v-else class="child-card__form" @submit.prevent="save(child.childId)">
        <label>
          {{ t('fills.monthlyAmountLabel') }}
          <input v-model.number="form.monthlyAmount" type="number" min="0" step="0.5" required />
        </label>
        <label>
          {{ t('fills.spendingPercentageLabel') }}
          <input v-model.number="form.spendingPercentage" type="number" min="0" max="100" required />
        </label>
        <label>
          {{ t('fills.screenMinutesLabel') }}
          <input v-model.number="form.baseMinutes" type="number" min="0" required />
        </label>
        <div class="child-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="saving">{{ saving ? t('common.saving') : t('common.save') }}</BaseButton>
          <BaseButton type="button" variant="danger" :disabled="saving" @click="cancelEdit">{{ t('common.cancel') }}</BaseButton>
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
}

.child-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.child-card__form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.child-card__form-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
