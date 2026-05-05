import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/auth',
      name: 'auth',
      component: () => import('../views/auth/AuthView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/dashboard/DashboardView.vue'),
      meta: { requiresAuth: true },
    },
    // ---- NOVAS ROTAS ADICIONADAS ABAIXO ----
    {
      path: '/triagem',
      name: 'triagem',
      component: () => import('../views/dashboard/TriagemView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/perfil',
      name: 'perfil',
      component: () => import('../views/dashboard/PerfilView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/perfil/editar',
      name: 'editar-perfil',
      component: () => import('../views/dashboard/EditarPerfilView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/evolucao',
      name: 'evolucao',
      component: () => import('../views/dashboard/EvolucaoView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/dieta',
      name: 'dieta',
      component: () => import('../views/dashboard/DietaView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/efetivarRefeicao',
      name: 'dieta-diaria',
      component: () => import('../views/dashboard/DietaDiariaView.vue'),
      meta: { requiresAuth: true },
    }
  ],
})

// Proteção de rotas (impede usuário deslogado de acessar o dashboard)
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'auth' }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }
})

export default router