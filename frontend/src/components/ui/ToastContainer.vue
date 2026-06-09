<script setup lang="ts">
import { computed } from 'vue'
import type { ToastType } from '@/types/toast'
import { useToast } from '@/composables/useToast'

const { toasts, removeToast } = useToast()

const errorToasts   = computed(() => toasts.filter(t => t.type === 'error'))
const generalToasts = computed(() => toasts.filter(t => t.type !== 'error'))

const ICONS: Record<ToastType, string> = {
  success: '✓',
  error:   '✕',
  warning: '⚠',
  info:    'ℹ',
}

const LABELS: Record<ToastType, string> = {
  success: 'Sucesso',
  error:   'Erro',
  warning: 'Aviso',
  info:    'Informação',
}

function closeLabel(message: string, title?: string): string {
  return title ? `Fechar notificação: ${title}` : `Fechar notificação: ${message}`
}
</script>

<template>
  <div class="toast-portal" aria-label="Notificações">
    <!-- Errors announced immediately -->
    <div
      class="toast-region"
      role="alert"
      aria-live="assertive"
      aria-atomic="false"
    >
      <TransitionGroup name="toast" tag="div" class="toast-group">
        <div
          v-for="toast in errorToasts"
          :key="toast.id"
          class="toast toast--error"
        >
          <span class="toast__icon" aria-hidden="true">{{ ICONS[toast.type] }}</span>
          <div class="toast__body">
            <span class="sr-only">{{ LABELS[toast.type] }}: </span>
            <strong v-if="toast.title" class="toast__title">{{ toast.title }}</strong>
            <p class="toast__message">{{ toast.message }}</p>
          </div>
          <button
            class="toast__close"
            type="button"
            :aria-label="closeLabel(toast.message, toast.title)"
            @click="removeToast(toast.id)"
          >✕</button>
        </div>
      </TransitionGroup>
    </div>

    <!-- Success / warning / info announced politely -->
    <div
      class="toast-region"
      role="status"
      aria-live="polite"
      aria-atomic="false"
    >
      <TransitionGroup name="toast" tag="div" class="toast-group">
        <div
          v-for="toast in generalToasts"
          :key="toast.id"
          :class="['toast', `toast--${toast.type}`]"
        >
          <span class="toast__icon" aria-hidden="true">{{ ICONS[toast.type] }}</span>
          <div class="toast__body">
            <span class="sr-only">{{ LABELS[toast.type] }}: </span>
            <strong v-if="toast.title" class="toast__title">{{ toast.title }}</strong>
            <p class="toast__message">{{ toast.message }}</p>
          </div>
          <button
            class="toast__close"
            type="button"
            :aria-label="closeLabel(toast.message, toast.title)"
            @click="removeToast(toast.id)"
          >✕</button>
        </div>
      </TransitionGroup>
    </div>
  </div>
</template>

<style scoped>
.toast-portal {
  position: fixed;
  top: 1.25rem;
  right: 1.25rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 22rem;
  max-width: calc(100vw - 2.5rem);
  pointer-events: none;
}

.toast-region {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.toast-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: 0.5rem;
  border-left: 4px solid transparent;
  background: #1e293b;
  color: #f1f5f9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  pointer-events: all;
  font-size: 0.875rem;
  line-height: 1.4;
}

.toast--success { border-left-color: #22c55e; }
.toast--error   { border-left-color: #ef4444; }
.toast--warning { border-left-color: #f59e0b; }
.toast--info    { border-left-color: #3b82f6; }

.toast__icon {
  flex-shrink: 0;
  font-size: 1rem;
  margin-top: 0.05rem;
}

.toast--success .toast__icon { color: #22c55e; }
.toast--error   .toast__icon { color: #ef4444; }
.toast--warning .toast__icon { color: #f59e0b; }
.toast--info    .toast__icon { color: #3b82f6; }

.toast__body {
  flex: 1;
  min-width: 0;
}

.toast__title {
  display: block;
  font-weight: 600;
  margin-bottom: 0.2rem;
}

.toast__message {
  margin: 0;
  word-break: break-word;
}

.toast__close {
  flex-shrink: 0;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  font-size: 0.875rem;
  line-height: 1;
  transition: color 0.15s;
}

.toast__close:hover { color: #f1f5f9; }

/* Screen-reader only */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* TransitionGroup animations */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(1.5rem);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(1.5rem);
}

.toast-move {
  transition: transform 0.25s ease;
}
</style>
