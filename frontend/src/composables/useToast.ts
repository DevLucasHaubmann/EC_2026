import { reactive } from 'vue'
import type { Toast, ToastType, ToastOptions } from '@/types/toast'

const DEFAULT_DURATION = 5000
const MAX_TOASTS = 5

const toasts = reactive<Toast[]>([])
const timers = new Map<string, ReturnType<typeof setTimeout>>()

function removeToast(id: string): void {
  const index = toasts.findIndex(t => t.id === id)
  if (index !== -1) toasts.splice(index, 1)

  const timer = timers.get(id)
  if (timer !== undefined) {
    clearTimeout(timer)
    timers.delete(id)
  }
}

function isToastPersistent(toast: Toast): boolean {
  return (toast.duration ?? DEFAULT_DURATION) === 0
}

// Eviction policy:
// 1. Remove the oldest non-persistent toast to make room.
// 2. If all are persistent and the new one is also persistent, evict the oldest persistent.
// 3. If all are persistent and the new one is not, block it (persistent toasts hold the queue).
function evictToMakeRoom(newIsPersistent: boolean): boolean {
  const idx = toasts.findIndex(t => !isToastPersistent(t))
  if (idx !== -1) {
    removeToast(toasts[idx]!.id)
    return true
  }
  if (newIsPersistent) {
    // Invariant: toasts.length >= MAX_TOASTS > 0, so toasts[0] is always defined here.
    removeToast(toasts[0]!.id)
    return true
  }
  return false
}

function addToast(type: ToastType, message: string, options?: ToastOptions): string {
  const duration = options?.duration ?? DEFAULT_DURATION

  if (toasts.length >= MAX_TOASTS) {
    if (!evictToMakeRoom(duration === 0)) return ''
  }

  const id = crypto.randomUUID()
  toasts.push({ id, type, message, title: options?.title, duration })

  if (duration > 0) {
    timers.set(id, setTimeout(() => removeToast(id), duration))
  }

  return id
}

// Used for cleanup in tests or on full page transitions.
function clearToasts(): void {
  timers.forEach(timer => clearTimeout(timer))
  timers.clear()
  toasts.splice(0)
}

export interface UseToastReturn {
  toasts: Toast[]
  success: (message: string, options?: ToastOptions) => string
  error: (message: string, options?: ToastOptions) => string
  warning: (message: string, options?: ToastOptions) => string
  info: (message: string, options?: ToastOptions) => string
  removeToast: (id: string) => void
  clearToasts: () => void
}

export function useToast(): UseToastReturn {
  return {
    toasts,
    success: (message: string, options?: ToastOptions) => addToast('success', message, options),
    error:   (message: string, options?: ToastOptions) => addToast('error', message, options),
    warning: (message: string, options?: ToastOptions) => addToast('warning', message, options),
    info:    (message: string, options?: ToastOptions) => addToast('info', message, options),
    removeToast,
    clearToasts,
  }
}
