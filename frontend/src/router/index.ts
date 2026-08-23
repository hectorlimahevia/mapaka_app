import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppShell from '@/components/shell/AppShell.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/registre',
      name: 'register-family',
      component: () => import('@/views/RegisterFamilyView.vue'),
      meta: { public: true },
    },
    {
      path: '/recuperar',
      name: 'recover-request',
      component: () => import('@/views/RecoverRequestView.vue'),
      meta: { public: true },
    },
    {
      path: '/recuperar/nou-pin',
      name: 'recover-set-pin',
      component: () => import('@/views/RecoverSetPinView.vue'),
      meta: { public: true },
    },
    {
      // URL gravada a l'etiqueta NFC física — sense login, identifica la família pel token.
      path: '/screen/:token',
      name: 'screen-session',
      component: () => import('@/views/ScreenSessionView.vue'),
      meta: { public: true },
    },
    {
      path: '/child',
      component: AppShell,
      meta: { role: 'CHILD' },
      children: [
        { path: '', redirect: { name: 'child-inici' } },
        { path: 'inici', name: 'child-inici', component: () => import('@/views/child/ChildIniciView.vue') },
        { path: 'tasques', name: 'child-tasques', component: () => import('@/views/child/ChildTasquesView.vue') },
        { path: 'objectius', name: 'child-objectius', component: () => import('@/views/child/ChildObjectiusView.vue') },
        { path: 'pantalla', name: 'child-pantalla', component: () => import('@/views/child/ChildPantallaView.vue') },
      ],
    },
    {
      path: '/parent',
      component: AppShell,
      meta: { role: 'PARENT' },
      children: [
        { path: '', redirect: { name: 'parent-resum' } },
        { path: 'resum', name: 'parent-resum', component: () => import('@/views/parent/ParentResumView.vue') },
        { path: 'tasques', name: 'parent-tasques', component: () => import('@/views/parent/ParentTasquesView.vue') },
        { path: 'aprovacions', name: 'parent-aprovacions', component: () => import('@/views/parent/ParentAprovacionsView.vue') },
        { path: 'fills', name: 'parent-fills', component: () => import('@/views/parent/ParentFillsView.vue') },
        { path: 'configuracio', name: 'parent-configuracio', component: () => import('@/views/parent/ParentConfiguracioView.vue') },
        { path: 'etiquetes-nfc', name: 'parent-nfc-tags', component: () => import('@/views/parent/ParentNfcTagsView.vue') },
        { path: 'resums-mensuals', name: 'parent-settlements', component: () => import('@/views/parent/ParentSettlementsView.vue') },
      ],
    },
    { path: '/', redirect: { name: 'login' } },
    { path: '/:pathMatch(.*)*', redirect: { name: 'login' } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) {
    await auth.tryRestoreSession()
  }

  const home = () => ({ name: auth.role === 'PARENT' ? 'parent-resum' : 'child-inici' })

  if (to.meta.public) {
    return auth.isAuthenticated && to.name === 'login' ? home() : true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.meta.role && to.meta.role !== auth.role) {
    return home()
  }

  return true
})

export default router
