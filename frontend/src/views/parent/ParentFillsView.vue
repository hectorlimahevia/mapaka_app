<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import type { ChildDetailResponse } from '@/types/parent'

const auth = useAuthStore()
const children = ref<ChildDetailResponse[]>([])
const loading = ref(true)
const editingId = ref<string | null>(null)
const saving = ref(false)

const form = reactive({ monthlyAmount: 0, spendingPercentage: 70, baseMinutes: 0 })

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
    <h1>Fills</h1>
    <p class="fills__sub">Gestiona el perfil, la paga i les regles de cada fill</p>

    <p v-if="!loading && children.length === 0" class="fills__empty">Encara no hi ha cap fill donat d'alta.</p>

    <BaseCard v-for="child in children" :key="child.childId" class="child-card">
      <div class="child-card__head">
        <div>
          <div class="child-card__name">{{ child.displayName }}</div>
          <div class="child-card__age">{{ child.age }} anys</div>
        </div>
        <BaseButton v-if="editingId !== child.childId" variant="accent" @click="startEdit(child)">Edita</BaseButton>
      </div>

      <div v-if="editingId !== child.childId" class="child-card__info">
        <span v-if="child.allowanceMonthlyAmount !== null">
          Paga: <AmountDisplay :value="child.allowanceMonthlyAmount" unit="€/mes" />
          ({{ child.allowanceSpendingPercentage }}% gastar · {{ child.allowanceSavingsPercentage }}% estalviar)
        </span>
        <span v-else>Sense paga configurada</span>
        <span v-if="child.screenBaseMinutes !== null">Pantalla: {{ child.screenBaseMinutes }} min/dia</span>
        <span v-else>Sense temps de pantalla configurat</span>
      </div>

      <form v-else class="child-card__form" @submit.prevent="save(child.childId)">
        <label>
          Paga mensual (€)
          <input v-model.number="form.monthlyAmount" type="number" min="0" step="0.5" required />
        </label>
        <label>
          % per gastar (la resta va a estalvi)
          <input v-model.number="form.spendingPercentage" type="number" min="0" max="100" required />
        </label>
        <label>
          Minuts de pantalla per dia
          <input v-model.number="form.baseMinutes" type="number" min="0" required />
        </label>
        <div class="child-card__form-actions">
          <BaseButton type="submit" variant="primary" :disabled="saving">{{ saving ? 'Desant…' : 'Desar' }}</BaseButton>
          <BaseButton type="button" variant="danger" :disabled="saving" @click="cancelEdit">Cancel·la</BaseButton>
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
