import { api } from '@/services/http/api'
import type { CreateProfileRequest, UpdateProfileRequest } from '@/types/profile'
import type { MeProfileResponse, ProfileCreatedResponse } from '@/types/api'

export const profileService = {
  createOwn: async (data: CreateProfileRequest): Promise<ProfileCreatedResponse> => {
    const res = await api.post<ProfileCreatedResponse>('/profiles/me', data)
    return res.data
  },

  getOwn: async (): Promise<MeProfileResponse> => {
    const res = await api.get<MeProfileResponse>('/profiles/me')
    return res.data
  },

  updateOwn: async (data: UpdateProfileRequest): Promise<MeProfileResponse> => {
    const res = await api.put<MeProfileResponse>('/profiles/me', data)
    return res.data
  },
}
