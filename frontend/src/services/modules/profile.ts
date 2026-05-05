import { api } from '../http/api'

export const profileService = {
  // ==========================================
  // 👤 USUÁRIO NORMAL (Self-Service)
  // ==========================================

  // POST /profiles/me - Cria o próprio perfil
  createOwn: async (data: any) => {
    const res = await api.post('/profiles/me', data)
    return res.data
  },

  // GET /profiles/me - Busca o próprio perfil
  getOwn: async () => {
    const res = await api.get('/profiles/me')
    return res.data
  },

  // PUT /profiles/me - Atualiza o próprio perfil (peso, altura, etc)
  updateOwn: async (data: any) => {
    const res = await api.put('/profiles/me', data)
    return res.data
  },

  // ==========================================
  // ⚙️ ADMINISTRADOR (Requer Role ADMIN)
  // ==========================================

  // GET /profiles - Lista todos os perfis (Paginado)
  findAll: async (page = 0, size = 10) => {
    const res = await api.get(`/profiles?page=${page}&size=${size}`)
    return res.data
  },

  // GET /profiles/{id} - Busca um perfil específico
  findById: async (id: number) => {
    const res = await api.get(`/profiles/${id}`)
    return res.data
  },

  // POST /profiles/users/{userId} - Cria perfil para outro usuário
  createForUser: async (userId: number, data: any) => {
    const res = await api.post(`/profiles/users/${userId}`, data)
    return res.data
  },

  // PUT /profiles/{id} - Edita perfil de outro usuário
  update: async (id: number, data: any) => {
    const res = await api.put(`/profiles/${id}`, data)
    return res.data
  },

  // DELETE /profiles/{id} - Deleta um perfil
  delete: async (id: number) => {
    await api.delete(`/profiles/${id}`)
  }
}