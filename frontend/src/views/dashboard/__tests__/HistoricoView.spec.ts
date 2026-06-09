// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import type { Recommendation } from '@/types/api'

// ── Mocks ────────────────────────────────────────────────────────────────────

vi.mock('@/services/modules/aiRecommendation', () => ({
  aiService: { getHistory: vi.fn() },
}))

import HistoricoView from '@/views/dashboard/HistoricoView.vue'
import { aiService } from '@/services/modules/aiRecommendation'

// ── Factories ─────────────────────────────────────────────────────────────────

function makeOption(optionNumber: number, protein: number): Recommendation.MealOption {
  return {
    optionNumber,
    items: [
      {
        foodId: optionNumber * 10,
        name: `Alimento ${optionNumber}`,
        displayName: `Alimento Display ${optionNumber}`,
        category: 'proteina',
        portionGrams: 100,
        calories: 200,
        protein,
        carbs: 20,
        fat: 10,
        fiber: 2,
      },
    ],
    totalCalories: 200,
    totalProtein: protein,
    totalCarbs: 20,
    totalFat: 10,
  }
}

function makeRecommendationWithTwoOptions(): Recommendation.Response {
  return {
    id: 1,
    userId: 1,
    summary: 'Dieta teste',
    plan: {
      dailyCalorieTarget: 2000,
      goal: 'WEIGHT_LOSS',
      meals: [
        {
          mealType: 'BREAKFAST',
          calorieTarget: 500,
          options: [makeOption(1, 30), makeOption(2, 25)],
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

function makeRecommendationWithOneOption(): Recommendation.Response {
  return {
    ...makeRecommendationWithTwoOptions(),
    id: 2,
    plan: {
      dailyCalorieTarget: 2000,
      goal: 'WEIGHT_LOSS',
      meals: [
        {
          mealType: 'LUNCH',
          calorieTarget: 700,
          options: [makeOption(1, 40)],
        },
      ],
    },
  }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('HistoricoView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders both options when a meal has 2 options', async () => {
    // given the service returns one recommendation with 2 options in its meal
    vi.mocked(aiService.getHistory).mockResolvedValue([makeRecommendationWithTwoOptions()])
    const wrapper = mount(HistoricoView)
    await flushPromises()

    // when the user selects the recommendation card
    await wrapper.find('.diet-card').trigger('click')
    await flushPromises()

    // then the detail panel shows 2 option blocks
    const options = wrapper.findAll('.detail-option')
    expect(options).toHaveLength(2)

    // and each option shows its food item
    expect(wrapper.text()).toContain('Alimento Display 1')
    expect(wrapper.text()).toContain('Alimento Display 2')

    // and each option shows its own macros (rounded)
    expect(wrapper.text()).toContain('30g prot')
    expect(wrapper.text()).toContain('25g prot')

    // and option labels are shown for multi-option meals
    expect(wrapper.text()).toContain('Opção 1')
    expect(wrapper.text()).toContain('Opção 2')
  })

  it('does not show option label when meal has only 1 option', async () => {
    // given one recommendation with a single-option meal
    vi.mocked(aiService.getHistory).mockResolvedValue([makeRecommendationWithOneOption()])
    const wrapper = mount(HistoricoView)
    await flushPromises()

    // when the user selects the card
    await wrapper.find('.diet-card').trigger('click')
    await flushPromises()

    // then one option block is rendered
    expect(wrapper.findAll('.detail-option')).toHaveLength(1)

    // and no "Opção N" label is shown for single-option meals
    expect(wrapper.find('.detail-option-label').exists()).toBe(false)
  })

  it('marks the newest non-ARCHIVED recommendation as active and ARCHIVED ones as archived', async () => {
    // given a newer ARCHIVED recommendation and an older non-ARCHIVED one
    const archived: Recommendation.Response = {
      ...makeRecommendationWithOneOption(),
      id: 10,
      status: 'ARCHIVED',
      createdAt: '2024-06-09T10:00:00Z',
    }
    const active: Recommendation.Response = {
      ...makeRecommendationWithOneOption(),
      id: 11,
      status: 'GENERATED',
      createdAt: '2024-06-07T10:00:00Z',
    }
    vi.mocked(aiService.getHistory).mockResolvedValue([archived, active])
    const wrapper = mount(HistoricoView)
    await flushPromises()

    // then exactly one card is marked active, and it is the non-ARCHIVED one despite being older
    const activeBadges = wrapper.findAll('.badge-active')
    expect(activeBadges).toHaveLength(1)
    expect(activeBadges[0]!.text()).toContain('Ativa')

    // and the ARCHIVED recommendation is shown as archived
    expect(wrapper.findAll('.badge-archived')).toHaveLength(1)
  })

  it('shows empty state when there are no recommendations', async () => {
    // given an empty history
    vi.mocked(aiService.getHistory).mockResolvedValue([])
    const wrapper = mount(HistoricoView)
    await flushPromises()
    // then the empty card is shown
    expect(wrapper.text()).toContain('Nenhuma dieta gerada ainda')
    // and the diet list is not rendered
    expect(wrapper.find('.diet-list').exists()).toBe(false)
  })

  it('shows error state when the service rejects', async () => {
    // given the service fails
    vi.mocked(aiService.getHistory).mockRejectedValue(new Error('network error'))
    const wrapper = mount(HistoricoView)
    await flushPromises()
    // then the error message is shown
    expect(wrapper.text()).toContain('Não foi possível carregar o histórico')
  })
})
