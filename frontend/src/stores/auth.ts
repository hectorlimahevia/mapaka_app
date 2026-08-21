import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import { useRoleStore } from '@/stores/role'
import type { PinLoginRequest, AuthResponse, FamilyRegisterResponse, UserRole } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const roleStore = useRoleStore()

  const accessToken = ref<string | null>(null)
  const userId = ref<string | null>(null)
  const familyId = ref<string | null>(null)
  const role = ref<UserRole | null>(null)
  const childId = ref<string | null>(null)
  const displayName = ref<string | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => accessToken.value !== null)

  function applyAuthResponse(data: AuthResponse) {
    accessToken.value = data.accessToken
    userId.value = data.userId
    familyId.value = data.familyId
    role.value = data.role
    childId.value = data.childId
    displayName.value = data.displayName
    roleStore.setRole(data.role)
  }

  function clearSession() {
    accessToken.value = null
    userId.value = null
    familyId.value = null
    role.value = null
    childId.value = null
    displayName.value = null
    roleStore.setRole(null)
  }

  /** Família + perfil + PIN — mateix flux per a PARENT i CHILD des del Prompt 6. */
  async function login(payload: PinLoginRequest) {
    const { data } = await api.post<AuthResponse>('/api/auth/login', payload)
    applyAuthResponse(data)
  }

  /** L'assistent de registre ja retorna sessió iniciada (auth) perquè es pugui continuar
   *  afegint fills sense haver de tornar a entrar. */
  function applyRegisterResponse(result: FamilyRegisterResponse) {
    applyAuthResponse(result.auth)
  }

  /** Access token només en memòria (mai localStorage). En recarregar la pàgina es recupera
   *  la sessió de forma silenciosa a partir de la cookie HttpOnly de refresh, si existeix. */
  async function refresh(): Promise<boolean> {
    try {
      const { data } = await api.post<AuthResponse>('/api/auth/refresh')
      applyAuthResponse(data)
      return true
    } catch {
      return false
    }
  }

  async function tryRestoreSession() {
    if (initialized.value) return
    await refresh()
    initialized.value = true
  }

  async function logout() {
    try {
      await api.post('/api/auth/logout')
    } finally {
      clearSession()
    }
  }

  return {
    accessToken,
    userId,
    familyId,
    role,
    childId,
    displayName,
    initialized,
    isAuthenticated,
    login,
    applyRegisterResponse,
    refresh,
    tryRestoreSession,
    clearSession,
    logout,
  }
})
