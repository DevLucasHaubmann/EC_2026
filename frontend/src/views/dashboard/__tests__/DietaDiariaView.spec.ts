// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { MealLog, Recommendation } from '@/types/api'

// ── Mocks ────────────────────────────────────────────────────────────────────

const hoisted = vi.hoisted(() => ({
  toastWarning: vi.fn(),
  toastError: vi.fn(),
  toastSuccess: vi.fn(),
}))

vi.mock('@/services/modules/aiRecommendation', () => ({
  aiService: { getLatest: vi.fn() },
}))
vi.mock('@/services/modules/mealLog', () => ({
  mealLogService: { getByDate: vi.fn(), log: vi.fn(), remove: vi.fn() },
}))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ logout: vi.fn() }),
}))
vi.mock('vue-router', () => ({
  useRouter: () => ({ back: vi.fn(), push: vi.fn() }),
}))
vi.mock('@/composables/useToast', () => ({
  useToast: () => ({
    warning: hoisted.toastWarning,
    error: hoisted.toastError,
    success: hoisted.toastSuccess,
    info: vi.fn(),
  }),
}))

import DietaDiariaView from '@/views/dashboard/DietaDiariaView.vue'
import { aiService } from '@/services/modules/aiRecommendation'
import { mealLogService } from '@/services/modules/mealLog'

// ── Factories ─────────────────────────────────────────────────────────────────

function makeRecommendation(): Recommendation.Response {
  return {
    id: 1,
    userId: 1,
    summary: 'Test diet',
    plan: {
      dailyCalorieTarget: 2000,
      goal: 'WEIGHT_LOSS',
      meals: [
        {
          mealType: 'BREAKFAST',
          calorieTarget: 500,
          options: [
            {
              optionNumber: 1,
              items: [],
              totalCalories: 480,
              totalProtein: 20,
              totalCarbs: 60,
              totalFat: 15,
            },
          ],
        },
      ],
    },
    mealExplanations: {},
    tips: [],
    alerts: [],
    provider: 'gemini',
    model: 'gemini-pro',
    status: 'GENERATED',
    createdAt: '2024-06-08T10:00:00Z',
  }
}

function makeLog(overrides: Partial<MealLog.Response> = {}): MealLog.Response {
  return {
    id: 1,
    recommendationId: 1,
    mealDate: '2024-06-08',
    mealType: 'BREAKFAST',
    optionNumber: 1,
    calories: 480,
    protein: 20,
    carbs: 60,
    fat: 15,
    notes: null,
    createdAt: '2024-06-08T08:30:00Z',
    ...overrides,
  }
}

const networkError = { response: { status: 500 } }
const conflictError = { response: { status: 409 } }

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('DietaDiariaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows loading state while services are pending', async () => {
    // given both services are pending indefinitely
    vi.mocked(aiService.getLatest).mockReturnValue(new Promise(() => {}))
    vi.mocked(mealLogService.getByDate).mockReturnValue(new Promise(() => {}))
    // when the view mounts
    const wrapper = mount(DietaDiariaView)
    await nextTick()
    // then the loading indicator is visible
    expect(wrapper.text()).toContain('Carregando seu plano do dia')
  })

  it('shows log error when getByDate rejects — not the empty-success state', async () => {
    // given recommendation loads successfully but logs fail
    vi.mocked(aiService.getLatest).mockResolvedValue(makeRecommendation())
    vi.mocked(mealLogService.getByDate).mockRejectedValue(networkError)
    const wrapper = mount(DietaDiariaView)
    await flushPromises()
    // then an explicit error message is shown for logs
    expect(wrapper.text()).toContain('Não foi possível carregar as refeições efetivadas de hoje.')
    // and the view does not mislead the user with an empty-success message
    expect(wrapper.text()).not.toContain('Nenhum registro encontrado para hoje')
  })

  it('shows empty state when getByDate succeeds with an empty array', async () => {
    // given recommendation loads and no logs exist for today
    vi.mocked(aiService.getLatest).mockResolvedValue(makeRecommendation())
    vi.mocked(mealLogService.getByDate).mockResolvedValue([])
    const wrapper = mount(DietaDiariaView)
    await flushPromises()
    // then the real empty state is shown (real zero, not error)
    expect(wrapper.text()).toContain('Nenhum registro encontrado para hoje')
    // and no error is shown
    expect(wrapper.text()).not.toContain('Não foi possível carregar')
  })

  it('displays calories and meal entries from real service data', async () => {
    // given recommendation and one logged meal
    vi.mocked(aiService.getLatest).mockResolvedValue(makeRecommendation())
    vi.mocked(mealLogService.getByDate).mockResolvedValue([makeLog()])
    const wrapper = mount(DietaDiariaView)
    await flushPromises()
    // then the logged calories are totalled and shown
    expect(wrapper.text()).toContain('480')
    // and the meal label is rendered
    expect(wrapper.text()).toContain('Café da manhã')
    // and there is exactly one log entry in the timeline
    expect(wrapper.findAll('.log-entry')).toHaveLength(1)
  })

  it('shows conflict warning on 409 and does not duplicate the log entry', async () => {
    // given recommendation loaded and logs start empty
    vi.mocked(aiService.getLatest).mockResolvedValue(makeRecommendation())
    vi.mocked(mealLogService.getByDate)
      .mockResolvedValueOnce([])         // initial fetch: no logs
      .mockResolvedValue([makeLog()])    // re-fetch after 409: server has the log

    // and the log call returns a 409 conflict
    vi.mocked(mealLogService.log).mockRejectedValue(conflictError)

    const wrapper = mount(DietaDiariaView)
    await flushPromises()

    // when the user tries to add a meal
    const addBtn = wrapper.find('.btn-add-plus')
    expect(addBtn.exists()).toBe(true)
    await addBtn.trigger('click')
    await flushPromises()

    // then a conflict warning toast is shown
    expect(hoisted.toastWarning).toHaveBeenCalledWith(
      expect.stringContaining('já registrado'),
    )
    // and the log list reflects the server state (1 item, not 0 or 2)
    expect(wrapper.findAll('.log-entry')).toHaveLength(1)
  })
})
