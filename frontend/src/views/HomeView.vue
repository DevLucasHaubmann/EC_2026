<template>
  <div class="tukan-layout">
    <TopBar />

    <main class="main-content">
      <GreetingHero :userName="currentUser" />

      <section class="overview-grid">
        <SummaryCard 
          v-for="stat in healthStats" 
          :key="stat.id" 
          :title="stat.title" 
          :value="stat.value" 
          :unit="stat.unit"
          :icon="stat.icon"
        />
      </section>

      <div class="content-split">
        <section class="primary-column">
          <RecommendationWidget :mealPlan="todayPlan" />
        </section>

        <section class="secondary-column">
          <ProgressWidget :progressData="userProgress" />
          
          <div class="quick-actions-container">
            <h3 class="section-title">Ações Rápidas</h3>
            <div class="actions-grid">
              <QuickActionWidget icon="🍽️" label="Nova Refeição" />
              <QuickActionWidget icon="💧" label="Beber Água" />
              <QuickActionWidget icon="⚖️" label="Atualizar Peso" />
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TopBar from '../components/layout/TopBar.vue'
import GreetingHero from '../components/GreetingHero.vue'
import SummaryCard from '../components/ui/SummaryCard.vue'
import RecommendationWidget from '../components/RecommendationWidget.vue'
import ProgressWidget from '../components/ProgressWidget.vue'
import QuickActionWidget from '../components/ui/QuickActionWidget.vue'

// Mock Data
const currentUser = ref('Lucas')

const healthStats = ref([
  { id: 1, title: 'Calorias Restantes', value: '1.240', unit: 'kcal', icon: '🔥' },
  { id: 2, title: 'Água Consumida', value: '1.5', unit: 'Litros', icon: '💧' },
  { id: 3, title: 'Horas de Sono', value: '7.5', unit: 'Horas', icon: '🌙' }
])

const todayPlan = ref({
  title: 'Almoço Foco Imunidade',
  description: 'Salada de quinoa com frango grelhado e mix de folhas verdes.',
  time: '12:30',
  calories: '450 kcal'
})

const userProgress = ref({
  message: 'Você perdeu 2kg nos últimos 30 dias!',
  percentage: 65,
  goal: 'Meta: 75kg',
  current: 'Atual: 78kg'
})
</script>

<style scoped>
/* Tukan Design System - CSS Variables */
.tukan-layout {
  --color-bg: #F4F9F4; /* Fundo verde claro suave */
  --color-surface: #FFFFFF;
  --color-primary: #2E7D32; /* Verde Tukan */
  --color-primary-light: #E8F5E9;
  --color-text-main: #2D3748;
  --color-text-muted: #718096;
  --color-border: #E2E8F0;
  
  --radius-lg: 16px;
  --radius-md: 12px;
  --shadow-sm: 0 2px 4px rgba(46, 125, 50, 0.05);
  --shadow-md: 0 4px 12px rgba(46, 125, 50, 0.08);

  min-height: 100vh;
  background-color: var(--color-bg);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  color: var(--color-text-main);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.content-split {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1.5rem;
  align-items: start;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: 1rem;
}

.secondary-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* Responsividade */
@media (max-width: 768px) {
  .content-split {
    grid-template-columns: 1fr;
  }
  .actions-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>