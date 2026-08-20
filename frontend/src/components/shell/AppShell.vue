<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useViewport } from '@/composables/useViewport'
import BottomNav from './BottomNav.vue'
import SidebarNav from './SidebarNav.vue'

const auth = useAuthStore()
const { isMobile } = useViewport()

const parentItems = [
  { name: 'parent-resum', label: 'Resum familiar', icon: 'family' as const },
  { name: 'parent-aprovacions', label: 'Aprovacions', icon: 'check' as const },
  { name: 'parent-fills', label: 'Fills', icon: 'child' as const },
  { name: 'parent-configuracio', label: 'Configuració', icon: 'settings' as const },
]

const childSidebarItems = [
  { name: 'child-inici', label: 'Inici', icon: 'home' as const },
  { name: 'child-tasques', label: 'Tasques', icon: 'tasks' as const },
  { name: 'child-objectius', label: 'Objectius', icon: 'target' as const },
  { name: 'child-pantalla', label: 'Pantalla', icon: 'device' as const },
]

// Sidebar per sobre de 768px sempre, i sempre per a PARENT independentment de la mida.
const showSidebar = computed(() => auth.role === 'PARENT' || !isMobile.value)
const showBottomNav = computed(() => auth.role === 'CHILD' && isMobile.value)
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--sidebar': showSidebar, 'app-shell--bottom-nav': showBottomNav }">
    <SidebarNav v-if="showSidebar" :items="auth.role === 'PARENT' ? parentItems : childSidebarItems" />

    <main class="app-shell__content">
      <RouterView />
    </main>

    <BottomNav v-if="showBottomNav" />
  </div>
</template>

<style scoped>
.app-shell__content {
  min-height: 100vh;
}

.app-shell--sidebar .app-shell__content {
  margin-left: 240px;
}

.app-shell--bottom-nav .app-shell__content {
  padding-bottom: 4.5rem;
}
</style>
