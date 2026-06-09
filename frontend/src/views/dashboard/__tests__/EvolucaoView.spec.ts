// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { Evolution, BodyWeight } from '@/types/api'

// ── Mocks ────────────────────────────────────────────────────────────────────

vi.mock('@/services/modules/evolution', () => ({
  evolutionService: { getWeekly: vi.fn() },
}))

vi.mock('@/services/modules/bodyWeight', () => ({
  bodyWeightService: {
    getSummary: vi.fn(),
    register: vi.fn(),
  },
}))

import EvolucaoView from '@/views/dashboard/EvolucaoView.vue'
import { evolutionService } from '@/services/modules/evolution'
import { bodyWeightService } from '@/services/modules/bodyWeight'

// ── Factories ─────────────────────────────────────────────────────────────────

function makeWeeklySummary(
  overrides: Partial<Evolution.WeeklySummary> = {},
): Evolution.WeeklySummary {
  return {
    weekStart: '2024-06-03',
    weekEnd: '2024-06-09',
    days: [
      {
        date: '2024-06-03',
        consumedCalories: 1800,
        consumedProtein: 90,
        consumedCarbs: 220,
        consumedFat: 60,
        completedMeals: 3,
        plannedMeals: 4,
        adherencePercentage: 75,
      },
    ],
    averageAdherencePercentage: 75,
    totalCalories: 1800,
    activeDays: 1,
    ...overrides,
  }
}

function makeWeightSummary(
  overrides: Partial<BodyWeight.SummaryResponse> = {},
): BodyWeight.SummaryResponse {
  return {
    currentWeightKg: 75,
    initialWeightKg: 80,
    totalChangeKg: -5,
    trend: 'LOSS',
    entryCount: 2,
    history: [
      { id: 1, weightKg: 80, measuredDate: '2024-05-01', createdAt: '2024-05-01T00:00:00Z' },
      { id: 2, weightKg: 75, measuredDate: '2024-06-01', createdAt: '2024-06-01T00:00:00Z' },
    ],
    ...overrides,
  }
}

function makeEmptyWeightSummary(): BodyWeight.SummaryResponse {
  return {
    currentWeightKg: null,
    initialWeightKg: null,
    totalChangeKg: null,
    trend: null,
    entryCount: 0,
    history: [],
  }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('EvolucaoView — adherence section', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Default weight summary so it doesn't interfere with adherence tests
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
  })

  it('shows loading state while evolution data is pending', async () => {
    // given the evolution service never resolves, weight resolves immediately
    vi.mocked(evolutionService.getWeekly).mockReturnValue(new Promise(() => {}))
    const wrapper = mount(EvolucaoView)
    await nextTick()
    expect(wrapper.text()).toContain('Carregando dados de evolução')
  })

  it('shows error message when getWeekly rejects', async () => {
    // given the service fails
    vi.mocked(evolutionService.getWeekly).mockRejectedValue(new Error('network error'))
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.text()).toContain('Não foi possível carregar os dados de evolução')
    expect(wrapper.find('.analytics-grid').exists()).toBe(false)
  })

  it('shows empty state when activeDays is 0 and does not render mock data', async () => {
    vi.mocked(evolutionService.getWeekly).mockResolvedValue(
      makeWeeklySummary({ activeDays: 0, totalCalories: 0 }),
    )
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.text()).toContain('Nenhuma refeição registrada nesta semana')
    expect(wrapper.find('.analytics-grid').exists()).toBe(false)
  })

  it('displays real adherence data when the service returns active days', async () => {
    vi.mocked(evolutionService.getWeekly).mockResolvedValue(makeWeeklySummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    // caloriasMedias = 1800 / 1 = 1800
    expect(wrapper.text()).toContain('1800 kcal')
    expect(wrapper.text()).toContain('1 dias / semana')
    expect(wrapper.text()).toContain('75.0%')
    expect(wrapper.find('.adherence-item').exists()).toBe(true)
  })

  it('no longer has a weight-unavailable placeholder anywhere in the DOM', async () => {
    vi.mocked(evolutionService.getWeekly).mockResolvedValue(makeWeeklySummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    // The placeholder block was removed in favour of the real weight section
    expect(wrapper.find('.weight-unavailable').exists()).toBe(false)
  })
})

describe('EvolucaoView — body weight section', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Default evolution so it doesn't block weight tests
    vi.mocked(evolutionService.getWeekly).mockResolvedValue(makeWeeklySummary())
  })

  it('shows weight loading state while getSummary is pending', async () => {
    vi.mocked(bodyWeightService.getSummary).mockReturnValue(new Promise(() => {}))
    const wrapper = mount(EvolucaoView)
    await nextTick()
    expect(wrapper.text()).toContain('Carregando dados de peso')
  })

  it('shows weight error state when getSummary rejects', async () => {
    vi.mocked(bodyWeightService.getSummary).mockRejectedValue(new Error('fail'))
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.text()).toContain('Não foi possível carregar os dados de peso')
  })

  it('shows empty registration form when entryCount is 0', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeEmptyWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.text()).toContain('Nenhum peso registrado ainda')
    expect(wrapper.find('#novo-peso').exists()).toBe(true)
  })

  it('shows summary cards with current and initial weight when entryCount >= 2', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.find('.weight-summary-grid').exists()).toBe(true)
    expect(wrapper.text()).toContain('75 kg') // current
    expect(wrapper.text()).toContain('80 kg') // initial
  })

  it('shows SVG chart when history has >= 2 points', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.find('.weight-chart').exists()).toBe(true)
    expect(wrapper.find('polyline').exists()).toBe(true)
  })

  it('shows trend badge for LOSS trend', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.find('.trend-badge.trend--loss').exists()).toBe(true)
    expect(wrapper.text()).toContain('Perda')
  })

  it('re-fetches weight summary after successful registration', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeEmptyWeightSummary())
    vi.mocked(bodyWeightService.register).mockResolvedValue({
      id: 1, weightKg: 75, measuredDate: '2024-06-09', createdAt: '2024-06-09T00:00:00Z',
    })
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    // Fill in a valid weight
    const input = wrapper.find('#novo-peso')
    await input.setValue(75)

    // Update mock to return 1 entry after registration
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue({
      ...makeEmptyWeightSummary(),
      currentWeightKg: 75,
      initialWeightKg: 75,
      entryCount: 1,
      history: [{ id: 1, weightKg: 75, measuredDate: '2024-06-09', createdAt: '2024-06-09T00:00:00Z' }],
    })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // getSummary called twice: once on mount, once after registration
    expect(bodyWeightService.getSummary).toHaveBeenCalledTimes(2)
    expect(bodyWeightService.register).toHaveBeenCalledWith(75)
  })

  it('shows inline validation error for weight below 20', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeEmptyWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    const input = wrapper.find('#novo-peso')
    await input.setValue(10)
    await flushPromises()

    expect(wrapper.text()).toContain('Peso mínimo: 20 kg')
  })

  it('shows inline validation error for weight above 500', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeEmptyWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    const input = wrapper.find('#novo-peso')
    await input.setValue(600)
    await flushPromises()

    expect(wrapper.text()).toContain('Peso máximo: 500 kg')
  })

  it('shows hint to register more weights when entryCount is 1', async () => {
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue({
      currentWeightKg: 75,
      initialWeightKg: 75,
      totalChangeKg: null,
      trend: null,
      entryCount: 1,
      history: [{ id: 1, weightKg: 75, measuredDate: '2024-06-09', createdAt: '2024-06-09T00:00:00Z' }],
    })
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    expect(wrapper.text()).toContain('Registre mais pesos para ver sua tendência')
  })

  // ── B3: validação de casas decimais ─────────────────────────────────────────

  it('blocks weight with more than 2 decimal places (75.999) and does not call register', async () => {
    // given empty weight state so the registration form is visible
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeEmptyWeightSummary())
    vi.mocked(bodyWeightService.register).mockResolvedValue({
      id: 1, weightKg: 75.999, measuredDate: '2024-06-09', createdAt: '2024-06-09T00:00:00Z',
    })
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    // when the user types a weight with 3 decimal places
    const input = wrapper.find('#novo-peso')
    await input.setValue(75.999)
    await flushPromises()

    // then an error message is shown
    expect(wrapper.text()).toContain('O peso deve ter no máximo 2 casas decimais.')

    // and the submit button is disabled
    const btn = wrapper.find('button[type="submit"]')
    expect(btn.attributes('disabled')).toBeDefined()

    // and submitting does not call the service
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(bodyWeightService.register).not.toHaveBeenCalled()
  })

  it('accepts weight with exactly 2 decimal places (75.99)', async () => {
    // given empty weight state
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeEmptyWeightSummary())
    vi.mocked(bodyWeightService.register).mockResolvedValue({
      id: 1, weightKg: 75.99, measuredDate: '2024-06-09', createdAt: '2024-06-09T00:00:00Z',
    })
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    // when the user types a weight with 2 decimal places
    const input = wrapper.find('#novo-peso')
    await input.setValue(75.99)
    await flushPromises()

    // then no decimal error is shown
    expect(wrapper.text()).not.toContain('O peso deve ter no máximo 2 casas decimais.')

    // and the button is enabled (not disabled)
    const btn = wrapper.find('button[type="submit"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  // ── R5.1: gráfico não deve aparecer com 1 registro (evita ponto isolado) ──────

  it('does not render the chart when entryCount is 1 (no isolated point)', async () => {
    // given a single weight entry
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue({
      currentWeightKg: 75,
      initialWeightKg: 75,
      totalChangeKg: null,
      trend: null,
      entryCount: 1,
      history: [{ id: 1, weightKg: 75, measuredDate: '2024-06-09', createdAt: '2024-06-09T00:00:00Z' }],
    })
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    // then the data card is shown, but the chart is suppressed for a single point
    expect(wrapper.find('.weight-data-card').exists()).toBe(true)
    expect(wrapper.find('.weight-chart').exists()).toBe(false)
  })

  // ── R5.1: CTA "Registrar novo peso" é um botão real, não um link ──────────────

  it('renders "Registrar novo peso" as a real button with the form collapsed', async () => {
    // given a user with weight history (the CTA lives in the data card)
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    // then the CTA is an actual <button>, and the inline form stays hidden until clicked
    const cta = wrapper.find('.weight-register-cta')
    expect(cta.exists()).toBe(true)
    expect(cta.element.tagName).toBe('BUTTON')
    expect(wrapper.find('#form-novo-peso-inline').exists()).toBe(false)
  })

  it('reveals the inline registration form when the CTA is clicked', async () => {
    // given the data card with the collapsed CTA
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()

    // when the user clicks the CTA
    await wrapper.find('.weight-register-cta').trigger('click')

    // then the form is revealed and the CTA is replaced by it
    expect(wrapper.find('#form-novo-peso-inline').exists()).toBe(true)
    expect(wrapper.find('.weight-register-cta').exists()).toBe(false)
  })

  it('clears the typed weight when the inline form is cancelled', async () => {
    // given the inline form is open with an invalid value typed in
    vi.mocked(bodyWeightService.getSummary).mockResolvedValue(makeWeightSummary())
    const wrapper = mount(EvolucaoView)
    await flushPromises()
    await wrapper.find('.weight-register-cta').trigger('click')
    await wrapper.find('#novo-peso-inline').setValue(10)
    await flushPromises()
    expect(wrapper.text()).toContain('Peso mínimo: 20 kg')

    // when the user cancels and reopens the form
    await wrapper.find('.weight-form-cancel').trigger('click')
    await wrapper.find('.weight-register-cta').trigger('click')

    // then the field is empty and no stale validation error is shown
    const input = wrapper.find('#novo-peso-inline').element as HTMLInputElement
    expect(input.value).toBe('')
    expect(wrapper.text()).not.toContain('Peso mínimo: 20 kg')
  })
})
