import { api } from '../http/api'

export const adminUserService = {
  // ==========================================
  // ⚙️ ADMINISTRADOR (Requer Role ADMIN)
  // ==========================================

  // GET /users - Lista todos os usuários do sistema (Paginado)
  findAll: async (page = 0, size = 10) => {
    const res = await api.get(`/users?page=${page}&size=${size}`)
    return res.data
  },

  // GET /users/{id} - Busca detalhes de um usuário específico
  findById: async (id: number) => {
    const res = await api.get(`/users/${id}`)
    return res.data
  },

  // GET /users/search - Busca usuário pelo email
  searchByEmail: async (email: string) => {
    const res = await api.get(`/users/search?email=${email}`)
    return res.data
  },

  // PUT /users/{id} - Edita os dados de um usuário (nome, tipo, status)
  update: async (id: number, data: any) => {
    const res = await api.put(`/users/${id}`, data)
    return res.data
  },

  // DELETE /users/{id} - Exclui permanentemente um usuário (e todas as dietas dele)
  delete: async (id: number) => {
    await api.delete(`/users/${id}`)
  },

  // DELETE /users/{id}/sessions - Desloga o usuário à força de todos os aparelhos
  revokeSessions: async (id: number) => {
    await api.delete(`/users/${id}/sessions`)
  }
}