import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/LandingPage.vue'),
    },
    {
      path: '/auth',
      name: 'auth',
      component: () => import('@/views/auth/AuthView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },
    {
      path: '/triagem',
      name: 'triagem',
      component: () => import('@/views/dashboard/TriagemView.vue'),
      meta: { requiresAuth: true, noSidebar: true, userOnly: true },
    },
    {
      path: '/perfil',
      name: 'perfil',
      component: () => import('@/views/dashboard/PerfilView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },
    {
      path: '/perfil/editar',
      name: 'editar-perfil',
      component: () => import('@/views/dashboard/EditarPerfilView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },
    {
      path: '/evolucao',
      name: 'evolucao',
      component: () => import('@/views/dashboard/EvolucaoView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },
    {
      path: '/dieta',
      name: 'dieta',
      component: () => import('@/views/dashboard/DietaView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },
    {
      path: '/efetivarRefeicao',
      name: 'dieta-diaria',
      component: () => import('@/views/dashboard/DietaDiariaView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },
    {
      path: '/historico',
      name: 'historico',
      component: () => import('@/views/dashboard/HistoricoView.vue'),
      meta: { requiresAuth: true, userOnly: true },
    },

    // ----- ADMIN
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: () => import('@/views/admin/DashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, noSidebar: true },
    },

    // ----- 404
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

// Proteção de rotas
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'auth' }
  }

  if (to.meta.requiresAdmin) {
    if (!authStore.isAuthenticated) return { name: 'auth' }
    if (!authStore.isAdmin) return { name: 'dashboard' }
  }

  if (to.meta.userOnly && authStore.isAuthenticated && authStore.isAdmin) {
    return { name: 'admin-dashboard' }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return authStore.isAdmin ? { name: 'admin-dashboard' } : { name: 'dashboard' }
  }
})

export default router
