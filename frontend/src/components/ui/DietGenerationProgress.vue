<script setup lang="ts">
// Dumb, props-driven progress panel for the async diet generation. It knows nothing about
// tokens, HTTP or SSE — it only renders the message it is given and emits `retry` when failed.
defineProps<{
  message: string
  failed: boolean
}>()

defineEmits<{
  (e: 'retry'): void
}>()
</script>

<template>
  <div class="gen-progress">
    <div v-if="!failed" class="spinner" aria-hidden="true"></div>
    <div v-else class="fail-icon" aria-hidden="true"></div>

    <p class="gen-message" :class="{ failed }" role="status" aria-live="polite">
      {{ message }}
    </p>

    <button v-if="failed" type="button" class="retry-btn" @click="$emit('retry')">
      Tentar novamente
    </button>
  </div>
</template>

<style scoped>
.gen-progress {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.25rem;
  text-align: center;
  padding: 2rem;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(16, 185, 129, 0.15);
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.fail-icon {
  width: 44px;
  height: 44px;
  background: #f87171;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2L2 22h20L12 2zm0 3.75L19.5 20H4.5L12 5.75zM11 10h2v5h-2v-5zm0 6h2v2h-2v-2z'/%3E%3C/svg%3E") no-repeat center;
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2L2 22h20L12 2zm0 3.75L19.5 20H4.5L12 5.75zM11 10h2v5h-2v-5zm0 6h2v2h-2v-2z'/%3E%3C/svg%3E") no-repeat center;
}

.gen-message {
  color: #cbd5e1;
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0;
  max-width: 440px;
  line-height: 1.5;
}
.gen-message.failed {
  color: #fca5a5;
}

.retry-btn {
  background: #10b981;
  color: #0f172a;
  border: none;
  padding: 0.9rem 2.5rem;
  border-radius: 14px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2);
}
</style>
