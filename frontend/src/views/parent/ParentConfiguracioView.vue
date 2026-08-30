<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseSwitch from '@/components/base/BaseSwitch.vue'
import LanguageSwitcher from '@/components/base/LanguageSwitcher.vue'
import { apiErrorMessage } from '@/utils/apiError'
import { i18n } from '@/i18n'
import type { AllowanceRuleResponse, FamilySettings } from '@/types/parent'
import type { LoginProfile } from '@/types/auth'

const { t } = useI18n()
const auth = useAuthStore()
const loading = ref(true)
const settings = reactive<FamilySettings>({
  taskApprovalRequired: true,
  notifyPendingApprovalsEnabled: false,
  allowSavingsTransfer: true,
})

const allowanceRules = ref<AllowanceRuleResponse[]>([])
const addingRule = ref(false)
const editingRuleId = ref<string | null>(null)
const savingRule = ref(false)
const ruleError = ref<string | null>(null)
const ruleForm = reactive({ minAge: 0, maxAge: 17, monthlyAmount: 0, spendingPercentage: 70, savingsPercentage: 30 })

const members = ref<LoginProfile[]>([])
const addingParent = ref(false)
const savingParent = ref(false)
const addParentError = ref<string | null>(null)
const newParent = reactive({ displayName: '', pin: '', pinConfirm: '' })

const resettingUserId = ref<string | null>(null)
const resetPin = ref('')
const resetPinConfirm = ref('')
const resetError = ref<string | null>(null)
const savingReset = ref(false)

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const [settingsRes, membersRes, rulesRes] = await Promise.all([
    api.get<FamilySettings>(`/api/families/${familyId}/settings`),
    api.get<LoginProfile[]>(`/api/families/${familyId}/login-profiles`),
    api.get<AllowanceRuleResponse[]>('/api/allowance-rules'),
  ])
  Object.assign(settings, settingsRes.data)
  members.value = membersRes.data
  allowanceRules.value = rulesRes.data
  loading.value = false
}

function startAddRule() {
  addingRule.value = true
  editingRuleId.value = null
  ruleError.value = null
  Object.assign(ruleForm, { minAge: 0, maxAge: 17, monthlyAmount: 0, spendingPercentage: 70, savingsPercentage: 30 })
}

function startEditRule(rule: AllowanceRuleResponse) {
  addingRule.value = false
  editingRuleId.value = rule.id
  ruleError.value = null
  Object.assign(ruleForm, {
    minAge: rule.minAge, maxAge: rule.maxAge, monthlyAmount: rule.monthlyAmount,
    spendingPercentage: rule.spendingPercentage, savingsPercentage: rule.savingsPercentage,
  })
}

async function submitRule() {
  ruleError.value = null
  if (ruleForm.spendingPercentage + ruleForm.savingsPercentage !== 100) {
    ruleError.value = t('config.percentagesMustSumTo100')
    return
  }
  savingRule.value = true
  try {
    if (editingRuleId.value) {
      await api.patch(`/api/allowance-rules/${editingRuleId.value}`, ruleForm)
    } else {
      await api.post('/api/allowance-rules', ruleForm)
    }
    addingRule.value = false
    editingRuleId.value = null
    await load()
  } catch (err) {
    ruleError.value = apiErrorMessage(err)
  } finally {
    savingRule.value = false
  }
}

async function removeRule(rule: AllowanceRuleResponse) {
  await api.delete(`/api/allowance-rules/${rule.id}`)
  await load()
}

async function toggle(key: keyof FamilySettings) {
  if (loading.value) return
  settings[key] = !settings[key]
  const familyId = auth.familyId
  await api.patch(`/api/families/${familyId}/settings`, settings)
}

function startAddParent() {
  addingParent.value = true
  addParentError.value = null
  Object.assign(newParent, { displayName: '', pin: '', pinConfirm: '' })
}

async function submitAddParent() {
  addParentError.value = null
  if (!newParent.displayName.trim()) {
    addParentError.value = t('config.missingName')
    return
  }
  if (!/^\d{4}$/.test(newParent.pin)) {
    addParentError.value = t('common.pinInvalid')
    return
  }
  if (newParent.pin !== newParent.pinConfirm) {
    addParentError.value = t('common.pinMismatch')
    return
  }
  savingParent.value = true
  try {
    await api.post('/api/families/current/parents', {
      displayName: newParent.displayName,
      pin: newParent.pin,
      locale: i18n.global.locale.value,
    })
    addingParent.value = false
    await load()
  } catch (err) {
    addParentError.value = apiErrorMessage(err)
  } finally {
    savingParent.value = false
  }
}

function startResetPin(member: LoginProfile) {
  resettingUserId.value = member.id
  resetError.value = null
  resetPin.value = ''
  resetPinConfirm.value = ''
}

async function submitResetPin(userId: string) {
  resetError.value = null
  if (!/^\d{4}$/.test(resetPin.value)) {
    resetError.value = t('common.pinInvalid')
    return
  }
  if (resetPin.value !== resetPinConfirm.value) {
    resetError.value = t('common.pinMismatch')
    return
  }
  savingReset.value = true
  try {
    await api.patch(`/api/users/${userId}/pin`, { newPin: resetPin.value })
    resettingUserId.value = null
  } catch (err) {
    resetError.value = apiErrorMessage(err)
  } finally {
    savingReset.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="config">
    <h1>{{ t('config.title') }}</h1>
    <p class="config__sub">{{ t('config.subtitle') }}</p>

    <div class="settings-row">
      <span>{{ t('config.taskApprovalRequired') }}</span>
      <BaseSwitch :model-value="settings.taskApprovalRequired" @update:model-value="toggle('taskApprovalRequired')" />
    </div>
    <div class="settings-row">
      <span>{{ t('config.notifyPendingApprovals') }}</span>
      <BaseSwitch :model-value="settings.notifyPendingApprovalsEnabled" @update:model-value="toggle('notifyPendingApprovalsEnabled')" />
    </div>
    <div class="settings-row">
      <span>{{ t('config.allowSavingsTransfer') }}</span>
      <BaseSwitch :model-value="settings.allowSavingsTransfer" @update:model-value="toggle('allowSavingsTransfer')" />
    </div>

    <RouterLink :to="{ name: 'parent-nfc-tags' }" class="config__nfc-link text-link-underline">
      {{ t('config.nfcLink') }}
    </RouterLink>

    <div class="config__language">
      <span>{{ t('common.language') }}</span>
      <LanguageSwitcher />
    </div>

    <h2 class="config__section-title">{{ t('config.allowanceRulesTitle') }}</h2>

    <BaseCard v-for="rule in allowanceRules" :key="rule.id" class="rule-card">
      <div class="rule-card__row">
        <div class="rule-card__label">{{ t('config.ageRange', { min: rule.minAge, max: rule.maxAge }) }}</div>
        <div class="rule-card__actions">
          <BaseButton variant="accent" @click="startEditRule(rule)">{{ t('fills.edit') }}</BaseButton>
          <BaseButton variant="danger" @click="removeRule(rule)">{{ t('tasques.deactivate') }}</BaseButton>
        </div>
      </div>
      <div class="rule-card__detail">
        {{ t('fills.allowancePrefix') }} {{ rule.monthlyAmount }} {{ t('common.perMonthUnit') }}
        {{ t('fills.allowanceDetail', { spending: rule.spendingPercentage, savings: rule.savingsPercentage }) }}
      </div>

      <form v-if="editingRuleId === rule.id" class="member-card__form" @submit.prevent="submitRule">
        <label>{{ t('config.minAgeLabel') }}<input v-model.number="ruleForm.minAge" type="number" min="0" required /></label>
        <label>{{ t('config.maxAgeLabel') }}<input v-model.number="ruleForm.maxAge" type="number" min="0" required /></label>
        <label>{{ t('fills.monthlyAmountLabel') }}<input v-model.number="ruleForm.monthlyAmount" type="number" min="0" step="0.5" required /></label>
        <label>{{ t('fills.spendingPercentageLabel') }}<input v-model.number="ruleForm.spendingPercentage" type="number" min="0" max="100" required /></label>
        <label>{{ t('config.savingsPercentageLabel') }}<input v-model.number="ruleForm.savingsPercentage" type="number" min="0" max="100" required /></label>
        <p v-if="ruleError" class="config__error">{{ ruleError }}</p>
        <div class="member-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="savingRule">{{ savingRule ? t('common.saving') : t('common.save') }}</BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingRule" @click="editingRuleId = null">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>

    <BaseCard v-if="addingRule" class="rule-card">
      <form class="member-card__form" @submit.prevent="submitRule">
        <label>{{ t('config.minAgeLabel') }}<input v-model.number="ruleForm.minAge" type="number" min="0" required autofocus /></label>
        <label>{{ t('config.maxAgeLabel') }}<input v-model.number="ruleForm.maxAge" type="number" min="0" required /></label>
        <label>{{ t('fills.monthlyAmountLabel') }}<input v-model.number="ruleForm.monthlyAmount" type="number" min="0" step="0.5" required /></label>
        <label>{{ t('fills.spendingPercentageLabel') }}<input v-model.number="ruleForm.spendingPercentage" type="number" min="0" max="100" required /></label>
        <label>{{ t('config.savingsPercentageLabel') }}<input v-model.number="ruleForm.savingsPercentage" type="number" min="0" max="100" required /></label>
        <p v-if="ruleError" class="config__error">{{ ruleError }}</p>
        <div class="member-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="savingRule">{{ savingRule ? t('common.saving') : t('common.save') }}</BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingRule" @click="addingRule = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="config__add-parent" @click="startAddRule">+ {{ t('config.addAllowanceRule') }}</BaseButton>

    <h2 class="config__section-title">{{ t('config.familyMembersTitle') }}</h2>

    <BaseCard v-for="member in members" :key="member.id" class="member-card">
      <div class="member-card__row">
        <span class="member-card__name">{{ member.displayName }}</span>
        <span class="member-card__role">{{ member.role === 'PARENT' ? t('config.roleAdult') : t('config.roleChild') }}</span>
        <BaseButton
          v-if="resettingUserId !== member.id"
          variant="accent"
          @click="startResetPin(member)"
        >
          {{ t('config.resetPin') }}
        </BaseButton>
      </div>
      <form v-if="resettingUserId === member.id" class="member-card__form" @submit.prevent="submitResetPin(member.id)">
        <label>
          {{ t('config.pinNewLabel') }}
          <input v-model="resetPin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required autofocus />
        </label>
        <label>
          {{ t('config.pinConfirmLabel') }}
          <input v-model="resetPinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <p v-if="resetError" class="config__error">{{ resetError }}</p>
        <div class="member-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="savingReset">
            {{ savingReset ? t('common.saving') : t('common.save') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingReset" @click="resettingUserId = null">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>

    <BaseCard v-if="addingParent" class="member-card">
      <form class="member-card__form" @submit.prevent="submitAddParent">
        <label>
          {{ t('config.adultNameLabel') }}
          <input v-model="newParent.displayName" type="text" required autofocus />
        </label>
        <label>
          {{ t('common.pinLabel') }}
          <input v-model="newParent.pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <label>
          {{ t('common.pinConfirmLabel') }}
          <input v-model="newParent.pinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <p v-if="addParentError" class="config__error">{{ addParentError }}</p>
        <div class="member-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="savingParent">
            {{ savingParent ? t('config.adding') : t('config.addAdult') }}
          </BaseButton>
          <BaseButton type="button" variant="ghost" :disabled="savingParent" @click="addingParent = false">{{ t('common.cancel') }}</BaseButton>
        </div>
      </form>
    </BaseCard>
    <BaseButton v-else variant="accent" class="config__add-parent" @click="startAddParent">{{ t('config.addParent') }}</BaseButton>
  </div>
</template>

<style scoped>
.config {
  max-width: 560px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.config__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1.25rem;
}

.settings-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 0.9rem 1rem;
  border-radius: 12px;
  margin-bottom: 0.6rem;
  font-size: 0.87rem;
}

.config__nfc-link {
  display: inline-block;
  margin-top: 1.25rem;
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--primary);
  text-decoration: none;
}

.config__language {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 0.9rem 1rem;
  border-radius: 12px;
  margin-top: 0.6rem;
  font-size: 0.87rem;
}

.config__section-title {
  margin: 1.75rem 0 0.75rem;
  font-size: 1.05rem;
}

.config__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}

.config__add-parent {
  margin-top: 0.25rem;
}

.rule-card {
  margin-bottom: 0.6rem;
}

.rule-card__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.rule-card__label {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
}

.rule-card__actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.rule-card__detail {
  font-size: 0.8rem;
  color: var(--muted);
  margin-top: 0.3rem;
}

.member-card {
  margin-bottom: 0.6rem;
}

.member-card__row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.member-card__name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.92rem;
  flex: 1;
}

.member-card__role {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
}

.member-card__form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.member-card__form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-weight: 700;
  font-size: 0.82rem;
}

.member-card__form input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.member-card__form-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
