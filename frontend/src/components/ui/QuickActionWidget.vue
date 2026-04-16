<template>
  <button class="action-btn">
    <span class="icon">{{ icon }}</span>
    <span class="label">{{ label }}</span>
  </button>
</template>

<script setup>
defineProps({ icon: String, label: String })
</script>

<style scoped>
.action-btn {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem 0.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-sm);
}
.action-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  transform: translateY(-2px);
}
.icon { font-size: 1.5rem; }
.label { font-size: 0.75rem; font-weight: 500; color: var(--color-text-main); }
</style>