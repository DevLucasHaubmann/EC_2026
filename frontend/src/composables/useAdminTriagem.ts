import { ref, type Ref } from 'vue'
import { adminAssessmentService } from '@/services/modules/adminAssessment'
import type { AssessmentData, AdminAssessmentRequest, TabState } from '@/types/admin'
import type { Dashboard, DietType } from '@/types/api'
import { ADMIN_MESSAGES, ASSESSMENT_MESSAGES, VALIDATION_MESSAGES } from '@/constants/messages'
import { composeMessage } from '@/utils/messageComposer'
import type { useToast } from '@/composables/useToast'

/**
 * Dependencies an admin tab composable shares with its host modal: the target userId,
 * the single error banner ref, a helper to clear it, and the toast bus. Keeping these
 * owned by the host preserves the modal's current single-message / single-toast UX.
 */
export interface AdminTabDeps {
  userId: Ref<number>
  erro: Ref<string | null>
  clearError: () => void
  toast: ReturnType<typeof useToast>
}

/**
 * Owns the admin "triagem" (assessment) tab for a single user: load, create, edit,
 * delete and the form/validation around it. The host modal stays a thin orchestrator.
 *
 * `removerEntidadeVinculada` exists for the cascade triggered from the profile tab
 * (deleting a profile also removes its linked assessment); it carries the linked-delete
 * messaging that differs from the standalone triagem deletion.
 */
export function useAdminTriagem({ userId, erro, clearError, toast }: AdminTabDeps) {
  const triagem = ref<TabState<AssessmentData>>('loading')
  const formTriagem = ref({
    goal: 'WEIGHT_LOSS' as Dashboard.NutritionalGoal,
    dietType: null as DietType | null,
    dietaryRestrictions: '',
    allergies: '',
    healthConditions: '',
    mealsPerDay: null as number | null,
    targetWeightKg: null as number | null,
  })
  const loadingTriagem = ref(false)
  const confirmExcluirTriagem = ref(false)
  const modoEdicaoTriagem = ref(false)
  const modoNovaTriagem = ref(false)

  // Retorna true se triagem é um objeto real (existe)
  const triagemExiste = () => typeof triagem.value === 'object' && triagem.value !== null

  async function carregarTriagem() {
    triagem.value = 'loading'
    try {
      const data: AssessmentData = await adminAssessmentService.findByUserId(userId.value)
      triagem.value = data
      formTriagem.value = {
        goal: data.goal,
        dietType: data.dietType,
        dietaryRestrictions: data.dietaryRestrictions ?? '',
        allergies: data.allergies ?? '',
        healthConditions: data.healthConditions ?? '',
        mealsPerDay: data.mealsPerDay,
        targetWeightKg: data.targetWeightKg,
      }
    } catch (err: any) {
      triagem.value = err.response?.status === 404 ? 'not-found' : 'error'
      if (err.response?.status !== 404) erro.value = ADMIN_MESSAGES.ASSESSMENT_LOAD_ERROR
    }
  }

  function iniciarNovaTriagem() {
    formTriagem.value = { goal: 'WEIGHT_LOSS', dietType: null, dietaryRestrictions: '', allergies: '', healthConditions: '', mealsPerDay: null, targetWeightKg: null }
    modoNovaTriagem.value = true
  }

  // Valida formulário de triagem antes de enviar. dietType e mealsPerDay são obrigatórios
  // pelo contrato do backend (CreateAssessmentRequest/UpdateAssessmentRequest).
  function validarTriagem(): string | null {
    const f = formTriagem.value
    if (!f.goal) return VALIDATION_MESSAGES.REQUIRED_NUTRITION_GOAL
    if (!f.dietType) return VALIDATION_MESSAGES.REQUIRED_DIET_TYPE
    if (f.mealsPerDay === null || f.mealsPerDay === undefined) return VALIDATION_MESSAGES.REQUIRED_MEALS_PER_DAY
    if (f.mealsPerDay < 3 || f.mealsPerDay > 5) return VALIDATION_MESSAGES.MEALS_PER_DAY_RANGE
    if (f.targetWeightKg !== null && f.targetWeightKg !== undefined && f.targetWeightKg <= 0) {
      return VALIDATION_MESSAGES.TARGET_WEIGHT_POSITIVE
    }
    return null
  }

  // Builds the request payload only after validation, narrowing dietType/mealsPerDay to non-null
  // so the admin flow never sends null for fields the backend now requires.
  function buildAssessmentPayload(): AdminAssessmentRequest {
    const f = formTriagem.value
    return {
      goal: f.goal,
      dietType: f.dietType!,
      mealsPerDay: f.mealsPerDay!,
      dietaryRestrictions: f.dietaryRestrictions || null,
      allergies: f.allergies || null,
      healthConditions: f.healthConditions || null,
      targetWeightKg: f.targetWeightKg ?? null,
    }
  }

  async function submitTriagem(isNew: boolean) {
    const erroValidacao = validarTriagem()
    if (erroValidacao) { erro.value = erroValidacao; return }
    clearError()
    loadingTriagem.value = true
    try {
      const payload = buildAssessmentPayload()
      const data: AssessmentData = isNew
        ? await adminAssessmentService.create(userId.value, payload)
        : await adminAssessmentService.update((triagem.value as AssessmentData).id, payload)
      triagem.value = data
      if (isNew) modoNovaTriagem.value = false
      else modoEdicaoTriagem.value = false
      toast.success(isNew ? ASSESSMENT_MESSAGES.CREATE_SUCCESS : ASSESSMENT_MESSAGES.UPDATE_SUCCESS)
    } catch (err: any) {
      erro.value = err.response?.data?.message ?? (isNew ? ASSESSMENT_MESSAGES.CREATE_ERROR : ASSESSMENT_MESSAGES.UPDATE_ERROR)
    } finally {
      loadingTriagem.value = false
    }
  }

  const salvarTriagem = () => submitTriagem(false)
  const criarTriagem  = () => submitTriagem(true)

  async function confirmarExcluirTriagem() {
    clearError()
    confirmExcluirTriagem.value = false
    loadingTriagem.value = true
    try {
      await adminAssessmentService.delete((triagem.value as AssessmentData).id)
      triagem.value = 'not-found'
      modoEdicaoTriagem.value = false
      toast.success(ASSESSMENT_MESSAGES.DELETE_SUCCESS)
    } catch (err: any) {
      erro.value = err.response?.data?.message ?? ASSESSMENT_MESSAGES.DELETE_ERROR
    } finally {
      loadingTriagem.value = false
    }
  }

  // Exclusão da triagem como passo vinculado da exclusão de perfil (cascata). Difere da
  // exclusão autônoma na mensagem de erro: aqui o perfil é mantido se a triagem falhar.
  async function removerEntidadeVinculada(): Promise<boolean> {
    try {
      await adminAssessmentService.delete((triagem.value as AssessmentData).id)
      triagem.value = 'not-found'
      return true
    } catch (err: any) {
      erro.value = composeMessage(err.response?.data?.message ?? ASSESSMENT_MESSAGES.LINKED_DELETE_ERROR, ASSESSMENT_MESSAGES.LINKED_DELETE_PROFILE_KEPT)
      return false
    }
  }

  return {
    triagem,
    formTriagem,
    loadingTriagem,
    confirmExcluirTriagem,
    modoEdicaoTriagem,
    modoNovaTriagem,
    triagemExiste,
    carregarTriagem,
    iniciarNovaTriagem,
    salvarTriagem,
    criarTriagem,
    confirmarExcluirTriagem,
    removerEntidadeVinculada,
  }
}
