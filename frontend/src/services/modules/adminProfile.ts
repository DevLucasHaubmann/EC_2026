import { api } from '../http/api'

export const adminProfileService = {
  findByUserId: async (userId: number) => {
    const res = await api.get(`/profiles/users/${userId}`)
    return res.data
  },

  update: async (id: number, data: object) => {
    const res = await api.put(`/profiles/${id}`, data)
    return res.data
  },

  delete: async (id: number) => {
    await api.delete(`/profiles/${id}`)
  },

  create: async (userId: number, data: object) => {
    const res = await api.post(`/profiles/users/${userId}`, data)
    return res.data
  },
}
