<script setup lang="ts">
import { ref, onMounted } from 'vue';
// Importando o serviço de IA da sua branch
import { aiService } from '@/services/modules/aiRecommendation';

const dieta = ref(null);
const carregando = ref(true);

onMounted(async () => {
  try {
    // Busca a dieta mais recente vinda do Spring Boot
    const resposta = await aiService.getLatest();

    // Adaptando o retorno do backend para o formato que a sua tela espera
    dieta.value = {
      objetivo: resposta.goal === 'MUSCLE_GAIN' ? 'Ganho de Massa' : 'Redução de Gordura',
      totalKcal: resposta.totalCalories || 0,
      macros: {
        proteina: `${resposta.proteinGrams || 0}g`,
        carbo: `${resposta.carbsGrams || 0}g`,
        gordura: `${resposta.fatGrams || 0}g`
      },
      // Fazendo um 'map' para listar as refeições geradas pela IA
      refeicoes: resposta.meals.map((meal: any, index: number) => ({
        id: index + 1,
        nome: meal.name,
        horario: meal.time || 'A definir',
        itens: meal.items || meal.foods.map((f: any) => f.name)
      }))
    };
  } catch (error) {
    console.error("Erro ao buscar a dieta:", error);
  } finally {
    carregando.value = false;
  }
});
</script>

<template>
  <div class="dieta-wrapper">
    <div v-if="carregando" class="loading-state">
      <div class="spinner"></div>
      <p>Buscando sua dieta personalizada na base de dados...</p>
    </div>

    <div v-else-if="dieta" class="dieta-container">
      <header class="dieta-header">
        <h2>Sua Dieta Personalizada</h2>
        <span class="badge">{{ dieta.objetivo }}</span>
      </header>

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

    <div v-else class="error-state">
      <p>Nenhuma dieta encontrada. Por favor, realize a triagem primeiro.</p>
      <router-link to="/triagem" class="btn-primary">Ir para Triagem</router-link>
    </div>
  </div>
</template>

<style scoped>
/* Estilos mantidos do original com ajustes para o loading */
.dieta-wrapper {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading-state {
  text-align: center;
  padding: 100px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #2D5A27;
  border-radius: 50%;
  margin: 0 auto 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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

.error-state {
  text-align: center;
  padding: 50px;
}

.btn-primary {
  display: inline-block;
  margin-top: 20px;
  padding: 10px 20px;
  background: #2D5A27;
  color: white;
  text-decoration: none;
  border-radius: 8px;
}
</style>