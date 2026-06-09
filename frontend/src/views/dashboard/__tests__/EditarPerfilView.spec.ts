// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { DietType, MeResponse } from '@/types/api'
import { DIET_LABELS } from '@/constants/labels'
import { DIET_TYPE_OPTIONS } from '@/constants/assessmentOptions'
import { CRITICAL_DIET_TYPES } from '@/views/__tests__/dietTestSupport'

// ── Mocks ─────────────────────────────────────────────────────────────────────
// EditarPerfilView preloads the current assessment and rebuilds the update payload
// inside <script setup>; we mount it and assert against the service boundary.
// updateAvatar is mocked because EditarPerfilView calls it from the avatar section;
// without a mock the resolved value would be undefined and the component would throw.

vi.mock('@/services/modules/me', () => ({
  meService: { getMe: vi.fn(), updateAvatar: vi.fn().mockResolvedValue({}) },
}))
vi.mock('@/services/modules/assessment', () => ({
  assessmentService: { updateOwn: vi.fn().mockResolvedValue({}) },
}))
vi.mock('@/services/modules/profile', () => ({
  profileService: { updateOwn: vi.fn().mockResolvedValue({}) },
}))
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
}))

import EditarPerfilView from '@/views/dashboard/EditarPerfilView.vue'
import { meService } from '@/services/modules/me'
import { assessmentService } from '@/services/modules/assessment'
import { useMeStore } from '@/stores/me'

function meWithDiet(dietType: DietType): MeResponse {
  return {
    id: 1,
    name: 'Tester',
    email: 'tester@tukan.test',
    type: 'USER',
    status: 'ACTIVE',
    avatarUrl: null,
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
    assessment: {
      id: 5,
      userId: 1,
      userName: 'Tester',
      goal: 'WEIGHT_LOSS',
      dietType,
      dietaryRestrictions: 'sem glúten',
      allergies: 'Amendoim',
      healthConditions: 'Hipertensão',
      mealsPerDay: 4,
      targetWeightKg: 65,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: null,
    },
  }
}

// The diet radio-tabs group is the one whose buttons carry the diet labels (the other
// radio-tabs group holds the 3/4/5 meals-per-day buttons).
function dietButtons(wrapper: VueWrapper) {
  const group = wrapper
    .findAll('.radio-tabs')
    .find((g) => g.findAll('button').some((b) => b.text() === DIET_LABELS.ONIVORA))
  if (!group) throw new Error('Diet radio-tabs group not rendered')
  return group.findAll('button')
}

function dietButton(wrapper: VueWrapper, dietType: DietType) {
  const target = dietButtons(wrapper).find((b) => b.text() === DIET_LABELS[dietType])
  if (!target) throw new Error(`Diet button not rendered for ${dietType}`)
  return target
}

// ── Avatar tests ──────────────────────────────────────────────────────────────

describe('EditarPerfilView — avatar section', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(meService.updateAvatar).mockResolvedValue({} as MeResponse)
  })

  async function mountWithAvatar(avatarUrl: string | null = null) {
    vi.mocked(meService.getMe).mockResolvedValue({ ...meWithDiet('ONIVORA'), avatarUrl })
    const wrapper = mount(EditarPerfilView)
    await flushPromises()
    return wrapper
  }

  it('calls updateAvatar with the typed URL and updates the store when a valid https URL is saved', async () => {
    // given the avatar input is filled with a valid https URL
    const wrapper = await mountWithAvatar(null)
    const meStore = useMeStore()
    // Seed the store so setAvatar has a me object to update
    meStore.me = { ...meWithDiet('ONIVORA'), avatarUrl: null }

    const input = wrapper.find('#campo-avatar-url')
    await input.setValue('https://example.com/foto.jpg')
    await wrapper.find('.avatar-save-btn').trigger('click')
    await flushPromises()

    // then the service is called with the URL
    expect(meService.updateAvatar).toHaveBeenCalledWith('https://example.com/foto.jpg')
    // and the store reflects the new avatar
    expect(meStore.me?.avatarUrl).toBe('https://example.com/foto.jpg')
  })

  it('calls updateAvatar with null and clears the store when the avatar field is emptied', async () => {
    // given the user had an avatar and clears the field
    const wrapper = await mountWithAvatar('https://example.com/old.jpg')
    const meStore = useMeStore()
    meStore.me = { ...meWithDiet('ONIVORA'), avatarUrl: 'https://example.com/old.jpg' }

    const input = wrapper.find('#campo-avatar-url')
    await input.setValue('')
    await wrapper.find('.avatar-save-btn').trigger('click')
    await flushPromises()

    // then the service is called with null (empty string trimmed → null)
    expect(meService.updateAvatar).toHaveBeenCalledWith(null)
    // and the store clears the avatar
    expect(meStore.me?.avatarUrl).toBeNull()
  })

  it.each([
    ['javascript:alert(1)'],
    ['data:text/html,<h1>xss</h1>'],
    ['file:///etc/passwd'],
    ['//example.com/no-protocol'],
    ['not-a-url'],
  ])('blocks invalid URL "%s": shows error and does not call updateAvatar', async (badUrl) => {
    // given an invalid URL is typed
    const wrapper = await mountWithAvatar(null)

    const input = wrapper.find('#campo-avatar-url')
    await input.setValue(badUrl)
    await flushPromises()

    // then an error message is shown
    expect(wrapper.find('#erro-avatar-url').exists()).toBe(true)
    expect(wrapper.find('#erro-avatar-url').text()).toContain('URL inválida')

    // and the button is disabled
    expect(wrapper.find('.avatar-save-btn').attributes('disabled')).toBeDefined()

    // and clicking does not call the service
    await wrapper.find('.avatar-save-btn').trigger('click')
    await flushPromises()
    expect(meService.updateAvatar).not.toHaveBeenCalled()
  })
})

// ── Diet type tests (existing) ────────────────────────────────────────────────

describe('EditarPerfilView — diet type preload and update payload', () => {
  beforeEach(() => {
    // EditarPerfilView calls useMeStore() in <script setup>, which requires an
    // active Pinia instance. A fresh store per test prevents state leaking
    // between cases.
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders all 8 diet options', async () => {
    // given a profile being edited
    vi.mocked(meService.getMe).mockResolvedValue(meWithDiet('ONIVORA'))
    const wrapper = mount(EditarPerfilView)
    await flushPromises()
    // then every canonical diet label is offered in canonical order
    expect(dietButtons(wrapper).map((b) => b.text())).toEqual(
      DIET_TYPE_OPTIONS.map((opt) => opt.label),
    )
  })

  it('preloads the current dietType as the active selection', async () => {
    // given the saved assessment uses VEGANA
    vi.mocked(meService.getMe).mockResolvedValue(meWithDiet('VEGANA'))
    const wrapper = mount(EditarPerfilView)
    await flushPromises()
    // then only the VEGANA button is active
    const active = dietButtons(wrapper).filter((b) => b.classes('active'))
    expect(active).toHaveLength(1)
    expect(active[0]!.text()).toBe(DIET_LABELS.VEGANA)
  })

  it.each(CRITICAL_DIET_TYPES)(
    'sends dietType=%s in the update payload after changing it',
    async (dietType) => {
      // given a profile currently set to ONIVORA
      vi.mocked(meService.getMe).mockResolvedValue(meWithDiet('ONIVORA'))
      const wrapper = mount(EditarPerfilView)
      await flushPromises()

      // when the user switches to the target diet and confirms
      await dietButton(wrapper, dietType).trigger('click')
      await wrapper.find('form.edit-form').trigger('submit')
      await flushPromises()

      // then the assessment update payload carries exactly that dietType
      expect(assessmentService.updateOwn).toHaveBeenCalledTimes(1)
      const payload = vi.mocked(assessmentService.updateOwn).mock.calls[0]![0]
      expect(payload.dietType).toBe(dietType)

      // and dietType is not conflated with the free-text restrictions field: the update
      // payload carries the enum and never echoes dietaryRestrictions back into it
      expect(payload).not.toHaveProperty('dietaryRestrictions')
    },
  )
})
