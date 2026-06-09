import { api } from '@/services/http/api'
import type { BodyWeight } from '@/types/api'

export const bodyWeightService = {
  register: async (weightKg: number): Promise<BodyWeight.LogResponse> => {
    const res = await api.post<BodyWeight.LogResponse>('/me/body-weight', { weightKg })
    return res.data
  },

  getHistory: async (): Promise<BodyWeight.LogResponse[]> => {
    const res = await api.get<BodyWeight.LogResponse[]>('/me/body-weight')
    return res.data
  },

  getSummary: async (): Promise<BodyWeight.SummaryResponse> => {
    const res = await api.get<BodyWeight.SummaryResponse>('/me/body-weight/summary')
    return res.data
  },
}
