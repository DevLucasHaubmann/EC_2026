<script setup lang="ts">
import { ref } from 'vue';

// Dados simulados que serão integrados com o seu backend Spring Boot
const dieta = ref({
  objetivo: 'Ganho de Massa',
  totalKcal: 2450,
  macros: {
    proteina: '180g',
    carbo: '280g',
    gordura: '75g'
  },
  refeicoes: [
    {
      id: 1,
      nome: 'Café da Manhã',
      horario: '07:30',
      itens: ['4 Ovos mexidos', '2 Fatias de pão integral', '1 Fruta (Banana ou Maçã)']
    },
    {
      id: 2,
      nome: 'Almoço',
      horario: '12:00',
      itens: ['150g de Frango grelhado', '200g de Arroz integral', '100g de Feijão', 'Salada à vontade']
    },
    {
      id: 3,
      nome: 'Lanche da Tarde',
      horario: '15:30',
      itens: ['30g de Whey Protein', '40g de Aveia em flocos']
    },
    {
      id: 4,
      nome: 'Jantar',
      horario: '19:30',
      itens: ['150g de Carne moída magra', '150g de Batata doce', 'Legumes cozidos']
    }
  ]
});
</script>

<template>
  <div class="dieta-wrapper">
    <header class="dieta-header">
      <h2>Sua Dieta Personalizada</h2>
      <span class="badge">{{ dieta.objetivo }}</span>
    </header>

    <!-- Resumo de Macronutrientes -->
    <section class="macros-grid">
      <div class="macro-card kcal">
        <strong>{{ dieta.totalKcal }}</strong>
        <span>Kcal Totais</span>
      </div>
      <div class="macro-card">
        <strong>{{ dieta.macros.proteina }}</strong>
        <span>Proteínas</span>
      </div>
      <div class="macro-card">
        <strong>{{ dieta.macros.carbo }}</strong>
        <span>Carbos</span>
      </div>
      <div class="macro-card">
        <strong>{{ dieta.macros.gordura }}</strong>
        <span>Gorduras</span>
      </div>
    </section>

    <!-- Lista de Refeições -->
    <div class="timeline">
      <div v-for="refeicao in dieta.refeicoes" :key="refeicao.id" class="refeicao-item">
        <div class="time-column">
          <span class="time">{{ refeicao.horario }}</span>
          <div class="line"></div>
        </div>
        <div class="content-card">
          <h3>{{ refeicao.nome }}</h3>
          <ul>
            <li v-for="(item, idx) in refeicao.itens" :key="idx">{{ item }}</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dieta-wrapper {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.dieta-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 30px;
}

.badge {
  background: #E8F5E9;
  color: #2D5A27;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
}

.macros-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-bottom: 40px;
}

.macro-card {
  background: #fff;
  padding: 15px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}

.macro-card.kcal {
  background: #2D5A27;
  color: #fff;
}

.macro-card strong {
  font-size: 1.2rem;
  display: block;
}

.macro-card span {
  font-size: 0.75rem;
  opacity: 0.8;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.refeicao-item {
  display: flex;
  gap: 20px;
}

.time-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 60px;
}

.time {
  font-size: 0.85rem;
  font-weight: bold;
  color: #666;
}

.line {
  width: 2px;
  flex-grow: 1;
  background: #E0E0E0;
  margin-top: 10px;
}

.refeicao-item:last-child .line {
  display: none;
}

.content-card {
  background: #fff;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  flex-grow: 1;
}

.content-card h3 {
  margin-bottom: 10px;
  color: #2D5A27;
  font-size: 1.1rem;
}

.content-card ul {
  list-style: none;
  padding: 0;
}

.content-card li {
  padding: 5px 0;
  border-bottom: 1px solid #F5F5F5;
  font-size: 0.95rem;
  color: #444;
}

.content-card li:last-child {
  border-bottom: none;
}
</style>