<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useApprovalsStore } from '@/stores/approvals'
import { useChildTasksStore } from '@/stores/childTasks'
import { useViewport } from '@/composables/useViewport'
import BottomNav from './BottomNav.vue'
import SidebarNav from './SidebarNav.vue'
import TopBar from './TopBar.vue'

const { t } = useI18n()
const auth = useAuthStore()
const approvals = useApprovalsStore()
const childTasks = useChildTasksStore()
const { isMobile } = useViewport()

const childItems = computed(() => [
  { name: 'child-inici', label: t('nav.inici'), icon: 'home' as const },
  {
    name: 'child-tasques',
    label: t('nav.tasques'),
    icon: 'tasks' as const,
    badge: childTasks.availableCount,
    badgeColor: auth.avatarColor,
  },
  { name: 'child-objectius', label: t('nav.objectius'), icon: 'target' as const },
  { name: 'child-pantalla', label: t('nav.pantalla'), icon: 'device' as const },
])

const parentItems = computed(() => [
  { name: 'parent-resum', label: t('nav.resum'), icon: 'family' as const },
  { name: 'parent-tasques', label: t('nav.tasques'), icon: 'tasks' as const },
  { name: 'parent-aprovacions', label: t('nav.aprovacions'), icon: 'check' as const, badge: approvals.pendingCount },
  { name: 'parent-fills', label: t('nav.fills'), icon: 'child' as const },
  { name: 'parent-configuracio', label: t('nav.config'), icon: 'settings' as const },
])

const parentSidebarItems = computed(() => [
  { name: 'parent-resum', label: t('nav.resumFull'), icon: 'family' as const },
  { name: 'parent-tasques', label: t('nav.tasques'), icon: 'tasks' as const },
  { name: 'parent-aprovacions', label: t('nav.aprovacionsFull'), icon: 'check' as const, badge: approvals.pendingCount },
  { name: 'parent-fills', label: t('nav.fills'), icon: 'child' as const },
  { name: 'parent-configuracio', label: t('nav.configFull'), icon: 'settings' as const },
])

// CHILD sempre veu la barra inferior (no té variant d'escriptori). PARENT canvia a
// panell lateral per sobre de 768px — mateix AppShell, mateix patró de barra inferior
// per sota (mapaka_mockup.html: "Vista PARENT — mòbil" reutilitza el de CHILD).
const showSidebar = computed(() => auth.role === 'PARENT' && !isMobile.value)
const bottomNavItems = computed(() => (auth.role === 'PARENT' ? parentItems.value : childItems.value))

watch(
  () => auth.role,
  (role) => {
    if (role === 'PARENT') approvals.refresh()
    if (role === 'CHILD') childTasks.refresh()
  },
  { immediate: true },
)
onMounted(() => {
  if (auth.role === 'PARENT') approvals.refresh()
  if (auth.role === 'CHILD') childTasks.refresh()
})
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--sidebar': showSidebar, 'app-shell--bottom-nav': !showSidebar }">
    <SidebarNav v-if="showSidebar" :items="parentSidebarItems" />
    <TopBar v-if="!showSidebar" />

    <main class="app-shell__content">
      <RouterView v-slot="{ Component }">
        <Transition name="screen" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <BottomNav v-if="!showSidebar" :items="bottomNavItems" />
  </div>
</template>

<style scoped>
/* iOS treats `position: fixed` as relative to the page while the document itself
   scrolls, which is exactly what breaks TopBar/BottomNav on a real device (and worse
   in an installed PWA's standalone mode): they visibly drift with the scroll instead
   of staying pinned. Making the shell itself the fixed, full-viewport box — with only
   `.app-shell__content` scrolling internally — removes the document scroll those bugs
   depend on, so the bars never move regardless of platform or display mode. */
.app-shell {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-shell__content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.app-shell--sidebar .app-shell__content {
  margin-left: 240px;
}

.app-shell--bottom-nav .app-shell__content {
  padding-top: 3.5rem;
  /* BottomNav's own safe-area padding makes it taller on notched iPhones — match that
     here too, or content ends up hidden behind the bar instead of scrolled clear of it. */
  padding-bottom: calc(4.5rem + env(safe-area-inset-bottom));
}
</style>

<style>
/* No es pot fer scoped: la transició s'aplica a l'arrel del component enrutat,
   que viu en un altre àmbit d'estil (reprodueix el fade+slide de mapaka_mockup.html). */
.screen-enter-active,
.screen-leave-active {
  transition:
    opacity 0.32s ease,
    transform 0.32s ease;
}

.screen-enter-from,
.screen-leave-to {
  opacity: 0;
  transform: translateY(14px);
}
</style>
