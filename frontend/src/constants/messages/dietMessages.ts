import type { GenerationJob } from '@/types/api'

export const DIET_MESSAGES = {
  LOADING: 'Carregando sua dieta...',
  LOAD_ERROR: 'Não foi possível carregar sua dieta. Tente novamente.',
  ONBOARDING_LOAD_ERROR: 'Não foi possível verificar seu status de cadastro. Tente recarregar a página.',
  EMPTY: 'Nenhuma recomendação encontrada.',
  COMPLETE_ASSESSMENT: 'Complete a triagem para gerar seu plano alimentar personalizado.',
  READY_TITLE: 'Tudo pronto!',
  READY_SUBTITLE: 'Seu perfil e triagem estão completos. Gere seu primeiro plano alimentar personalizado.',
  GENERATE_FIRST: 'Gerar primeira dieta',
  GENERATION_ERROR: 'Não foi possível gerar sua dieta agora.',
  GENERATION_ERROR_RETRY: 'Não foi possível gerar sua dieta agora. Clique em "Gerar nova dieta" para tentar novamente.',
  GENERATION_FIRST_ERROR_RETRY: 'Não foi possível gerar sua dieta agora. Tente novamente.',
} as const

// Honest, status-driven messages for the async generation flow. One message per real
// backend phase — no percentage, no time estimate, no "step X of Y".
export const DIET_GENERATION_STATUS_MESSAGES: Record<GenerationJob.Status, string> = {
  PENDING: 'Preparando sua solicitação...',
  PREPARING_CONTEXT: 'Analisando seu perfil nutricional...',
  GENERATING_WITH_AI: 'Gerando sua recomendação com IA...',
  SAVING_RECOMMENDATION: 'Salvando sua dieta...',
  COMPLETED: 'Dieta gerada com sucesso.',
  FAILED: DIET_MESSAGES.GENERATION_ERROR,
} as const

// Pure mapping helper — kept side-effect free so it can be unit tested without a DOM.
export function resolveGenerationStatusMessage(status: GenerationJob.Status): string {
  return DIET_GENERATION_STATUS_MESSAGES[status]
}
