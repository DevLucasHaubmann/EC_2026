import { api } from '../http/api'

export const assessmentService = {
  // ==========================================
  // 👤 USUÁRIO NORMAL (Self-Service)
  // ==========================================

  // POST /assessments/me - Cria a própria triagem
  createOwn: async (data: any) => {
    const res = await api.post('/assessments/me', data)
    return res.data
  },

  // GET /assessments/me - Busca a própria triagem
  getOwn: async () => {
    const res = await api.get('/assessments/me')
    return res.data
  },

  // PUT /assessments/me - Atualiza a própria triagem (objetivo, alergias)
  updateOwn: async (data: any) => {
    const res = await api.put('/assessments/me', data)
    return res.data
  },

  // ==========================================
  // ⚙️ ADMINISTRADOR (Requer Role ADMIN)
  // ==========================================

  // GET /assessments - Lista todas as triagens (Paginado)
  findAll: async (page = 0, size = 10) => {
    const res = await api.get(`/assessments?page=${page}&size=${size}`)
    return res.data
  },

  // GET /assessments/{id} - Busca uma triagem específica
  findById: async (id: number) => {
    const res = await api.get(`/assessments/${id}`)
    return res.data
  },

  // POST /assessments/users/{userId} - Cria triagem para outro usuário
  createForUser: async (userId: number, data: any) => {
    const res = await api.post(`/assessments/users/${userId}`, data)
    return res.data
  },

  // PUT /assessments/{id} - Edita triagem de outro usuário
  update: async (id: number, data: any) => {
    const res = await api.put(`/assessments/${id}`, data)
    return res.data
  },

  // DELETE /assessments/{id} - Deleta uma triagem
  delete: async (id: number) => {
    await api.delete(`/assessments/${id}`)
  }
}