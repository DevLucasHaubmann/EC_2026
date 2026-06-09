import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mealLogService } from '@/services/modules/mealLog'
import { api } from '@/services/http/api'

vi.mock('@/services/http/api', () => ({
  api: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockLog = {
  id: 1,
  recommendationId: 10,
  mealDate: '2026-06-08',
  mealType: 'BREAKFAST',
  optionNumber: 1,
  calories: 380,
  protein: 25,
  carbs: 40,
  fat: 10,
  notes: null,
  createdAt: '2026-06-08T10:00:00Z',
}

describe('mealLogService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('log', () => {
    it('posts to /meal-logs and returns the response', async () => {
      // given
      vi.mocked(api.post).mockResolvedValue({ data: mockLog })
      const request = {
        recommendationId: 10,
        mealDate: '2026-06-08',
        mealType: 'BREAKFAST',
        optionNumber: 1,
      }

      // when
      const result = await mealLogService.log(request)

      // then
      expect(api.post).toHaveBeenCalledWith('/meal-logs', request)
      expect(result).toEqual(mockLog)
    })

    it('forwards optional notes in the request', async () => {
      // given
      vi.mocked(api.post).mockResolvedValue({ data: { ...mockLog, notes: 'Sem sal' } })
      const request = {
        recommendationId: 10,
        mealDate: '2026-06-08',
        mealType: 'BREAKFAST',
        optionNumber: 1,
        notes: 'Sem sal',
      }

      // when
      await mealLogService.log(request)

      // then
      expect(api.post).toHaveBeenCalledWith('/meal-logs', request)
    })
  })

  describe('getByDate', () => {
    it('gets /meal-logs with date param and returns array', async () => {
      // given
      vi.mocked(api.get).mockResolvedValue({ data: [mockLog] })

      // when
      const result = await mealLogService.getByDate('2026-06-08')

      // then
      expect(api.get).toHaveBeenCalledWith('/meal-logs', { params: { date: '2026-06-08' } })
      expect(result).toEqual([mockLog])
    })

    it('returns empty array when no logs exist for the date', async () => {
      // given
      vi.mocked(api.get).mockResolvedValue({ data: [] })

      // when
      const result = await mealLogService.getByDate('2026-06-08')

      // then
      expect(result).toHaveLength(0)
    })
  })

  describe('remove', () => {
    it('deletes /meal-logs/{id} with the correct id', async () => {
      // given
      vi.mocked(api.delete).mockResolvedValue({})

      // when
      await mealLogService.remove(42)

      // then
      expect(api.delete).toHaveBeenCalledWith('/meal-logs/42')
    })
  })
})
