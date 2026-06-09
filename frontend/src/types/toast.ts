export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  type: ToastType
  message: string
  title?: string
  /** Duration in ms. 0 = no auto-dismiss. Defaults to 5000. */
  duration?: number
}

export interface ToastOptions {
  title?: string
  duration?: number
}
