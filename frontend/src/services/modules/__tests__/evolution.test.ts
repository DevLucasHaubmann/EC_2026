import { describe, it, expect, vi, beforeEach } from 'vitest'
import { evolutionService } from '@/services/modules/evolution'
import { api } from '@/services/http/api'

vi.mock('@/services/http/api', () => ({
  api: {
    get: vi.fn(),
  },
}))

const mockDailyMetrics = {
  date: '2026-06-08',
  consumedCalories: 1060,
  consumedProtein: 75,
  consumedCarbs: 110,
  consumedFat: 25,
  completedMeals: 2,
  plannedMeals: 3,
  adherencePercentage: 66.7,
}

const mockWeeklySummary = {
  weekStart: '2026-06-01',
  weekEnd: '2026-06-07',
  days: [],
  averageAdherencePercentage: 75.0,
  totalCalories: 5000.0,
  activeDays: 5,
}

describe('evolutionService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getDaily', () => {
    it('calls GET /evolution/daily with date param and returns single DailyMetrics', async () => {
      // given
      vi.mocked(api.get).mockResolvedValue({ data: mockDailyMetrics })

      // when
      const result = await evolutionService.getDaily('2026-06-08')

      // then
      expect(api.get).toHaveBeenCalledWith('/evolution/daily', { params: { date: '2026-06-08' } })
      expect(result).toEqual(mockDailyMetrics)
    })

    it('returns null adherencePercentage for day without planned meals', async () => {
      // given — backend returns null when no recommendation was active
      vi.mocked(api.get).mockResolvedValue({
        data: { ...mockDailyMetrics, plannedMeals: 0, adherencePercentage: null },
      })

      // when
      const result = await evolutionService.getDaily('2026-05-01')

      // then
      expect(result.plannedMeals).toBe(0)
      expect(result.adherencePercentage).toBeNull()
    })
  })

  describe('getWeekly', () => {
    it('calls GET /evolution/weekly with weekStart param', async () => {
      // given
      vi.mocked(api.get).mockResolvedValue({ data: mockWeeklySummary })

      // when
      const result = await evolutionService.getWeekly('2026-06-01')

      // then
      expect(api.get).toHaveBeenCalledWith('/evolution/weekly', { params: { weekStart: '2026-06-01' } })
      expect(result).toEqual(mockWeeklySummary)
    })

    it('returns null averageAdherencePercentage when no meals logged in the week', async () => {
      // given
      vi.mocked(api.get).mockResolvedValue({
        data: { ...mockWeeklySummary, averageAdherencePercentage: null, activeDays: 0 },
      })

      // when
      const result = await evolutionService.getWeekly('2026-06-01')

      // then
      expect(result.averageAdherencePercentage).toBeNull()
      expect(result.activeDays).toBe(0)
    })
  })
})
