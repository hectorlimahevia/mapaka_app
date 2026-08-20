import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import { useRoleStore } from '@/stores/role'
import type { AdultLoginRequest, ChildLoginRequest, AuthResponse, UserRole } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const roleStore = useRoleStore()

  const accessToken = ref<string | null>(null)
  const userId = ref<string | null>(null)
  const familyId = ref<string | null>(null)
  const role = ref<UserRole | null>(null)
  const childId = ref<string | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => accessToken.value !== null)

  function applyAuthResponse(data: AuthResponse) {
    accessToken.value = data.accessToken
    userId.value = data.userId
    familyId.value = data.familyId
    role.value = data.role
    childId.value = data.childId
    roleStore.setRole(data.role)
  }

  function clearSession() {
    accessToken.value = null
    userId.value = null
    familyId.value = null
    role.value = null
    childId.value = null
    roleStore.setRole(null)
  }

  async function loginAdult(payload: AdultLoginRequest) {
    const { data } = await api.post<AuthResponse>('/api/auth/login', payload)
    applyAuthResponse(data)
  }

  async function loginChild(payload: ChildLoginRequest) {
    const { data } = await api.post<AuthResponse>('/api/auth/login', payload)
    applyAuthResponse(data)
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
    initialized,
    isAuthenticated,
    loginAdult,
    loginChild,
    refresh,
    tryRestoreSession,
    clearSession,
    logout,
  }
})
