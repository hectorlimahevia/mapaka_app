<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import ChildScreenHeader from '@/components/base/ChildScreenHeader.vue'
import type { ScreenTimeStatusResponse } from '@/types/child'

const { t } = useI18n()
const auth = useAuthStore()
const status = ref<ScreenTimeStatusResponse>({ baseMinutes: 0, availableMinutes: 0 })
const dashOffset = ref(490)

const CIRCUMFERENCE = 490

onMounted(async () => {
  const childId = auth.childId
  if (!childId) return
  const { data } = await api.get<ScreenTimeStatusResponse>(`/api/children/${childId}/screen-time/today`)
  status.value = data

  const fraction = data.baseMinutes > 0 ? Math.min(1, Math.max(0, data.availableMinutes / data.baseMinutes)) : 0
  requestAnimationFrame(() =>
    requestAnimationFrame(() => {
      dashOffset.value = CIRCUMFERENCE * (1 - fraction)
    }),
  )
})
</script>

<template>
  <div class="pantalla">
    <ChildScreenHeader :title="t('pantalla.title')" />
    <p class="pantalla__sub">{{ t('pantalla.subtitle') }}</p>

    <div class="ring-wrap">
      <div class="ring-wrap__circle-box">
        <svg width="180" height="180" viewBox="0 0 180 180">
          <circle cx="90" cy="90" r="78" fill="none" stroke="color-mix(in srgb, var(--primary) 12%, transparent)" stroke-width="14" />
          <circle
            cx="90"
            cy="90"
            r="78"
            fill="none"
            stroke="url(#gradRing)"
            stroke-width="14"
            stroke-linecap="round"
            :stroke-dasharray="CIRCUMFERENCE"
            :stroke-dashoffset="dashOffset"
            transform="rotate(-90 90 90)"
            class="ring-wrap__circle"
          />
          <defs>
            <linearGradient id="gradRing" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="var(--primary)" />
              <stop offset="100%" stop-color="var(--secondary)" />
            </linearGradient>
          </defs>
        </svg>
        <div class="ring-wrap__value" :class="{ 'ring-wrap__value--negative': status.availableMinutes < 0 }">
          {{ status.availableMinutes }}
        </div>
      </div>
      <div class="ring-wrap__below">
        <b>{{ t('pantalla.availableMinutes', { n: status.availableMinutes }) }}</b>
        <span>{{ t('pantalla.assignedCaption', { n: status.baseMinutes }) }}</span>
      </div>
    </div>

    <div class="nfc-hint">
      <svg class="nfc-hint__icon" viewBox="0 0 24 24">
        <rect x="6" y="3" width="12" height="18" rx="2" />
        <line x1="12" y1="17.5" x2="12.01" y2="17.5" />
      </svg>
      <p>{{ t('pantalla.nfcHint') }}</p>
    </div>
  </div>
</template>

<style scoped>
.pantalla {
  max-width: 480px;
  margin: 0 auto;
  padding: 1.5rem 1.25rem 2rem;
}

.pantalla__sub {
  color: var(--muted);
  font-size: 0.85rem;
  margin: 0 0 1rem;
}

.ring-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 1rem 0 0.5rem;
}

.ring-wrap__circle-box {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ring-wrap__circle {
  transition: stroke-dashoffset 1.1s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.ring-wrap__value {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
  font-weight: 900;
  font-size: 1.6rem;
  font-variant-numeric: tabular-nums;
}

.ring-wrap__value--negative {
  color: var(--error);
}

.ring-wrap__below {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  margin-top: 0.9rem;
  font-size: 0.85rem;
}

.ring-wrap__below span {
  font-size: 0.74rem;
  color: var(--muted);
}

.nfc-hint {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background: color-mix(in srgb, var(--accent) 15%, white);
  border-radius: 14px;
  padding: 0.85rem 1rem;
  margin-top: 1rem;
  font-size: 0.82rem;
  color: var(--text);
}

.nfc-hint p {
  margin: 0;
}

.nfc-hint__icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
