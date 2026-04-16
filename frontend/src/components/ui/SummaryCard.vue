<template>
  <div class="card summary-card">
    <div class="card-header">
      <span class="icon">{{ icon }}</span>
      <span class="title">{{ title }}</span>
    </div>
    <div class="card-body">
      <span class="value">{{ value }}</span>
      <span class="unit">{{ unit }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({ title: String, value: String, unit: String, icon: String })
</script>

<style scoped>
.card {
  background-color: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border);
}
.card-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.75rem; }
.title { font-size: 0.875rem; color: var(--color-text-muted); font-weight: 500; }
.value { font-size: 1.5rem; font-weight: 700; color: var(--color-text-main); }
.unit { font-size: 0.875rem; color: var(--color-text-muted); margin-left: 0.25rem; }
</style>