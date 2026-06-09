export type ModalVariant = 'default' | 'danger' | 'warning' | 'info'

export interface ModalOptions {
  title: string
  message?: string
  /** Label for the confirm button. Defaults to 'Confirmar'. */
  confirmLabel?: string
  /**
   * Label for the cancel button.
   * If omitted, no cancel button is rendered (alert-style modal).
   */
  cancelLabel?: string
  /** Visual variant. Defaults to 'default'. */
  variant?: ModalVariant
}
