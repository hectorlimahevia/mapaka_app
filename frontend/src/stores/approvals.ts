import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { PendingApprovalResponse } from '@/types/parent'

/** Compte de pendents compartit entre el badge de navegació i la vista d'Aprovacions,
 *  perquè s'actualitzi "en temps real" (Prompt 7) sense refetch complet de la pàgina. */
export const useApprovalsStore = defineStore('approvals', () => {
  const pendingCount = ref(0)

  async function refresh() {
    const auth = useAuthStore()
    if (!auth.familyId || auth.role !== 'PARENT') return
    const { data } = await api.get<PendingApprovalResponse[]>(`/api/families/${auth.familyId}/pending-approvals`)
    pendingCount.value = data.length
  }

  function decrement(count = 1) {
    pendingCount.value = Math.max(0, pendingCount.value - count)
  }

  return { pendingCount, refresh, decrement }
})
