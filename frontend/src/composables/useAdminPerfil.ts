import { ref } from 'vue'
import { adminProfileService } from '@/services/modules/adminProfile'
import type { ProfileData, TabState } from '@/types/admin'
import type { Dashboard } from '@/types/api'
import { ADMIN_MESSAGES, PROFILE_MESSAGES, VALIDATION_MESSAGES } from '@/constants/messages'
import { composeMessage } from '@/utils/messageComposer'
import type { AdminTabDeps } from '@/composables/useAdminTriagem'
import type { useAdminTriagem } from '@/composables/useAdminTriagem'

/**
 * Owns the admin "perfil" (nutritional profile) tab for a single user: load, create,
 * edit and delete. Profile deletion cascades over the linked assessment, so this
 * composable depends on the triagem composable to coordinate that single user-facing
 * step while each tab keeps owning its own state.
 */
export function useAdminPerfil(
  { userId, erro, clearError, toast }: AdminTabDeps,
  triagem: ReturnType<typeof useAdminTriagem>,
) {
  const perfil = ref<TabState<ProfileData>>('loading')
  const formPerfil = ref({
    dateOfBirth: '',
    gender: 'MALE' as Dashboard.Gender,
    weightKg: '' as string | number,
    heightCm: '' as string | number,
    activityLevel: 'SEDENTARY' as Dashboard.ActivityLevel,
  })
  const loadingPerfil = ref(false)
  const confirmExcluirPerfil = ref(false)
  const modoEdicaoPerfil = ref(false)
  const modoNovoPerfil = ref(false)

  // Retorna true se sabemos com certeza que o perfil não existe (não está ainda carregando)
  const perfilAusente = () => perfil.value === 'not-found'

  async function carregarPerfil() {
    perfil.value = 'loading'
    try {
      const data: ProfileData = await adminProfileService.findByUserId(userId.value)
      perfil.value = data
      formPerfil.value = {
        dateOfBirth: data.dateOfBirth?.slice(0, 10) ?? '',
        gender: data.gender,
        weightKg: data.weightKg,
        heightCm: data.heightCm,
        activityLevel: data.activityLevel,
      }
    } catch (err: any) {
      perfil.value = err.response?.status === 404 ? 'not-found' : 'error'
      if (err.response?.status !== 404) erro.value = ADMIN_MESSAGES.PROFILE_LOAD_ERROR
    }
  }

  function iniciarNovoPerfil() {
    formPerfil.value = { dateOfBirth: '', gender: 'MALE', weightKg: '', heightCm: '', activityLevel: 'SEDENTARY' }
    modoNovoPerfil.value = true
  }

  // Valida formulário de perfil antes de enviar
  function validarPerfil(): string | null {
    const f = formPerfil.value
    if (!f.dateOfBirth) return VALIDATION_MESSAGES.REQUIRED_BIRTHDATE
    if (new Date(f.dateOfBirth as string) >= new Date()) return VALIDATION_MESSAGES.PAST_BIRTHDATE
    if (!f.gender) return VALIDATION_MESSAGES.REQUIRED_GENDER
    const peso = Number(f.weightKg)
    if (!f.weightKg || isNaN(peso) || peso < 20 || peso > 500) return VALIDATION_MESSAGES.WEIGHT_RANGE
    const altura = Number(f.heightCm)
    if (!f.heightCm || isNaN(altura) || altura < 50 || altura > 300) return VALIDATION_MESSAGES.HEIGHT_RANGE
    if (!f.activityLevel) return VALIDATION_MESSAGES.REQUIRED_ACTIVITY_LEVEL
    return null
  }

  async function submitPerfil(isNew: boolean) {
    const erroValidacao = validarPerfil()
    if (erroValidacao) { erro.value = erroValidacao; return }
    clearError()
    loadingPerfil.value = true
    try {
      const data: ProfileData = isNew
        ? await adminProfileService.create(userId.value, formPerfil.value)
        : await adminProfileService.update((perfil.value as ProfileData).id, formPerfil.value)
      perfil.value = data
      if (isNew) modoNovoPerfil.value = false
      else modoEdicaoPerfil.value = false
      toast.success(isNew ? PROFILE_MESSAGES.CREATE_SUCCESS : PROFILE_MESSAGES.UPDATE_SUCCESS)
    } catch (err: any) {
      erro.value = err.response?.data?.message ?? (isNew ? PROFILE_MESSAGES.CREATE_ERROR : PROFILE_MESSAGES.UPDATE_ERROR)
    } finally {
      loadingPerfil.value = false
    }
  }

  const salvarPerfil = () => submitPerfil(false)
  const criarPerfil  = () => submitPerfil(true)

  // Antes de mostrar confirmação de excluir perfil, garante que o estado da triagem é conhecido
  async function solicitarExclusaoPerfil() {
    if (triagem.triagem.value === 'loading') {
      await triagem.carregarTriagem()
    }
    confirmExcluirPerfil.value = true
  }

  // Exclusão de perfil com cascata condicional sobre triagem
  async function confirmarExcluirPerfil() {
    clearError()
    confirmExcluirPerfil.value = false
    loadingPerfil.value = true

    const triagemExistente = triagem.triagemExiste()

    // Passo 1: excluir triagem vinculada, se existir
    if (triagemExistente) {
      const removida = await triagem.removerEntidadeVinculada()
      if (!removida) {
        loadingPerfil.value = false
        return
      }
    }

    // Passo 2: excluir o perfil
    try {
      await adminProfileService.delete((perfil.value as ProfileData).id)
      perfil.value = 'not-found'
      modoEdicaoPerfil.value = false
      toast.success(triagemExistente
        ? PROFILE_MESSAGES.DELETE_WITH_ASSESSMENT_SUCCESS
        : PROFILE_MESSAGES.DELETE_SUCCESS)
    } catch (err: any) {
      // Triagem já foi excluída mas perfil falhou — recarrega para estado consistente
      erro.value = composeMessage(err.response?.data?.message ?? PROFILE_MESSAGES.DELETE_ERROR, PROFILE_MESSAGES.DELETE_ASSESSMENT_REMOVED)
      await Promise.all([carregarPerfil(), triagem.carregarTriagem()])
    } finally {
      loadingPerfil.value = false
    }
  }

  return {
    perfil,
    formPerfil,
    loadingPerfil,
    confirmExcluirPerfil,
    modoEdicaoPerfil,
    modoNovoPerfil,
    perfilAusente,
    carregarPerfil,
    iniciarNovoPerfil,
    salvarPerfil,
    criarPerfil,
    solicitarExclusaoPerfil,
    confirmarExcluirPerfil,
  }
}
