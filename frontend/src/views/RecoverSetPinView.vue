<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useRecoveryStore } from '@/stores/recovery'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import MapakaLogo from '@/components/base/MapakaLogo.vue'

const router = useRouter()
const recovery = useRecoveryStore()

const loading = ref(false)
const error = ref<string | null>(null)
const done = ref(false)
const pin = ref('')
const pinConfirm = ref('')

onMounted(() => {
  if (!recovery.recoveryToken) {
    router.replace({ name: 'recover-request' })
  }
})

async function submit() {
  error.value = null
  if (!/^\d{4}$/.test(pin.value)) {
    error.value = 'El PIN ha de tenir exactament 4 dígits.'
    return
  }
  if (pin.value !== pinConfirm.value) {
    error.value = 'Els PIN no coincideixen.'
    return
  }
  loading.value = true
  try {
    await api.post('/api/auth/recover/reset-pin', {
      recoveryToken: recovery.recoveryToken,
      newPin: pin.value,
    })
    recovery.clear()
    done.value = true
  } catch {
    error.value = 'El token ha caducat. Torna a demanar la recuperació.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="set-pin">
    <MapakaLogo class="set-pin__brand" />

    <BaseCard class="set-pin__card">
      <template v-if="done">
        <p class="set-pin__prompt">PIN actualitzat ✓</p>
        <BaseButton type="button" variant="primary" @click="router.push({ name: 'login' })">
          Anar a l'inici de sessió
        </BaseButton>
      </template>
      <form v-else class="set-pin__form" @submit.prevent="submit">
        <p class="set-pin__prompt">Defineix un PIN nou</p>
        <label>
          PIN de 4 dígits
          <input v-model="pin" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required autofocus />
        </label>
        <label>
          Confirma el PIN
          <input v-model="pinConfirm" type="password" inputmode="numeric" pattern="[0-9]*" maxlength="4" required />
        </label>
        <p v-if="error" class="set-pin__error">{{ error }}</p>
        <BaseButton type="submit" variant="primary" :disabled="loading">
          {{ loading ? 'Desant…' : 'Desar PIN nou' }}
        </BaseButton>
      </form>
    </BaseCard>
  </div>
</template>

<style scoped>
.set-pin {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  padding: 2rem 1.5rem;
}

.set-pin__card {
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.set-pin__prompt {
  font-family: var(--font-heading);
  font-weight: 700;
  margin: 0;
}

.set-pin__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.set-pin__form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 700;
  font-size: 0.9rem;
}

.set-pin__form input {
  font: inherit;
  padding: 0.65rem 0.85rem;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
  background: white;
}

.set-pin__error {
  color: var(--error);
  font-size: 0.85rem;
  font-weight: 700;
  margin: 0;
}
</style>
