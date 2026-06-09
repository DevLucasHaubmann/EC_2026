import { api } from '@/services/http/api'
import type { Activity } from '@/types/api'

export const activityService = {
  recordAccess: async (): Promise<Activity.StreakResponse> => {
    const res = await api.post<Activity.StreakResponse>('/me/activity')
    return res.data
  },
}
