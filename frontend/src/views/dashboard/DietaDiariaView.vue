<script setup lang="ts">
import { ref } from 'vue';

// Dados simulados da dieta gerada pela IA
const refeicoes = ref([
  { id: 1, nome: 'Café da Manhã', horario: '08:00', descricao: 'Omelete com 3 ovos e aveia', consumido: true },
  { id: 2, nome: 'Almoço', horario: '12:30', descricao: 'Arroz integral, feijão e peito de frango', consumido: false },
  { id: 3, nome: 'Lanche da Tarde', horario: '16:00', descricao: 'Iogurte natural com 1 banana', consumido: false },
  { id: 4, nome: 'Jantar', horario: '20:00', descricao: 'Salada completa com atum', consumido: false },
]);

const alternarConsumo = (id: number) => {
  const refeicao = refeicoes.value.find(r => r.id === id);
  if (refeicao) {
    refeicao.consumido = !refeicao.consumido;
  }
};
</script>

<template>
  <div class="dieta-wrapper">
    <header class="dieta-header">
      <span class="date-label">Hoje, 03 de Maio</span>
      <h1>Plano Alimentar</h1>
      <div class="daily-progress">
        <div class="progress-info">
          <span>Progresso Diário</span>
          <span>{{ refeicoes.filter(r => r.consumido).length }}/{{ refeicoes.length }}</span>
        </div>
        <div class="progress-bar">
          <div 
            class="progress-fill" 
            :style="{ width: (refeicoes.filter(r => r.consumido).length / refeicoes.length * 100) + '%' }"
          ></div>
        </div>
      </div>
    </header>

    <div class="timeline">
      <div 
        v-for="item in refeicoes" 
        :key="item.id" 
        class="meal-card"
        :class="{ 'is-done': item.consumido }"
      >
        <div class="meal-time">{{ item.horario }}</div>
        
        <div class="meal-content">
          <div class="meal-info">
            <h3>{{ item.nome }}</h3>
            <p>{{ item.descricao }}</p>
          </div>
          
          <button 
            @click="alternarConsumo(item.id)" 
            class="check-btn"
            :aria-label="item.consumido ? 'Desmarcar' : 'Marcar como feito'"
          >
            <div class="icon-circle">
              <svg v-if="item.consumido" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M20 6L9 17L4 12" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dieta-wrapper {
  --primary: #2D5A27;
  --bg: #f9fafb;
  --border: #e5e7eb;
  --text-main: #111827;
  --text-muted: #6b7280;

  max-width: 600px;
  margin: 0 auto;
  padding: 2rem 1rem;
  font-family: 'Inter', sans-serif;
}

.dieta-header {
  margin-bottom: 2.5rem;
}

.date-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.dieta-header h1 {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0.5rem 0 1.5rem;
}

/* Barra de Progresso */
.daily-progress {
  background: white;
  padding: 1rem;
  border-radius: 12px;
  border: 1px solid var(--border);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.progress-bar {
  height: 8px;
  background: #f3f4f6;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary);
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Timeline e Cards */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.meal-card {
  display: flex;
  gap: 1rem;
  transition: all 0.3s ease;
}

.meal-time {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-muted);
  width: 50px;
  padding-top: 1.25rem;
}

.meal-content {
  flex: 1;
  background: white;
  padding: 1.25rem;
  border-radius: 16px;
  border: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s ease;
}

.meal-info h3 {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.meal-info p {
  font-size: 0.875rem;
  color: var(--text-secondary, #4b5563);
  line-height: 1.4;
}

/* Botão Check */
.check-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.icon-circle {
  width: 32px;
  height: 32px;
  border: 2px solid var(--border);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.2s ease;
}

.icon-circle svg {
  width: 18px;
  height: 18px;
}

/* Estados Ativos */
.meal-card.is-done .meal-content {
  border-color: var(--primary);
  background: #f0f7f1;
}

.meal-card.is-done .icon-circle {
  background: var(--primary);
  border-color: var(--primary);
}

.meal-card.is-done .meal-info h3 {
  color: var(--primary);
  text-decoration: line-through;
  opacity: 0.7;
}

.meal-card.is-done .meal-info p {
  opacity: 0.6;
}

@media (max-width: 480px) {
  .meal-time {
    display: none; /* Em telas muito pequenas, focamos no conteúdo */
  }
}
</style>