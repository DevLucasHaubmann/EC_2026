// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import type { GenerationJob } from '@/types/api'

// ── Hoisted mock handles (must be declared before vi.mock calls) ──────────────

type SseOpts = {
  signal?: AbortSignal
  onmessage?: (event: { event: string; data: string }) => void
  onerror?: (err: unknown) => void
}

const hoisted = vi.hoisted(() => ({
  mockFetchEventSource: vi.fn<[string, SseOpts], Promise<void>>(),
  mockStartJob: vi.fn<[], Promise<GenerationJob.StartResult>>(),
  mockGetJobStatus: vi.fn<[string], Promise<GenerationJob.Response>>(),
}))

vi.mock('@microsoft/fetch-event-source', () => ({
  fetchEventSource: hoisted.mockFetchEventSource,
}))

vi.mock('@/services/modules/aiRecommendation', () => ({
  aiService: {
    startGenerationJob: hoisted.mockStartJob,
    getGenerationJobStatus: hoisted.mockGetJobStatus,
  },
}))

// ── Import after mocks are registered ────────────────────────────────────────

import { useDietGeneration } from '@/composables/useDietGeneration'

// ── Shared SSE callback capture ───────────────────────────────────────────────

// Populated on each test's openStream call via mockFetchEventSource.mockImplementation.
let sse: SseOpts = {}

// ── Helpers ───────────────────────────────────────────────────────────────────

function withSetup() {
  let composable!: ReturnType<typeof useDietGeneration>
  const wrapper = mount(
    defineComponent({
      setup() {
        composable = useDietGeneration()
        return {}
      },
      render: () => h('div'),
    }),
  )
  return { get: () => composable, wrapper }
}

function makeJob(overrides: Partial<GenerationJob.Response> = {}): GenerationJob.Response {
  return {
    jobId: 'job-1',
    status: 'PENDING',
    recommendationId: null,
    errorMessage: null,
    ...overrides,
  }
}

function simulateSseEvent(job: GenerationJob.Response): void {
  sse.onmessage?.({ event: 'job-update', data: JSON.stringify(job) })
}

function simulateSseTransportError(): void {
  try {
    sse.onerror?.(new Error('SSE transport failure'))
  } catch {
    // onerror re-throws to stop the library's retry loop — expected in tests
  }
}

// ── Per-test setup ────────────────────────────────────────────────────────────

beforeEach(() => {
  sse = {}
  vi.clearAllMocks()
  hoisted.mockFetchEventSource.mockImplementation((_url, opts) => {
    sse = opts
    return Promise.resolve()
  })
  localStorage.clear()
})

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('useDietGeneration', () => {
  describe('HTTP start error', () => {
    it('surfaces backend errorMessage when job start fails with one', async () => {
      hoisted.mockStartJob.mockRejectedValue({
        isAxiosError: true,
        response: { status: 400, data: { message: 'Calorie floor too low for your profile.' } },
      })
      const { get } = withSetup()

      await get().start()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBe('Calorie floor too low for your profile.')
    })

    it('uses null error when HTTP failure carries no backend message', async () => {
      hoisted.mockStartJob.mockRejectedValue({
        isAxiosError: true,
        response: { status: 500, data: {} },
      })
      const { get } = withSetup()

      await get().start()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBeNull()
    })

    it('uses null error for non-Axios errors (network timeout etc.)', async () => {
      hoisted.mockStartJob.mockRejectedValue(new Error('Network timeout'))
      const { get } = withSetup()

      await get().start()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBeNull()
    })

    it('exposes only the message field — no raw JSON, stack trace or secrets', async () => {
      hoisted.mockStartJob.mockRejectedValue({
        isAxiosError: true,
        response: {
          status: 400,
          data: { message: 'Mensagem segura.', stackTrace: 'at Service.run(Service.java:42)' },
        },
      })
      const { get } = withSetup()

      await get().start()

      expect(get().error.value).toBe('Mensagem segura.')
      expect(get().error.value).not.toContain('stackTrace')
      expect(get().error.value).not.toContain('{')
    })
  })

  describe('SSE job events', () => {
    beforeEach(() => {
      hoisted.mockStartJob.mockResolvedValue({
        created: true,
        job: makeJob({ status: 'PENDING', jobId: 'job-1' }),
      })
    })

    it('surfaces backend errorMessage when FAILED SSE event carries one', async () => {
      const { get } = withSetup()
      await get().start()

      simulateSseEvent(makeJob({ status: 'FAILED', errorMessage: 'Perfil nutricional insuficiente.' }))
      await nextTick()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBe('Perfil nutricional insuficiente.')
    })

    it('uses null error when FAILED SSE event has no errorMessage', async () => {
      const { get } = withSetup()
      await get().start()

      simulateSseEvent(makeJob({ status: 'FAILED', errorMessage: null }))
      await nextTick()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBeNull()
    })

    it('sets recommendationId and clears error on COMPLETED', async () => {
      const { get } = withSetup()
      await get().start()

      simulateSseEvent(makeJob({ status: 'COMPLETED', recommendationId: 42 }))
      await nextTick()

      expect(get().status.value).toBe('COMPLETED')
      expect(get().recommendationId.value).toBe(42)
      expect(get().error.value).toBeNull()
    })

    it('does not overwrite errorMessage when transport error fires after a FAILED event', async () => {
      const { get } = withSetup()
      await get().start()

      simulateSseEvent(makeJob({ status: 'FAILED', errorMessage: 'Dieta não pôde ser gerada: perfil incompleto.' }))
      await nextTick()

      // Transport error arriving after a terminal event must not touch the received message
      simulateSseTransportError()
      await nextTick()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBe('Dieta não pôde ser gerada: perfil incompleto.')
    })
  })

  describe('SSE transport error — polling recovery', () => {
    beforeEach(() => {
      hoisted.mockStartJob.mockResolvedValue({
        created: true,
        job: makeJob({ status: 'PENDING', jobId: 'job-1' }),
      })
    })

    it('recovers backend errorMessage via polling when SSE drops before a terminal event', async () => {
      hoisted.mockGetJobStatus.mockResolvedValue(
        makeJob({ status: 'FAILED', errorMessage: 'Floor calórico insuficiente para o perfil.' }),
      )
      const { get } = withSetup()
      await get().start()

      simulateSseTransportError()
      await flushPromises()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBe('Floor calórico insuficiente para o perfil.')
    })

    it('keeps null error when polling also fails after SSE transport drop', async () => {
      hoisted.mockGetJobStatus.mockRejectedValue(new Error('polling network error'))
      const { get } = withSetup()
      await get().start()

      simulateSseTransportError()
      await flushPromises()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBeNull()
    })

    it('keeps null error when polling returns FAILED with no errorMessage', async () => {
      hoisted.mockGetJobStatus.mockResolvedValue(
        makeJob({ status: 'FAILED', errorMessage: null }),
      )
      const { get } = withSetup()
      await get().start()

      simulateSseTransportError()
      await flushPromises()

      expect(get().status.value).toBe('FAILED')
      expect(get().error.value).toBeNull()
    })

    it('does not apply polling result if user retried (start() reset state)', async () => {
      let resolvePolling!: (v: GenerationJob.Response) => void
      hoisted.mockGetJobStatus.mockReturnValue(
        new Promise<GenerationJob.Response>((res) => { resolvePolling = res }),
      )
      // Second start returns PENDING (simulating a new job)
      hoisted.mockStartJob
        .mockResolvedValueOnce({ created: true, job: makeJob({ status: 'PENDING' }) })
        .mockResolvedValueOnce({ created: true, job: makeJob({ status: 'PENDING' }) })

      const { get } = withSetup()
      await get().start()

      simulateSseTransportError() // polling pending...

      // User retries before polling resolves
      await get().start()
      expect(get().error.value).toBeNull() // reset by retry

      // Now the stale polling resolves
      resolvePolling(makeJob({ status: 'FAILED', errorMessage: 'Stale error, must be ignored.' }))
      await flushPromises()

      // The guard (status.value !== 'FAILED' at polling time) prevents the stale message
      expect(get().error.value).toBeNull()
      expect(get().status.value).toBe('PENDING')
    })
  })

  describe('state reset on retry', () => {
    it('clears error and starts fresh when start() is called again', async () => {
      hoisted.mockStartJob
        .mockRejectedValueOnce({
          isAxiosError: true,
          response: { status: 400, data: { message: 'Primeiro erro.' } },
        })
        .mockResolvedValue({ created: true, job: makeJob({ status: 'PENDING' }) })

      const { get } = withSetup()
      await get().start()
      expect(get().error.value).toBe('Primeiro erro.')
      expect(get().status.value).toBe('FAILED')

      await get().start()

      expect(get().error.value).toBeNull()
      expect(get().status.value).toBe('PENDING')
    })
  })
})
