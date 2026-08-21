<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { isNativePlatform, writeScreenUrlToTag } from '@/services/nfc'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import type { ScreenTagResponse } from '@/types/parent'

const auth = useAuthStore()
const tags = ref<ScreenTagResponse[]>([])
const loading = ref(true)
const creating = ref(false)
const writingTagId = ref<string | null>(null)
const writeMessage = ref<string | null>(null)
const nativeApp = isNativePlatform()

let cancelWrite: (() => void) | null = null

async function load() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<ScreenTagResponse[]>(`/api/families/${familyId}/screen-tags`)
  tags.value = data
  loading.value = false
}

async function createTag() {
  const familyId = auth.familyId
  if (!familyId || creating.value) return
  creating.value = true
  try {
    const { data } = await api.post<ScreenTagResponse>(`/api/families/${familyId}/screen-tags`)
    tags.value = [data, ...tags.value]
  } finally {
    creating.value = false
  }
}

function writeTag(tag: ScreenTagResponse) {
  if (writingTagId.value) return
  writingTagId.value = tag.id
  writeMessage.value = "Apropa l'etiqueta NFC en blanc al dispositiu…"
  cancelWrite?.()
  cancelWrite = writeScreenUrlToTag(
    tag.url,
    () => {
      writeMessage.value = 'Etiqueta escrita correctament!'
      writingTagId.value = null
    },
    (message) => {
      writeMessage.value = message
      writingTagId.value = null
    },
  )
}

onMounted(load)
</script>

<template>
  <div class="nfc-tags">
    <h1>Etiquetes NFC</h1>
    <p class="nfc-tags__sub">
      Vincula un objecte físic amb una etiqueta NFC a la sessió de temps de pantalla compartida.
    </p>

    <p v-if="!nativeApp" class="nfc-tags__hint">
      Escriure etiquetes des de l'app només és possible a la versió Android instal·lada — aquí pots crear
      l'etiqueta i copiar la URL per gravar-la amb una altra eina.
    </p>

    <BaseButton variant="primary" :disabled="creating" @click="createTag">
      {{ creating ? 'Creant…' : 'Crear etiqueta nova' }}
    </BaseButton>

    <p v-if="!loading && tags.length === 0" class="nfc-tags__empty">Encara no hi ha cap etiqueta registrada.</p>

    <BaseCard v-for="tag in tags" :key="tag.id" class="tag-card">
      <div class="tag-card__token">{{ tag.token }}</div>
      <div class="tag-card__url">{{ tag.url }}</div>
      <BaseButton
        v-if="nativeApp"
        variant="accent"
        :disabled="writingTagId !== null"
        @click="writeTag(tag)"
      >
        {{ writingTagId === tag.id ? 'Esperant etiqueta…' : 'Escriure a una etiqueta' }}
      </BaseButton>
    </BaseCard>

    <p v-if="writeMessage" class="nfc-tags__write-status">{{ writeMessage }}</p>
  </div>
</template>

<style scoped>
.nfc-tags {
  max-width: 560px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.nfc-tags__sub {
  color: var(--muted);
  font-size: 0.88rem;
  margin: 0 0 1rem;
}

.nfc-tags__hint {
  background: color-mix(in srgb, var(--accent) 15%, white);
  border-radius: 12px;
  padding: 0.75rem 0.9rem;
  font-size: 0.82rem;
  margin-bottom: 1rem;
}

.nfc-tags__empty {
  color: var(--muted);
  font-size: 0.85rem;
  margin-top: 1rem;
}

.tag-card {
  margin-top: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
}

.tag-card__token {
  font-family: var(--font-heading);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.tag-card__url {
  font-size: 0.76rem;
  color: var(--muted);
  word-break: break-all;
}

.nfc-tags__write-status {
  margin-top: 1rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--primary);
}
</style>
