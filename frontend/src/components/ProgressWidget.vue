<template>
  <div class="card progress-card">
    <h3>Sua Evolução</h3>
    <p class="message">{{ progressData.message }}</p>
    
    <div class="progress-bar-container">
      <div class="progress-bar" :style="{ width: progressData.percentage + '%' }"></div>
    </div>
    
    <div class="progress-stats">
      <span>{{ progressData.current }}</span>
      <span>{{ progressData.goal }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({ progressData: Object })
</script>

<style scoped>
.card { background: var(--color-surface); border-radius: var(--radius-lg); padding: 1.5rem; box-shadow: var(--shadow-sm); border: 1px solid var(--color-border); }
h3 { font-size: 1.125rem; margin: 0 0 0.5rem 0; }
.message { font-size: 0.875rem; color: var(--color-primary); font-weight: 500; margin-bottom: 1.25rem; }
.progress-bar-container { background: var(--color-primary-light); height: 8px; border-radius: 4px; overflow: hidden; margin-bottom: 0.75rem; }
.progress-bar { background: var(--color-primary); height: 100%; border-radius: 4px; transition: width 0.5s ease; }
.progress-stats { display: flex; justify-content: space-between; font-size: 0.75rem; color: var(--color-text-muted); font-weight: 500; }
</style>