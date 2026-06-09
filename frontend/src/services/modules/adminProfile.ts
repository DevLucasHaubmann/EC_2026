import { api } from '@/services/http/api'
import type { ProfileData, AdminProfileRequest, Page } from '@/types/admin'

export const adminProfileService = {
  findAll: async (page = 0, size = 10): Promise<Page<ProfileData>> => {
    const res = await api.get<Page<ProfileData>>(`/profiles?page=${page}&size=${size}`)
    return res.data
  },

  findById: async (id: number): Promise<ProfileData> => {
    const res = await api.get<ProfileData>(`/profiles/${id}`)
    return res.data
  },

  findByUserId: async (userId: number): Promise<ProfileData> => {
    const res = await api.get<ProfileData>(`/profiles/users/${userId}`)
    return res.data
  },

  update: async (id: number, data: AdminProfileRequest): Promise<ProfileData> => {
    const res = await api.put<ProfileData>(`/profiles/${id}`, data)
    return res.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/profiles/${id}`)
  },

  create: async (userId: number, data: AdminProfileRequest): Promise<ProfileData> => {
    const res = await api.post<ProfileData>(`/profiles/users/${userId}`, data)
    return res.data
  },
}
