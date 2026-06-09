import { api } from '@/services/http/api'
import type { Evolution } from '@/types/api'

export const evolutionService = {
  getDaily: async (date: string): Promise<Evolution.DailyMetrics> => {
    const res = await api.get<Evolution.DailyMetrics>('/evolution/daily', {
      params: { date },
    })
    return res.data
  },
  getWeekly: async (weekStart: string): Promise<Evolution.WeeklySummary> => {
    const res = await api.get<Evolution.WeeklySummary>('/evolution/weekly', {
      params: { weekStart },
    })
    return res.data
  },
}
