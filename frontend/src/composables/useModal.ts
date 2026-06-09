import { reactive } from 'vue'
import type { ModalOptions, ModalVariant } from '@/types/modal'

export interface ModalState {
  isOpen: boolean
  title: string
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  variant?: ModalVariant
}

const state = reactive<ModalState>({
  isOpen: false,
  title: '',
})

let resolveCurrent: ((confirmed: boolean) => void) | null = null

function openModal(options: ModalOptions): Promise<boolean> {
  if (resolveCurrent) {
    resolveCurrent(false)
    resolveCurrent = null
    resetState()
  }

  return new Promise<boolean>((resolve) => {
    resolveCurrent = resolve
    state.isOpen = true
    state.title = options.title
    state.message = options.message
    state.confirmLabel = options.confirmLabel
    state.cancelLabel = options.cancelLabel
    state.variant = options.variant
  })
}

function confirmModal(): void {
  resolveCurrent?.(true)
  resolveCurrent = null
  resetState()
}

function cancelModal(): void {
  resolveCurrent?.(false)
  resolveCurrent = null
  resetState()
}

function resetState(): void {
  state.isOpen = false
  state.title = ''
  state.message = undefined
  state.confirmLabel = undefined
  state.cancelLabel = undefined
  state.variant = undefined
}

export interface UseModalReturn {
  state: ModalState
  open: (options: ModalOptions) => Promise<boolean>
  confirm: () => void
  cancel: () => void
}

export function useModal(): UseModalReturn {
  return {
    state,
    open: openModal,
    confirm: confirmModal,
    cancel: cancelModal,
  }
}
