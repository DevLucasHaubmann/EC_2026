import { describe, it, expect, beforeEach, vi } from 'vitest'
import type { AxiosInstance, AxiosResponse } from 'axios'
import type { BodyWeight } from '@/types/api'

// ── Module mock: replace the shared Axios instance ────────────────────────────
// bodyWeightService uses the named `api` export from @/services/http/api, so we
// intercept it at the module level and expose spies for .get and .post.
//
// vi.hoisted() is required here because vi.mock() calls are hoisted to the top
// of the file by Vitest's transform, which would place them before the const
// declarations and trigger a TDZ ReferenceError.
const { mockGet, mockPost } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
}))

vi.mock('@/services/http/api', () => ({
  api: { get: mockGet, post: mockPost } as unknown as AxiosInstance,
}))

// Import the service AFTER the mock is registered so it picks up the mock instance.
import { bodyWeightService } from '@/services/modules/bodyWeight'

// ── Helpers ───────────────────────────────────────────────────────────────────

function axiosResponse<T>(data: T): AxiosResponse<T> {
  return { data, status: 200, statusText: 'OK', headers: {}, config: {} as never }
}

function makeLog(overrides: Partial<BodyWeight.LogResponse> = {}): BodyWeight.LogResponse {
  return {
    id: 1,
    weightKg: 75,
    measuredDate: '2024-06-09',
    createdAt: '2024-06-09T10:00:00Z',
    ...overrides,
  }
}

function makeSummary(
  overrides: Partial<BodyWeight.SummaryResponse> = {},
): BodyWeight.SummaryResponse {
  return {
    currentWeightKg: 75,
    initialWeightKg: 80,
    totalChangeKg: -5,
    trend: 'LOSS',
    entryCount: 2,
    history: [makeLog({ id: 1, weightKg: 80 }), makeLog({ id: 2, weightKg: 75 })],
    ...overrides,
  }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('bodyWeightService.register', () => {
  beforeEach(() => vi.clearAllMocks())

  it('posts to /me/body-weight with the given weight and returns the log entry', async () => {
    const log = makeLog()
    mockPost.mockResolvedValue(axiosResponse(log))

    const result = await bodyWeightService.register(75)

    expect(mockPost).toHaveBeenCalledTimes(1)
    expect(mockPost).toHaveBeenCalledWith('/me/body-weight', { weightKg: 75 })
    expect(result).toEqual(log)
  })

  it('propagates the rejection when the API call fails', async () => {
    mockPost.mockRejectedValue(new Error('network error'))

    await expect(bodyWeightService.register(80)).rejects.toThrow('network error')
  })
})

describe('bodyWeightService.getHistory', () => {
  beforeEach(() => vi.clearAllMocks())

  it('calls GET /me/body-weight and returns an array of log entries', async () => {
    const history = [makeLog({ id: 1 }), makeLog({ id: 2, weightKg: 80 })]
    mockGet.mockResolvedValue(axiosResponse(history))

    const result = await bodyWeightService.getHistory()

    expect(mockGet).toHaveBeenCalledTimes(1)
    expect(mockGet).toHaveBeenCalledWith('/me/body-weight')
    expect(result).toEqual(history)
  })

  it('returns an empty array when the user has no entries', async () => {
    mockGet.mockResolvedValue(axiosResponse([]))

    const result = await bodyWeightService.getHistory()

    expect(result).toEqual([])
  })

  it('propagates the rejection when the API call fails', async () => {
    mockGet.mockRejectedValue(new Error('unauthorized'))

    await expect(bodyWeightService.getHistory()).rejects.toThrow('unauthorized')
  })
})

describe('bodyWeightService.getSummary', () => {
  beforeEach(() => vi.clearAllMocks())

  it('calls GET /me/body-weight/summary and returns the summary object', async () => {
    const summary = makeSummary()
    mockGet.mockResolvedValue(axiosResponse(summary))

    const result = await bodyWeightService.getSummary()

    expect(mockGet).toHaveBeenCalledTimes(1)
    expect(mockGet).toHaveBeenCalledWith('/me/body-weight/summary')
    expect(result).toEqual(summary)
  })

  it('returns a summary with all null fields when the user has no entries', async () => {
    const emptySummary = makeSummary({
      currentWeightKg: null,
      initialWeightKg: null,
      totalChangeKg: null,
      trend: null,
      entryCount: 0,
      history: [],
    })
    mockGet.mockResolvedValue(axiosResponse(emptySummary))

    const result = await bodyWeightService.getSummary()

    expect(result.entryCount).toBe(0)
    expect(result.currentWeightKg).toBeNull()
    expect(result.trend).toBeNull()
    expect(result.history).toEqual([])
  })

  it('propagates the rejection when the API call fails', async () => {
    mockGet.mockRejectedValue(new Error('server error'))

    await expect(bodyWeightService.getSummary()).rejects.toThrow('server error')
  })
})
