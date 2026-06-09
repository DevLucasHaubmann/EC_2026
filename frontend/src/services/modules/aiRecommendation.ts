import { api } from '@/services/http/api'
import type { GenerationJob, Recommendation } from '@/types/api'

const HTTP_ACCEPTED = 202
const HTTP_CONFLICT = 409

export const aiService = {
  // ==========================================
  // 🤖 RECOMENDAÇÕES DA IA (Dieta)
  // ==========================================

  // GET /ai/recommendations/me/latest - Pega a dieta atual/mais recente do usuário
  getLatest: async (): Promise<Recommendation.Response> => {
    const res = await api.get<Recommendation.Response>('/ai/recommendations/me/latest')
    return res.data
  },

  // ==========================================
  // ⚙️ GERAÇÃO ASSÍNCRONA (Job + SSE)
  // ==========================================

  // POST /ai/recommendations/jobs - Inicia a geração assíncrona.
  // 202: job recém-criado (created=true). 409: já existe job ativo, reanexar (created=false).
  startGenerationJob: async (): Promise<GenerationJob.StartResult> => {
    const res = await api.post<GenerationJob.Response>('/ai/recommendations/jobs', null, {
      validateStatus: (status) => status === HTTP_ACCEPTED || status === HTTP_CONFLICT,
    })
    return { job: res.data, created: res.status === HTTP_ACCEPTED }
  },

  // GET /ai/recommendations/jobs/{jobId} - Estado atual do job (fallback de polling se o SSE cair).
  getGenerationJobStatus: async (jobId: string): Promise<GenerationJob.Response> => {
    const res = await api.get<GenerationJob.Response>(`/ai/recommendations/jobs/${jobId}`)
    return res.data
  },

  // GET /ai/recommendations/me - Histórico de todas as dietas geradas para o usuário
  getHistory: async (): Promise<Recommendation.Response[]> => {
    const res = await api.get<Recommendation.Response[]>('/ai/recommendations/me')
    return res.data
  },

  // GET /ai/recommendations/{id} - Busca uma dieta específica pelo ID
  getById: async (id: number): Promise<Recommendation.Response> => {
    const res = await api.get<Recommendation.Response>(`/ai/recommendations/${id}`)
    return res.data
  },

  // PATCH /ai/recommendations/{id}/viewed - Marca a dieta como "vista"
  markAsViewed: async (id: number): Promise<Recommendation.Response> => {
    const res = await api.patch<Recommendation.Response>(`/ai/recommendations/${id}/viewed`)
    return res.data
  },

  // POST /ai/recommendations/{id}/feedback - Envia ou atualiza feedback da dieta (upsert)
  addFeedback: async (id: number, feedback: Recommendation.FeedbackRequest): Promise<Recommendation.FeedbackResponse> => {
    const res = await api.post<Recommendation.FeedbackResponse>(`/ai/recommendations/${id}/feedback`, feedback)
    return res.data
  },

  // GET /ai/recommendations/{id}/feedback - Retorna o feedback existente (null se não houver)
  getFeedback: async (id: number): Promise<Recommendation.FeedbackResponse | null> => {
    const res = await api.get<Recommendation.FeedbackResponse>(`/ai/recommendations/${id}/feedback`, {
      validateStatus: (status) => status === 200 || status === 204,
    })
    return res.status === 204 ? null : res.data
  },

  // PATCH /ai/recommendations/{id}/archive - Arquiva uma dieta antiga
  archive: async (id: number): Promise<Recommendation.Response> => {
    const res = await api.patch<Recommendation.Response>(`/ai/recommendations/${id}/archive`)
    return res.data
  }
}
