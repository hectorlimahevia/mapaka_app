<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { FamilySettings } from '@/types/parent'

const auth = useAuthStore()
const loading = ref(true)
const settings = reactive<FamilySettings>({
  taskApprovalRequired: true,
  notifyPendingApprovalsEnabled: false,
  allowSavingsTransfer: true,
})

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<FamilySettings>(`/api/families/${familyId}/settings`)
  Object.assign(settings, data)
  loading.value = false
}

async function toggle(key: keyof FamilySettings) {
  if (loading.value) return
  settings[key] = !settings[key]
  const familyId = auth.familyId
  await api.patch(`/api/families/${familyId}/settings`, settings)
}

onMounted(load)
</script>

<template>
  <div class="config">
    <h1>Configuració</h1>
    <p class="config__sub">Regles generals de paga, estalvi i pantalla</p>

    <div class="settings-row">
      <span>Aprovació obligatòria per a totes les recompenses</span>
      <button
        type="button"
        class="switch"
        :class="{ 'switch--on': settings.taskApprovalRequired }"
        role="switch"
        :aria-checked="settings.taskApprovalRequired"
        @click="toggle('taskApprovalRequired')"
      />
    </div>
    <div class="settings-row">
      <span>Notificar per correu les aprovacions pendents</span>
      <button
        type="button"
        class="switch"
        :class="{ 'switch--on': settings.notifyPendingApprovalsEnabled }"
        role="switch"
        :aria-checked="settings.notifyPendingApprovalsEnabled"
        @click="toggle('notifyPendingApprovalsEnabled')"
      />
    </div>
    <div class="settings-row">
      <span>Permetre transferència disponible → estalvi</span>
      <button
        type="button"
        class="switch"
        :class="{ 'switch--on': settings.allowSavingsTransfer }"
        role="switch"
        :aria-checked="settings.allowSavingsTransfer"
        @click="toggle('allowSavingsTransfer')"
      />
    </div>
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

.switch {
  width: 38px;
  height: 22px;
  background: color-mix(in srgb, var(--text) 12%, transparent);
  border-radius: 999px;
  position: relative;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s ease;
}

.switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background: white;
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.switch--on {
  background: var(--primary);
}

.switch--on::after {
  transform: translateX(16px);
}
</style>
