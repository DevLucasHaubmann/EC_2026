<script setup lang="ts">
import { ref } from 'vue';

// Dados simulados de evolução (virão do seu banco de dados via Spring Boot)
const historicoPeso = ref([
  { data: '01/03', peso: 88.0 },
  { data: '15/03', peso: 86.5 },
  { data: '01/04', peso: 85.8 },
  { data: '15/04', peso: 84.2 },
  { data: '03/05', peso: 83.5 },
]);

const pesoInicial = historicoPeso.value[0].peso;
const pesoAtual = historicoPeso.value[historicoPeso.value.length - 1].peso;
const totalPerdido = (pesoInicial - pesoAtual).toFixed(1);

const novoPeso = ref(null);

const registrarPeso = () => {
  if (novoPeso.value) {
    console.log('Registrando novo peso:', novoPeso.value);
    // Aqui você faria o POST para o backend
    novoPeso.value = null;
  }
};
</script>

<template>
  <div class="evolucao-container">
    <header class="header-section">
      <h2>Minha Evolução</h2>
      <p>Acompanhe sua jornada rumo ao objetivo.</p>
    </header>

    <!-- Cards de Resumo -->
    <div class="resumo-grid">
      <div class="resumo-card">
        <span>Peso Inicial</span>
        <strong>{{ pesoInicial }} kg</strong>
      </div>
      <div class="resumo-card highlight">
        <span>Total Eliminado</span>
        <strong>- {{ totalPerdido }} kg</strong>
      </div>
      <div class="resumo-card">
        <span>Peso Atual</span>
        <strong>{{ pesoAtual }} kg</strong>
      </div>
    </div>

    <!-- Seção de Gráfico (Placeholder Visual) -->
    <section class="grafico-section">
      <h3>Progresso de Peso</h3>
      <div class="chart-placeholder">
        <div 
          v-for="(registro, index) in historicoPeso" 
          :key="index" 
          class="bar-container"
        >
          <div 
            class="bar" 
            :style="{ height: (registro.peso * 2) + 'px' }"
          >
            <span class="bar-value">{{ registro.peso }}</span>
          </div>
          <span class="bar-label">{{ registro.data }}</span>
        </div>
      </div>
    </section>

    <!-- Registro de Novo Peso -->
    <section class="registro-section">
      <h3>Atualizar Peso</h3>
      <div class="input-inline">
        <input 
          v-model="novoPeso" 
          type="number" 
          step="0.1" 
          placeholder="Ex: 83.0"
        />
        <button @click="registrarPeso" class="btn-save">Salvar</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.evolucao-container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 1rem;
}

.header-section {
  margin-bottom: 2rem;
}

.resumo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 2.5rem;
}

.resumo-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.resumo-card.highlight {
  background: var(--primary-color, #2D5A27);
  color: white;
}

.resumo-card span {
  display: block;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  opacity: 0.9;
}

.resumo-card strong {
  font-size: 1.5rem;
}

.grafico-section {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.chart-placeholder {
  height: 250px;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  padding-top: 2rem;
  border-bottom: 2px solid #eee;
}

.bar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar {
  width: 30px;
  background: #A8D5BA; /* Verde claro */
  border-radius: 4px 4px 0 0;
  position: relative;
  transition: height 0.3s ease;
}

.bar:last-child {
  background: var(--primary-color, #2D5A27);
}

.bar-value {
  position: absolute;
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.8rem;
  font-weight: bold;
}

.bar-label {
  margin-top: 10px;
  font-size: 0.75rem;
  color: #666;
}

.registro-section {
  background: #f9f9f9;
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px dashed #ccc;
}

.input-inline {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.input-inline input {
  flex: 1;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.btn-save {
  padding: 0 1.5rem;
  background: var(--primary-color, #2D5A27);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
}
</style>