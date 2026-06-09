import { ref, computed, onUnmounted } from 'vue'
import { isAxiosError } from 'axios'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { aiService } from '@/services/modules/aiRecommendation'
import { resolveGenerationStatusMessage } from '@/constants/messages/dietMessages'
import type { GenerationJob } from '@/types/api'

const ACCESS_TOKEN_KEY = 'tukan_access_token'

// Named SSE event emitted by the backend (GenerationJobSseService.EVENT_NAME).
const JOB_UPDATE_EVENT = 'job-update'

// Goes through the same Vite proxy as Axios (/api -> backend). The JWT is sent via the
// Authorization header — never as a query param — so the URL carries no secret.
function eventsUrl(jobId: string): string {
  return `/api/ai/recommendations/jobs/${jobId}/events`
}

/**
 * Drives one async diet-generation job: starts it, streams real status changes over SSE,
 * and exposes ephemeral screen state. State is local on purpose (no global store) — it lives
 * and dies with the screen that generates the diet.
 *
 * The SSE connection is torn down on every terminal/error path and on component unmount,
 * so no connection leaks across navigations or retries.
 */
export function useDietGeneration() {
  const status = ref<GenerationJob.Status | null>(null)
  const error = ref<string | null>(null)
  const recommendationId = ref<number | null>(null)

  let controller: AbortController | null = null

  const isTerminal = computed(
    () => status.value === 'COMPLETED' || status.value === 'FAILED',
  )
  const isGenerating = computed(() => status.value !== null && !isTerminal.value)
  const currentMessage = computed(() =>
    status.value ? resolveGenerationStatusMessage(status.value) : '',
  )

  function applyJob(job: GenerationJob.Response): void {
    status.value = job.status
    if (job.status === 'COMPLETED') {
      recommendationId.value = job.recommendationId ?? null
    } else if (job.status === 'FAILED') {
      error.value = job.errorMessage ?? null
    }
  }

  function cleanup(): void {
    controller?.abort()
    controller = null
  }

  /**
   * Starts (or reattaches to) a generation job and opens the SSE stream. Never rejects:
   * any failure is surfaced as a FAILED status so the screen can react uniformly.
   */
  async function start(): Promise<void> {
    cleanup()
    error.value = null
    recommendationId.value = null
    status.value = 'PENDING'

    try {
      const { job } = await aiService.startGenerationJob()
      applyJob(job)
      // A reattached job (409) may already be terminal — skip streaming in that case.
      if (isTerminal.value) {
        return
      }
      openStream(job.jobId)
    } catch (err) {
      status.value = 'FAILED'
      error.value = isAxiosError<{ message?: string }>(err)
        ? (err.response?.data?.message ?? null)
        : null
    }
  }

  function openStream(jobId: string): void {
    controller = new AbortController()
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)

    fetchEventSource(eventsUrl(jobId), {
      signal: controller.signal,
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      // Keep streaming even if the tab is backgrounded — generation is short-lived.
      openWhenHidden: true,
      onmessage(event) {
        if (event.event !== JOB_UPDATE_EVENT || !event.data) {
          return
        }
        applyJob(JSON.parse(event.data) as GenerationJob.Response)
        if (isTerminal.value) {
          // Aborting on terminal stops fetch-event-source from auto-reconnecting.
          cleanup()
        }
      },
      onerror(err) {
        // Transport-level failure before reaching a terminal status.
        // Re-throw always (stops the library's auto-retry loop), but first
        // attempt a single polling call so a FAILED job's errorMessage is not
        // silently swallowed by the transport drop.
        if (!isTerminal.value) {
          status.value = 'FAILED'
          error.value = null
          aiService.getGenerationJobStatus(jobId)
            .then((finalJob) => {
              // Guard: only apply if the user hasn't already retried (status reset).
              if (status.value === 'FAILED' && finalJob.status === 'FAILED' && finalJob.errorMessage) {
                error.value = finalJob.errorMessage
              }
            })
            .catch(() => {})
        }
        throw err
      },
    }).catch(() => {
      // Expected on abort (terminal/unmount) and on the re-thrown onerror — the FAILED
      // state, if any, is already reflected in the refs above.
    })
  }

  onUnmounted(cleanup)

  return {
    status,
    error,
    recommendationId,
    isGenerating,
    isTerminal,
    currentMessage,
    start,
    cleanup,
  }
}
