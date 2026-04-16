<template>
  <div class="card recommendation-card">
    <div class="header">
      <h2>Próxima Refeição Recomendada</h2>
      <span class="badge">Inteligência Tukan</span>
    </div>
    <div class="content">
      <h3 class="meal-title">{{ mealPlan.title }}</h3>
      <p class="meal-desc">{{ mealPlan.description }}</p>
      <div class="meal-meta">
        <span>⏰ {{ mealPlan.time }}</span>
        <span>🔥 {{ mealPlan.calories }}</span>
      </div>
      <button class="btn-primary">Ver Plano Completo</button>
    </div>
  </div>
</template>

<script setup>
defineProps({ mealPlan: Object })
</script>

<style scoped>
.card { background: var(--color-surface); border-radius: var(--radius-lg); padding: 1.5rem; box-shadow: var(--shadow-md); }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.header h2 { font-size: 1.125rem; font-weight: 600; margin: 0; }
.badge { background: var(--color-primary-light); color: var(--color-primary); padding: 0.25rem 0.75rem; border-radius: 99px; font-size: 0.75rem; font-weight: 600; }
.meal-title { font-size: 1.25rem; color: var(--color-primary); margin: 0 0 0.5rem 0; }
.meal-desc { color: var(--color-text-muted); line-height: 1.5; margin-bottom: 1rem; }
.meal-meta { display: flex; gap: 1rem; margin-bottom: 1.5rem; font-size: 0.875rem; color: var(--color-text-main); font-weight: 500; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 0.75rem 1.5rem; border-radius: var(--radius-md); font-weight: 600; cursor: pointer; width: 100%; transition: opacity 0.2s; }
.btn-primary:hover { opacity: 0.9; }
</style>