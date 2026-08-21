import { defineStore } from 'pinia'
import { ref } from 'vue'

/** Token temporal de POST /api/auth/recover — només en memòria, mai persistit (mateix
 *  criteri que l'access token a stores/auth.ts). */
export const useRecoveryStore = defineStore('recovery', () => {
  const recoveryToken = ref<string | null>(null)

  function setToken(token: string) {
    recoveryToken.value = token
  }

  function clear() {
    recoveryToken.value = null
  }

  return { recoveryToken, setToken, clear }
})
