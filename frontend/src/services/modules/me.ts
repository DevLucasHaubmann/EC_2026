import { api } from '@/services/http/api'
import type { MeResponse } from '@/types/api'

export const meService = {
  getMe: async (): Promise<MeResponse> => {
    const res = await api.get<MeResponse>('/me')
    return res.data
  },

  updateAvatar: async (avatarUrl: string | null): Promise<MeResponse> => {
    const res = await api.patch<MeResponse>('/me/avatar', { avatarUrl })
    return res.data
  },
}