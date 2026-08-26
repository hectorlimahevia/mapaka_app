import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ChildTaskResponse } from '@/types/child'

/** Comptador de tasques assignades encara no marcades com a fetes (AVAILABLE), per al
 * badge de l'ítem "Tasques" de la nav de CHILD (Prompt 15, checklist 28). */
export const useChildTasksStore = defineStore('childTasks', () => {
  const availableCount = ref(0)

  async function refresh() {
    const auth = useAuthStore()
    if (!auth.childId || auth.role !== 'CHILD') return
    const { data } = await api.get<ChildTaskResponse[]>(`/api/children/${auth.childId}/tasks`)
    availableCount.value = data.filter((t) => t.status === 'AVAILABLE').length
  }

  return { availableCount, refresh }
})
