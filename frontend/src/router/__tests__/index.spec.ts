// @vitest-environment jsdom
/**
 * Route guard RBAC tests — router/index.ts `beforeEach`
 *
 * Strategy: use the real router instance so the actual guard logic is exercised.
 * All lazy-loaded view components are stubbed out to avoid import resolution errors.
 * `useAuthStore` is mocked via vi.hoisted() so the state object is available inside
 * the vi.mock factory even after Vitest's automatic hoisting transformation.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'

// ── Auth store mock ───────────────────────────────────────────────────────────
// vi.hoisted() runs before module evaluation, guaranteeing the object is defined
// when the vi.mock factory closure is invoked after hoisting.
const authState = vi.hoisted(() => ({
  isAuthenticated: false,
  isAdmin: false,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authState,
}))

// ── Lazy-loaded component stubs ───────────────────────────────────────────────
// The real router defines all routes with dynamic imports. We stub every view
// module so Vite does not attempt to resolve them during tests.
vi.mock('@/views/LandingPage.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/auth/AuthView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/DashboardView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/TriagemView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/PerfilView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/EditarPerfilView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/EvolucaoView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/DietaView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/DietaDiariaView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/dashboard/HistoricoView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/admin/DashboardView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/NotFoundView.vue', () => ({ default: { template: '<div />' } }))

// ── Router import — AFTER mocks ───────────────────────────────────────────────
import router from '@/router'

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Resets auth state to unauthenticated guest before each test. */
function setGuest(): void {
  authState.isAuthenticated = false
  authState.isAdmin = false
}

function setUser(): void {
  authState.isAuthenticated = true
  authState.isAdmin = false
}

function setAdmin(): void {
  authState.isAuthenticated = true
  authState.isAdmin = true
}

/**
 * Navigate to `target` and return the resolved route name.
 * Starts from `/` so the router is always in a known state before asserting.
 */
async function navigateTo(target: string): Promise<string | null | undefined> {
  // Return to a neutral route that has no guard restrictions.
  await router.push('/')
  await router.isReady()

  await router.push(target)
  await router.isReady()

  return router.currentRoute.value.name as string | null | undefined
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('router guard — RBAC', () => {
  beforeEach(() => {
    setGuest()
  })

  it('(caso 1) ADMIN autenticado acessando rota userOnly é redirecionado para admin-dashboard', async () => {
    // given: usuário autenticado com papel ADMIN
    setAdmin()

    // when: tenta navegar para /dashboard (meta: requiresAuth + userOnly)
    const resolvedName = await navigateTo('/dashboard')

    // then: guard redireciona para admin-dashboard, não permite acesso
    expect(resolvedName).toBe('admin-dashboard')
  })

  it('(caso 2) USER autenticado acessando rota userOnly tem acesso permitido', async () => {
    // given: usuário autenticado com papel USER
    setUser()

    // when: navega para /dashboard (meta: requiresAuth + userOnly)
    const resolvedName = await navigateTo('/dashboard')

    // then: guard não bloqueia — rota resolve normalmente
    expect(resolvedName).toBe('dashboard')
  })

  it('(caso 3) ADMIN autenticado acessando rota requiresAdmin tem acesso permitido', async () => {
    // given: usuário autenticado com papel ADMIN
    setAdmin()

    // when: navega para /admin/dashboard (meta: requiresAuth + requiresAdmin)
    const resolvedName = await navigateTo('/admin/dashboard')

    // then: guard não bloqueia — rota resolve normalmente
    expect(resolvedName).toBe('admin-dashboard')
  })

  it('(caso 4 — regressão) guest acessando rota requiresAuth é redirecionado para auth', async () => {
    // given: usuário não autenticado (guest)
    setGuest()

    // when: tenta acessar /dashboard (meta: requiresAuth + userOnly)
    const resolvedName = await navigateTo('/dashboard')

    // then: guard redireciona para a tela de autenticação
    expect(resolvedName).toBe('auth')
  })

  it('(bônus) USER autenticado em rota guestOnly é redirecionado para dashboard', async () => {
    // given: usuário USER já autenticado
    setUser()

    // when: tenta acessar /auth (meta: guestOnly)
    const resolvedName = await navigateTo('/auth')

    // then: guard redireciona para dashboard, pois usuário já está logado
    expect(resolvedName).toBe('dashboard')
  })

  it('(bônus) ADMIN autenticado em rota guestOnly é redirecionado para admin-dashboard', async () => {
    // given: usuário ADMIN já autenticado
    setAdmin()

    // when: tenta acessar /auth (meta: guestOnly)
    const resolvedName = await navigateTo('/auth')

    // then: guard redireciona para admin-dashboard
    expect(resolvedName).toBe('admin-dashboard')
  })

  it('(bônus) guest acessando rota requiresAdmin é redirecionado para auth', async () => {
    // given: usuário não autenticado
    setGuest()

    // when: tenta acessar /admin/dashboard
    const resolvedName = await navigateTo('/admin/dashboard')

    // then: guard redireciona para auth (caminho requiresAuth dentro de requiresAdmin)
    expect(resolvedName).toBe('auth')
  })

  it('(bônus) USER autenticado acessando rota requiresAdmin é redirecionado para dashboard', async () => {
    // given: usuário USER autenticado (sem papel ADMIN)
    setUser()

    // when: tenta acessar /admin/dashboard
    const resolvedName = await navigateTo('/admin/dashboard')

    // then: guard redireciona para dashboard (autenticado mas sem permissão de admin)
    expect(resolvedName).toBe('dashboard')
  })
})
