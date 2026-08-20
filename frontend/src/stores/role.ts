import { defineStore } from 'pinia'
import { ref } from 'vue'

export type UserRole = 'PARENT' | 'CHILD'

export const useRoleStore = defineStore('role', () => {
  const role = ref<UserRole | null>(null)

  function setRole(newRole: UserRole | null) {
    role.value = newRole
    if (newRole) {
      document.documentElement.dataset.role = newRole.toLowerCase()
    } else {
      delete document.documentElement.dataset.role
    }
  }

  return { role, setRole }
})
