import { api } from '@/services/http/api'
import type { MealLog } from '@/types/api'

export const mealLogService = {
  log: async (data: MealLog.LogRequest): Promise<MealLog.Response> => {
    const res = await api.post<MealLog.Response>('/meal-logs', data)
    return res.data
  },
  getByDate: async (date: string): Promise<MealLog.Response[]> => {
    const res = await api.get<MealLog.Response[]>('/meal-logs', { params: { date } })
    return res.data
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`/meal-logs/${id}`)
  },
}
