// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import type { DietType, MeResponse } from '@/types/api'
import { DIET_LABELS } from '@/constants/labels'
import { DIET_TYPE_OPTIONS } from '@/constants/assessmentOptions'
import { CRITICAL_DIET_TYPES } from '@/views/__tests__/dietTestSupport'

// ── Mocks ─────────────────────────────────────────────────────────────────────
// The diet payload is built inside <script setup> and only becomes observable through
// the service call, so we mount the real component and intercept the service boundary.

const hoisted = vi.hoisted(() => ({
  startGeneration: vi.fn(),
}))

vi.mock('@/services/modules/me', () => ({
  meService: { getMe: vi.fn() },
}))
vi.mock('@/services/modules/assessment', () => ({
  assessmentService: { createOwn: vi.fn().mockResolvedValue({}) },
}))
vi.mock('@/services/modules/profile', () => ({
  profileService: { createOwn: vi.fn().mockResolvedValue({}) },
}))
vi.mock('@/composables/useDietGeneration', async () => {
  const { ref } = await import('vue')
  return {
    useDietGeneration: () => ({
      status: ref(null),
      error: ref(null),
      currentMessage: ref(''),
      recommendationId: ref(null),
      start: hoisted.startGeneration,
    }),
  }
})
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }),
}))
vi.mock('@/composables/useModal', () => ({
  useModal: () => ({ open: vi.fn().mockResolvedValue(false) }),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
}))

import TriagemView from '@/views/dashboard/TriagemView.vue'
import { meService } from '@/services/modules/me'
import { assessmentService } from '@/services/modules/assessment'
import { profileService } from '@/services/modules/profile'

const RESTRICTIVE_DIET_TYPES: DietType[] = ['LOW_CARB', 'CETOGENICA', 'CARNIVORA']

// User who already has a profile: the triagem starts at step 2 (Objetivos), so the
// biometric steps are skipped and finalizing must NOT recreate the profile.
const userWithProfile: MeResponse = {
  id: 1,
  name: 'Tester',
  email: 'tester@tukan.test',
  type: 'USER',
  status: 'ACTIVE',
  profile: {
    id: 1,
    userId: 1,
    userName: 'Tester',
    dateOfBirth: '1990-01-01',
    gender: 'MALE',
    weightKg: 70,
    heightCm: 175,
    activityLevel: 'MODERATE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: null,
  },
  assessment: null,
}

function dietButton(wrapper: VueWrapper, dietType: DietType) {
  const target = wrapper
    .findAll('.segmented-diet button')
    .find((b) => b.text() === DIET_LABELS[dietType])
  if (!target) throw new Error(`Diet button not rendered for ${dietType}`)
  return target
}

// Drives the wizard from its initial step to step 4 (Saúde), where the diet selector lives.
async function goToHealthStep(): Promise<VueWrapper> {
  vi.mocked(meService.getMe).mockResolvedValue(userWithProfile)
  const wrapper = mount(TriagemView)
  await flushPromises() // onMounted -> getMe -> starts at step 2

  await wrapper.find('.selectable-card').trigger('click') // pick a goal (required to advance step 2)
  await wrapper.find('.btn-primary').trigger('click') // step 2 -> 3
  // step 3 -> 4 advances because qtdRefeicoes defaults to 3 (etapaValida requires >= 3)
  await wrapper.find('.btn-primary').trigger('click')
  return wrapper
}

describe('TriagemView — diet type selection and payload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders all 8 diet options on the health step', async () => {
    // given a user reaching the health step
    const wrapper = await goToHealthStep()
    // when reading the diet selector buttons
    const labels = wrapper.findAll('.segmented-diet button').map((b) => b.text())
    // then every canonical diet label is offered, in the canonical order
    expect(labels).toEqual(DIET_TYPE_OPTIONS.map((opt) => opt.label))
    expect(labels).toHaveLength(8)
  })

  it.each(CRITICAL_DIET_TYPES)(
    'submits dietType=%s in the assessment payload when selected',
    async (dietType) => {
      // given a user on the health step who selects a critical diet type
      const wrapper = await goToHealthStep()
      await dietButton(wrapper, dietType).trigger('click')

      // when finalizing the triagem
      await wrapper.find('.btn-finish').trigger('click')
      await flushPromises()

      // then the assessment payload carries exactly that dietType
      expect(assessmentService.createOwn).toHaveBeenCalledTimes(1)
      const payload = vi.mocked(assessmentService.createOwn).mock.calls[0]![0]
      expect(payload.dietType).toBe(dietType)

      // and the existing profile is preserved (not recreated) and generation is handed off
      expect(profileService.createOwn).not.toHaveBeenCalled()
      expect(hoisted.startGeneration).toHaveBeenCalledTimes(1)
    },
  )

  it.each(RESTRICTIVE_DIET_TYPES)(
    'shows the restrictive-diet caution for %s',
    async (dietType) => {
      // given the health step
      const wrapper = await goToHealthStep()
      // when a restrictive diet is selected
      await dietButton(wrapper, dietType).trigger('click')
      // then the caution note is shown
      expect(wrapper.find('.diet-caution').exists()).toBe(true)
    },
  )

  it('does not show the restrictive-diet caution for a non-restrictive type (ONIVORA)', async () => {
    // given the health step (ONIVORA is the default selection)
    const wrapper = await goToHealthStep()
    // when no restrictive diet is selected
    await dietButton(wrapper, 'ONIVORA').trigger('click')
    // then no caution is shown
    expect(wrapper.find('.diet-caution').exists()).toBe(false)
  })
})
