<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue'
import { useModal } from '@/composables/useModal'

const { state, confirm, cancel } = useModal()

const TITLE_ID = 'modal-dialog-title'
const dialogRef = ref<HTMLElement | null>(null)
const previousFocus = ref<HTMLElement | null>(null)

watch(
  () => state.isOpen,
  async (isOpen) => {
    if (isOpen) {
      previousFocus.value = document.activeElement as HTMLElement | null
      document.body.style.overflow = 'hidden'
      await nextTick()
      dialogRef.value?.focus()
    } else {
      document.body.style.overflow = ''
      previousFocus.value?.focus()
      previousFocus.value = null
    }
  },
)

onUnmounted(() => {
  document.body.style.overflow = ''
})

function trapFocus(event: KeyboardEvent): void {
  if (!dialogRef.value) return

  const focusable = Array.from(
    dialogRef.value.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ),
  )
  if (focusable.length === 0) return

  const first = focusable[0]!
  const last = focusable[focusable.length - 1]!

  if (event.shiftKey) {
    if (document.activeElement === first) {
      last.focus()
      event.preventDefault()
    }
  } else {
    if (document.activeElement === last) {
      first.focus()
      event.preventDefault()
    }
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="state.isOpen" class="modal-overlay" @click.self="cancel">
        <div
          ref="dialogRef"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="TITLE_ID"
          class="modal-dialog"
          :class="`modal-dialog--${state.variant ?? 'default'}`"
          tabindex="-1"
          @keydown.esc.prevent="cancel"
          @keydown.tab="trapFocus"
        >
          <header class="modal-header">
            <h2 :id="TITLE_ID" class="modal-title">{{ state.title }}</h2>
            <button class="modal-close" type="button" aria-label="Fechar modal" @click="cancel">
              <span aria-hidden="true">&times;</span>
            </button>
          </header>

          <div v-if="state.message" class="modal-body">
            <p class="modal-message">{{ state.message }}</p>
          </div>

          <footer class="modal-footer">
            <button
              v-if="state.cancelLabel !== undefined"
              type="button"
              class="modal-btn modal-btn--cancel"
              @click="cancel"
            >
              {{ state.cancelLabel }}
            </button>
            <button
              type="button"
              class="modal-btn modal-btn--confirm"
              :class="`modal-btn--${state.variant ?? 'default'}`"
              @click="confirm"
            >
              {{ state.confirmLabel ?? 'Confirmar' }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  padding: 1rem;
}

.modal-dialog {
  background: #1e293b;
  border-radius: 0.75rem;
  width: 100%;
  max-width: 28rem;
  border: 1px solid #334155;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  outline: none;
}

.modal-dialog--default { border-top: 3px solid #3b82f6; }
.modal-dialog--danger  { border-top: 3px solid #ef4444; }
.modal-dialog--warning { border-top: 3px solid #f59e0b; }
.modal-dialog--info    { border-top: 3px solid #06b6d4; }

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem 1rem;
  border-bottom: 1px solid #334155;
}

.modal-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #f1f5f9;
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  font-size: 1.5rem;
  line-height: 1;
  padding: 0.25rem 0.375rem;
  border-radius: 0.25rem;
  transition: color 0.15s;
}
.modal-close:hover         { color: #f1f5f9; }
.modal-close:focus-visible { outline: 2px solid #3b82f6; outline-offset: 2px; }

.modal-body {
  padding: 1.25rem 1.5rem;
}

.modal-message {
  margin: 0;
  color: #94a3b8;
  font-size: 0.9375rem;
  line-height: 1.6;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem 1.25rem;
}

.modal-btn {
  padding: 0.5rem 1.25rem;
  border-radius: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.modal-btn:focus-visible { outline: 2px solid #3b82f6; outline-offset: 2px; }

.modal-btn--cancel {
  background: transparent;
  color: #94a3b8;
  border-color: #475569;
}
.modal-btn--cancel:hover {
  background: #334155;
  color: #f1f5f9;
}

.modal-btn--confirm.modal-btn--default { background: #3b82f6; color: #fff; }
.modal-btn--confirm.modal-btn--default:hover { background: #2563eb; }

.modal-btn--confirm.modal-btn--danger  { background: #ef4444; color: #fff; }
.modal-btn--confirm.modal-btn--danger:hover  { background: #dc2626; }

.modal-btn--confirm.modal-btn--warning { background: #f59e0b; color: #0f172a; }
.modal-btn--confirm.modal-btn--warning:hover { background: #d97706; }

.modal-btn--confirm.modal-btn--info    { background: #06b6d4; color: #fff; }
.modal-btn--confirm.modal-btn--info:hover    { background: #0891b2; }

/* Transition: overlay fades, dialog scales */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .modal-dialog,
.modal-leave-active .modal-dialog {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-dialog,
.modal-leave-to .modal-dialog {
  transform: scale(0.95) translateY(-0.5rem);
  opacity: 0;
}
</style>
