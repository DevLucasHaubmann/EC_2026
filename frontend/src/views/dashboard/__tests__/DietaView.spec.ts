// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import type { Dashboard } from '@/types/api'
import { DIET_MESSAGES } from '@/constants/messages'

// ── Hoisted handles ───────────────────────────────────────────────────────────

// Refs are created inside the mock factory (which runs lazily, after the `ref` import is
// initialized) and attached here. The vi.hoisted callback runs before module imports, so it
// must not reference `ref` directly — doing so triggers "Cannot access before initialization".
const hoisted = vi.hoisted(() => ({
  startGeneration: vi.fn(),
  generationStatus: null as unknown as ReturnType<typeof ref<string | null>>,
  generationError: null as unknown as ReturnType<typeof ref<string | null>>,
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
}))

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/services/modules/aiRecommendation', () => ({
  aiService: {
    getLatest: vi.fn(),
    getById: vi.fn(),
  },
}))

vi.mock('@/services/modules/dashboard', () => ({
  dashboardService: {
    getDashboard: vi.fn(),
  },
}))

vi.mock('@/composables/useDietGeneration', () => {
  // Created here (lazy factory execution) where `ref` is already initialized, then shared with
  // the test body via `hoisted` so assertions can drive status/error before mounting.
  hoisted.generationStatus = ref<string | null>(null)
  hoisted.generationError = ref<string | null>(null)
  return {
    useDietGeneration: () => ({
      isGenerating: ref(false),
      status: hoisted.generationStatus,
      error: hoisted.generationError,
      currentMessage: ref(''),
      recommendationId: ref(null),
      start: hoisted.startGeneration,
    }),
  }
})

vi.mock('@/components/ui/DietGenerationProgress.vue', () => ({
  default: { template: '<div class="diet-gen-progress-stub" />' },
}))

vi.mock('@/components/feedback/RecommendationFeedbackForm.vue', () => ({
  default: { template: '<div class="feedback-form-stub" />' },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: hoisted.routerPush, replace: hoisted.routerReplace }),
  useRoute: () => ({ query: {} }),
}))

// ── Import after mocks ────────────────────────────────────────────────────────

import DietaView from '@/views/dashboard/DietaView.vue'
import { aiService } from '@/services/modules/aiRecommendation'
import { dashboardService } from '@/services/modules/dashboard'

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeDashboard(onboardingRequired: boolean): Dashboard.Response {
  return {
    name: 'Tester',
    profile: onboardingRequired ? null : {
      weightKg: 70,
      heightCm: 175,
      bmi: 22.9,
      bmiClassification: 'Normal',
      activityLevel: 'MODERATE',
      gender: 'MALE',
      age: 30,
    },
    assessment: onboardingRequired ? null : {
      goal: 'WEIGHT_LOSS',
      hasRestrictions: false,
      hasAllergies: false,
      hasHealthConditions: false,
    },
    onboarding: { onboardingRequired, nextStep: onboardingRequired ? 'PROFILE' : 'NONE' },
  }
}

function makeAxios404(): unknown {
  return { response: { status: 404 } }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('DietaView — estado vazio (sem recomendação)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    hoisted.generationStatus.value = null
    hoisted.generationError.value = null
  })

  describe('(A) onboardingRequired = true → precisa completar triagem', () => {
    it('mostra o CTA de triagem e NÃO o botão de gerar', async () => {
      // given: sem recomendação (404) e onboarding incompleto
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(true))

      // when: monta a view
      const wrapper = mount(DietaView)
      await flushPromises()

      // then: exibe a mensagem de triagem pendente e o botão de navegação
      expect(wrapper.text()).toContain(DIET_MESSAGES.COMPLETE_ASSESSMENT)
      expect(wrapper.text()).toContain('Ir para a triagem')
      // and: NÃO exibe o botão de gerar primeira dieta
      expect(wrapper.text()).not.toContain(DIET_MESSAGES.GENERATE_FIRST)
    })

    it('navega para /triagem ao clicar no CTA', async () => {
      // given
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(true))
      const wrapper = mount(DietaView)
      await flushPromises()

      // when: clica no botão de ir para triagem
      await wrapper.find('button.regen-btn--first').trigger('click')

      // then: router navega para a rota 'triagem'
      expect(hoisted.routerPush).toHaveBeenCalledWith({ name: 'triagem' })
    })

    it('NÃO exibe a mensagem "Tudo pronto!" quando onboarding está incompleto', async () => {
      // given
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(true))
      const wrapper = mount(DietaView)
      await flushPromises()

      // then: a mensagem de pronto não aparece (evita confusão)
      expect(wrapper.text()).not.toContain(DIET_MESSAGES.READY_TITLE)
    })
  })

  describe('(B) onboardingRequired = false → pronto para gerar a primeira dieta', () => {
    it('mostra o título "Tudo pronto!" e o botão de gerar', async () => {
      // given: sem recomendação (404) e onboarding completo
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(false))

      // when
      const wrapper = mount(DietaView)
      await flushPromises()

      // then
      expect(wrapper.text()).toContain(DIET_MESSAGES.READY_TITLE)
      expect(wrapper.text()).toContain(DIET_MESSAGES.READY_SUBTITLE)
      expect(wrapper.text()).toContain(DIET_MESSAGES.GENERATE_FIRST)
    })

    it('NÃO exibe instrução falsa de "Complete a triagem" quando ela já está completa', async () => {
      // given
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(false))
      const wrapper = mount(DietaView)
      await flushPromises()

      // then: a instrução enganosa não aparece
      expect(wrapper.text()).not.toContain(DIET_MESSAGES.COMPLETE_ASSESSMENT)
    })

    it('dispara startGeneration ao clicar em "Gerar primeira dieta"', async () => {
      // given
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(false))
      const wrapper = mount(DietaView)
      await flushPromises()

      // when: clica no botão de gerar
      await wrapper.find('button.regen-btn--first').trigger('click')

      // then: o fluxo de geração é iniciado
      expect(hoisted.startGeneration).toHaveBeenCalledTimes(1)
    })
  })

  describe('(C) falha na geração (status FAILED) com onboardingRequired = false', () => {
    it('mostra mensagem de erro e botão "Tentar novamente"', async () => {
      // given: sem recomendação, onboarding completo, e geração prévia falhou
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(false))
      hoisted.generationStatus.value = 'FAILED'
      hoisted.generationError.value = 'Erro de IA'

      // when
      const wrapper = mount(DietaView)
      await flushPromises()

      // then: exibe erro e opção de retry
      expect(wrapper.text()).toContain(DIET_MESSAGES.GENERATION_ERROR)
      expect(wrapper.text()).toContain('Tentar novamente')
      // and: NÃO exibe o título de sucesso
      expect(wrapper.text()).not.toContain(DIET_MESSAGES.READY_TITLE)
    })

    it('dispara startGeneration ao clicar em "Tentar novamente"', async () => {
      // given
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockResolvedValue(makeDashboard(false))
      hoisted.generationStatus.value = 'FAILED'
      const wrapper = mount(DietaView)
      await flushPromises()

      // when
      await wrapper.find('button.regen-btn--first').trigger('click')

      // then
      expect(hoisted.startGeneration).toHaveBeenCalledTimes(1)
    })
  })

  describe('erro ao carregar status de onboarding', () => {
    it('mostra mensagem de erro de carregamento, sem CTA enganoso', async () => {
      // given: sem recomendação (404) e falha ao buscar o dashboard
      vi.mocked(aiService.getLatest).mockRejectedValue(makeAxios404())
      vi.mocked(dashboardService.getDashboard).mockRejectedValue(new Error('network error'))

      // when
      const wrapper = mount(DietaView)
      await flushPromises()

      // then: exibe mensagem de erro honesta
      expect(wrapper.text()).toContain(DIET_MESSAGES.ONBOARDING_LOAD_ERROR)
      // and: nenhum CTA de triagem ou geração é exibido
      expect(wrapper.text()).not.toContain('Ir para a triagem')
      expect(wrapper.text()).not.toContain(DIET_MESSAGES.GENERATE_FIRST)
    })
  })
})
