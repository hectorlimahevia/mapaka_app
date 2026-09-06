import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ExpenseResponse, PendingApprovalResponse } from '@/types/parent'

/** Compte de pendents compartit entre el badge de navegació i la vista d'Aprovacions,
 *  perquè s'actualitzi "en temps real" (Prompt 7) sense refetch complet de la pàgina.
 *  Inclou tant tasques com gastos pendents — totes dues coses necessiten la mateixa
 *  acció del pare (aprovar/rebutjar) abans de tenir efecte real. */
export const useApprovalsStore = defineStore('approvals', () => {
  const pendingCount = ref(0)

  async function refresh() {
    const auth = useAuthStore()
    if (!auth.familyId || auth.role !== 'PARENT') return
    const [tasks, expenses] = await Promise.all([
      api.get<PendingApprovalResponse[]>(`/api/families/${auth.familyId}/pending-approvals`),
      api.get<ExpenseResponse[]>(`/api/families/${auth.familyId}/pending-expenses`),
    ])
    pendingCount.value = tasks.data.length + expenses.data.length
  }

  function decrement(count = 1) {
    pendingCount.value = Math.max(0, pendingCount.value - count)
  }

  return { pendingCount, refresh, decrement }
})
