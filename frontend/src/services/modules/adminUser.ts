import { api } from '@/services/http/api'
import type { AdminUser, AdminUpdateUserRequest, Page } from '@/types/admin'

export const adminUserService = {
  // ==========================================
  // ⚙️ ADMINISTRADOR (Requer Role ADMIN)
  // ==========================================

  // GET /users - Lista todos os usuários do sistema (Paginado).
  // `q` (opcional) filtra por nome ou email no backend, preservando a paginação.
  findAll: async (page = 0, size = 10, q = ''): Promise<Page<AdminUser>> => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    const term = q.trim()
    if (term) params.set('q', term)
    const res = await api.get<Page<AdminUser>>(`/users?${params.toString()}`)
    return res.data
  },

  // GET /users/{id} - Busca detalhes de um usuário específico
  findById: async (id: number): Promise<AdminUser> => {
    const res = await api.get<AdminUser>(`/users/${id}`)
    return res.data
  },

  // GET /users/search - Busca usuário pelo email
  searchByEmail: async (email: string): Promise<AdminUser> => {
    const params = new URLSearchParams({ email })
    const res = await api.get<AdminUser>(`/users/search?${params.toString()}`)
    return res.data
  },

  // PUT /users/{id} - Edita os dados de um usuário (nome, tipo, status)
  update: async (id: number, data: AdminUpdateUserRequest): Promise<AdminUser> => {
    const res = await api.put<AdminUser>(`/users/${id}`, data)
    return res.data
  },

  // DELETE /users/{id} - Exclui permanentemente um usuário (e todas as dietas dele)
  delete: async (id: number): Promise<void> => {
    await api.delete(`/users/${id}`)
  },

  // DELETE /users/{id}/sessions - Desloga o usuário à força de todos os aparelhos
  revokeSessions: async (id: number): Promise<void> => {
    await api.delete(`/users/${id}/sessions`)
  }
}
