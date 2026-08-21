<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import BaseButton from '@/components/base/BaseButton.vue'
import { isNfcSupported, scanForScreenToken } from '@/services/nfc'
import { apiErrorMessage } from '@/utils/apiError'
import type { AssignSessionResponse, ChildSummary, ScreenSessionStatusResponse } from '@/types/nfc'

const { t } = useI18n()
const route = useRoute()
const token = route.params.token as string

// "Escanejar ara" és exclusiu d'Android empaquetat (Prompt 9) — la pantalla ha de
// funcionar igual de bé sense NFC actiu (patró passiu del Prompt 8).
const nfcAvailable = ref(false)
let cancelScan: (() => void) | null = null

onMounted(async () => {
  nfcAvailable.value = await isNfcSupported()
})

onUnmounted(() => cancelScan?.())

function scanNow() {
  if (loading.value) return
  errorMessage.value = null
  cancelScan?.()
  cancelScan = scanForScreenToken(
    (scannedToken) => tap(scannedToken),
    (message) => {
      errorMessage.value = message
    },
  )
}

type State = 'idle' | 'active' | 'whoplayed' | 'result'
const state = ref<State>('idle')
const loading = ref(false)
const errorMessage = ref<string | null>(null)

const sessionId = ref<string | null>(null)
const elapsedSeconds = ref(0)
const familyChildren = ref<ChildSummary[]>([])
const selectedChildIds = ref<Set<string>>(new Set())
const results = ref<AssignSessionResponse['participants']>([])

// Cronòmetre en viu mentre la sessió està ACTIVE — el backend no transmet temps,
// només compta els segons reals des que es va tocar l'objecte per iniciar.
let tickInterval: ReturnType<typeof setInterval> | null = null
const liveSeconds = ref(0)
const timerLabel = computed(() => {
  const mm = Math.floor(liveSeconds.value / 60).toString().padStart(2, '0')
  const ss = Math.floor(liveSeconds.value % 60).toString().padStart(2, '0')
  return `${mm}:${ss}`
})

function startTicking() {
  const start = Date.now()
  tickInterval = setInterval(() => {
    liveSeconds.value = Math.floor((Date.now() - start) / 1000)
  }, 200)
}

function stopTicking() {
  if (tickInterval) clearInterval(tickInterval)
  tickInterval = null
}

onUnmounted(stopTicking)

function applyStatus(data: ScreenSessionStatusResponse) {
  sessionId.value = data.sessionId
  if (data.status === 'ACTIVE') {
    state.value = 'active'
    liveSeconds.value = 0
    startTicking()
  } else {
    stopTicking()
    elapsedSeconds.value = data.elapsedSeconds ?? 0
    familyChildren.value = data.familyChildren ?? []
    selectedChildIds.value = new Set()
    state.value = 'whoplayed'
  }
}

async function tap(tagToken: string = token) {
  if (loading.value) return
  loading.value = true
  errorMessage.value = null
  try {
    const { data } = await api.post<ScreenSessionStatusResponse>(`/api/screen-tags/${tagToken}/tap`)
    applyStatus(data)
  } catch (err) {
    errorMessage.value = apiErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function stop() {
  if (loading.value || !sessionId.value) return
  loading.value = true
  try {
    const { data } = await api.post<ScreenSessionStatusResponse>(`/api/screen-sessions/${sessionId.value}/stop`)
    applyStatus(data)
  } finally {
    loading.value = false
  }
}

function toggleChild(childId: string) {
  const next = new Set(selectedChildIds.value)
  if (next.has(childId)) next.delete(childId)
  else next.add(childId)
  selectedChildIds.value = next
}

async function confirmSplit() {
  if (loading.value || !sessionId.value || selectedChildIds.value.size === 0) return
  loading.value = true
  try {
    const { data } = await api.post<AssignSessionResponse>(`/api/screen-sessions/${sessionId.value}/assign`, {
      childIds: [...selectedChildIds.value],
    })
    results.value = data.participants
    state.value = 'result'
  } finally {
    loading.value = false
  }
}

function chargedMinutes(seconds: number) {
  return seconds > 0 ? Math.max(1, Math.round(seconds / 60)) : 0
}

function reset() {
  sessionId.value = null
  elapsedSeconds.value = 0
  familyChildren.value = []
  selectedChildIds.value = new Set()
  results.value = []
  state.value = 'idle'
}
</script>

<template>
  <div class="screen-session">
    <div class="tablet-screen">
      <div v-if="state === 'idle'" class="tstate">
        <div class="tap-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <circle cx="12" cy="12" r="2.2" />
            <path d="M8.5 8.5a5 5 0 010 7" />
            <path d="M5.8 5.8a9 9 0 010 12.4" />
            <path d="M15.5 8.5a5 5 0 010 7" />
            <path d="M18.2 5.8a9 9 0 010 12.4" />
          </svg>
        </div>
        <h1 class="t-title">{{ t('nfc.tapPrompt') }}</h1>
        <p class="t-sub">{{ t('nfc.tapHint') }}</p>
        <p v-if="errorMessage" class="t-error">{{ errorMessage }}</p>
        <BaseButton variant="primary" :disabled="loading" @click="tap()">
          {{ loading ? t('nfc.waiting') : t('nfc.simulateOrStart') }}
        </BaseButton>
        <BaseButton v-if="nfcAvailable" variant="accent" :disabled="loading" class="t-scan-btn" @click="scanNow">
          {{ t('nfc.scanNow') }}
        </BaseButton>
      </div>

      <div v-else-if="state === 'active'" class="tstate">
        <p class="t-sub">{{ t('nfc.sessionRunning') }}</p>
        <div class="t-timer">{{ timerLabel }}</div>
        <p class="t-sub t-sub--tight">{{ t('nfc.notAssignedYet') }}</p>
        <BaseButton variant="danger" :disabled="loading" @click="stop">{{ t('nfc.stop') }}</BaseButton>
        <p class="t-hint">{{ t('nfc.tapAgainHint') }}</p>
      </div>

      <div v-else-if="state === 'whoplayed'" class="tstate">
        <h1 class="t-title">{{ t('nfc.whoPlayed') }}</h1>
        <p class="t-sub">{{ t('nfc.totalTime', { n: chargedMinutes(elapsedSeconds) }) }}</p>
        <div class="kid-select-row">
          <button
            v-for="child in familyChildren"
            :key="child.id"
            type="button"
            class="kid-chip"
            :class="{ 'kid-chip--selected': selectedChildIds.has(child.id) }"
            @click="toggleChild(child.id)"
          >
            {{ child.displayName }}
          </button>
        </div>
        <BaseButton variant="primary" :disabled="loading || selectedChildIds.size === 0" @click="confirmSplit">
          {{ t('nfc.confirmSplit') }}
        </BaseButton>
      </div>

      <div v-else class="tstate">
        <h1 class="t-title">{{ t('nfc.resultTitle') }}</h1>
        <p class="t-sub">{{ t('nfc.resultSubtitle') }}</p>
        <div class="result-list">
          <div v-for="p in results" :key="p.childId" class="result-row">
            <div>
              <div class="result-row__name">{{ p.displayName }}</div>
              <div v-if="p.negativeBalance" class="result-row__warn">
                {{ t('nfc.negativeBalanceWarning') }}
              </div>
            </div>
            <div class="result-row__min" :class="{ 'result-row__min--negative': p.negativeBalance }">
              -{{ chargedMinutes(p.assignedSeconds) }} {{ t('common.minutesAbbr') }}
            </div>
          </div>
        </div>
        <BaseButton variant="primary" @click="reset">{{ t('nfc.done') }}</BaseButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.screen-session {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 1.5rem;
}

.tablet-screen {
  width: 100%;
  max-width: 420px;
}

.tstate {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.4rem;
}

.tap-icon {
  color: var(--primary);
  margin-bottom: 0.6rem;
}

.tap-icon svg {
  width: 60px;
  height: 60px;
}

.t-title {
  margin-bottom: 0.2rem;
}

.t-sub {
  color: var(--muted);
  font-size: 0.9rem;
  margin: 0 0 1.25rem;
}

.t-sub--tight {
  margin-bottom: 0;
}

.t-error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: -0.5rem 0 1rem;
}

.t-scan-btn {
  margin-top: 0.6rem;
}

.t-timer {
  font-family: var(--font-body);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  font-size: 3rem;
  color: var(--primary);
  margin-bottom: 0.4rem;
}

.t-hint {
  font-size: 0.74rem;
  color: var(--muted);
  margin-top: 1rem;
}

.kid-select-row {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  justify-content: center;
}

.kid-chip {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.9rem;
  padding: 0.75rem 1.4rem;
  border-radius: 16px;
  border: 2px solid color-mix(in srgb, var(--primary) 15%, transparent);
  background: white;
  cursor: pointer;
  color: var(--text);
  transition: all 0.2s ease;
}

.kid-chip--selected {
  border-color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, white);
  color: var(--primary);
}

.result-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.result-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 0.75rem 1rem;
  box-shadow: 0 2px 8px -2px color-mix(in srgb, var(--text) 12%, transparent);
  text-align: left;
}

.result-row__name {
  font-weight: 700;
  font-size: 0.9rem;
}

.result-row__warn {
  font-size: 0.68rem;
  color: var(--error);
  margin-top: 0.15rem;
  max-width: 220px;
}

.result-row__min {
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  font-size: 0.95rem;
  white-space: nowrap;
}

.result-row__min--negative {
  color: var(--error);
}
</style>
