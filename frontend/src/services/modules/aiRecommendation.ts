import { api } from '../http/api'

export const aiService = {
  // ==========================================
  // 🤖 RECOMENDAÇÕES DA IA (Dieta)
  // ==========================================

  // POST /ai/recommendations/me - Pede para a IA gerar uma NOVA dieta agora
  generateNew: async () => {
    const res = await api.post('/ai/recommendations/me')
    return res.data
  },

  // GET /ai/recommendations/me/latest - Pega a dieta atual/mais recente do usuário
  getLatest: async () => {
    const res = await api.get('/ai/recommendations/me/latest')
    return res.data
  },

  // GET /ai/recommendations/me - Histórico de todas as dietas geradas para o usuário
  getHistory: async () => {
    const res = await api.get('/ai/recommendations/me')
    return res.data
  },

  // GET /ai/recommendations/{id} - Busca uma dieta específica pelo ID
  getById: async (id: number) => {
    const res = await api.get(`/ai/recommendations/${id}`)
    return res.data
  },

  // PATCH /ai/recommendations/{id}/viewed - Marca a dieta como "vista"
  markAsViewed: async (id: number) => {
    const res = await api.patch(`/ai/recommendations/${id}/viewed`)
    return res.data
  },

  // POST /ai/recommendations/{id}/feedback - Envia feedback (Gostei/Não Gostei) da dieta
  addFeedback: async (id: number, feedback: { rating: 'LIKED' | 'DISLIKED', reason?: string, observation?: string }) => {
    const res = await api.post(`/ai/recommendations/${id}/feedback`, feedback)
    return res.data
  },

  // PATCH /ai/recommendations/{id}/archive - Arquiva uma dieta antiga
  archive: async (id: number) => {
    const res = await api.patch(`/ai/recommendations/${id}/archive`)
    return res.data
  }
}