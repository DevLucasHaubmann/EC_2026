// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/services/modules/me', () => ({
  meService: { getMe: vi.fn() },
}))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isAdmin: false }),
}))

// me store is driven by meService.getMe; we control it via the service mock.
// We also need a real Pinia instance so the store is reactive across tests.
import { createPinia, setActivePinia } from 'pinia'
import AppSidebar from '@/components/AppSidebar.vue'
import { meService } from '@/services/modules/me'
import type { MeResponse } from '@/types/api'

// Minimal router so RouterLink resolves without warnings.
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'dashboard',    component: { template: '<div/>' } },
    { path: '/dieta',       name: 'dieta',        component: { template: '<div/>' } },
    { path: '/efetivarRefeicao', name: 'dieta-diaria', component: { template: '<div/>' } },
    { path: '/triagem',     name: 'triagem',      component: { template: '<div/>' } },
    { path: '/evolucao',    name: 'evolucao',     component: { template: '<div/>' } },
    { path: '/historico',   name: 'historico',    component: { template: '<div/>' } },
    { path: '/perfil',      name: 'perfil',       component: { template: '<div/>' } },
    { path: '/auth',        name: 'auth',         component: { template: '<div/>' } },
  ],
})

function meWith(profile: MeResponse['profile'], assessment: MeResponse['assessment']): MeResponse {
  return {
    id: 1,
    name: 'Tester',
    email: 'tester@tukan.test',
    type: 'USER',
    status: 'ACTIVE',
    avatarUrl: null,
    profile,
    assessment,
  }
}

const PROFILE = {
  id: 1, userId: 1, userName: 'Tester',
  dateOfBirth: '1990-01-01', gender: 'MALE' as const,
  weightKg: 70, heightCm: 175, activityLevel: 'MODERATE' as const,
  createdAt: '2024-01-01T00:00:00Z', updatedAt: null,
}

const ASSESSMENT = {
  id: 1, userId: 1, userName: 'Tester',
  goal: 'WEIGHT_LOSS' as const, dietType: 'ONIVORA' as const,
  dietaryRestrictions: null, allergies: null, healthConditions: null,
  mealsPerDay: 4, targetWeightKg: null,
  createdAt: '2024-01-01T00:00:00Z', updatedAt: null,
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('AppSidebar — triagem visibility and wip badge', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Fresh Pinia per test so the me store state does not bleed between cases.
    setActivePinia(createPinia())
  })

  it('shows the Triagem link when onboarding is incomplete (no profile, no assessment)', async () => {
    // given a user with neither profile nor assessment
    vi.mocked(meService.getMe).mockResolvedValue(meWith(null, null))

    const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
    await flushPromises()

    // then the Triagem link is rendered
    const links = wrapper.findAll('.nav-link')
    expect(links.some((l) => l.text() === 'Triagem')).toBe(true)
  })

  it('shows the Triagem link when profile exists but assessment does not (onboarding incomplete)', async () => {
    // given a user with a profile but no assessment
    vi.mocked(meService.getMe).mockResolvedValue(meWith(PROFILE, null))

    const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
    await flushPromises()

    const links = wrapper.findAll('.nav-link')
    expect(links.some((l) => l.text() === 'Triagem')).toBe(true)
  })

  // Mirrors backend AssessmentCompletenessPolicy: a stale/incomplete assessment
  // (missing goal, dietType or mealsPerDay) does NOT complete onboarding, so Triagem stays.
  const INCOMPLETE_ASSESSMENTS: Array<[string, MeResponse['assessment']]> = [
    ['missing goal', { ...ASSESSMENT, goal: null as unknown as typeof ASSESSMENT.goal }],
    ['missing dietType', { ...ASSESSMENT, dietType: null }],
    ['missing mealsPerDay', { ...ASSESSMENT, mealsPerDay: null }],
  ]

  it.each(INCOMPLETE_ASSESSMENTS)(
    'shows the Triagem link when profile exists but assessment is incomplete (%s)',
    async (_label, assessment) => {
      // given a profile and an assessment that lacks a generation-required field
      vi.mocked(meService.getMe).mockResolvedValue(meWith(PROFILE, assessment))

      const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
      await flushPromises()

      // then onboarding is NOT complete and the Triagem link stays visible
      const links = wrapper.findAll('.nav-link')
      expect(links.some((l) => l.text() === 'Triagem')).toBe(true)
    },
  )

  it('hides the Triagem link when onboarding is complete (profile + complete assessment)', async () => {
    // given a user who has both profile and assessment
    vi.mocked(meService.getMe).mockResolvedValue(meWith(PROFILE, ASSESSMENT))

    const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
    await flushPromises()

    // then Triagem is not rendered
    const links = wrapper.findAll('.nav-link')
    expect(links.some((l) => l.text() === 'Triagem')).toBe(false)
  })

  it('does not render any "Em dev." badge anywhere (item 15)', async () => {
    // given any user state
    vi.mocked(meService.getMe).mockResolvedValue(meWith(null, null))

    const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
    await flushPromises()

    // then no wip badge text is present
    expect(wrapper.text()).not.toContain('Em dev.')
    expect(wrapper.find('.nav-wip-badge').exists()).toBe(false)
  })

  it('still renders Evolução even when onboarding is complete (link is not removed)', async () => {
    // given a fully onboarded user
    vi.mocked(meService.getMe).mockResolvedValue(meWith(PROFILE, ASSESSMENT))

    const wrapper = mount(AppSidebar, { global: { plugins: [router] } })
    await flushPromises()

    const links = wrapper.findAll('.nav-link')
    expect(links.some((l) => l.text() === 'Evolução')).toBe(true)
  })
})
