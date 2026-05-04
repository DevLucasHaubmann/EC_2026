<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const form = ref({
  peso: null,
  altura: null,
  idade: null,
  genero: '',
  objetivo: '',
  nivelAtividade: ''
});

const finalizarTriagem = () => {
  console.log('Dados da triagem:', form.value);
  router.push('/dieta');
};
</script>

<template>
  <div class="triagem-wrapper">
    <div class="triagem-container">
      <header class="step-header">
        <span class="step-indicator">Configuração Inicial</span>
        <h1>Perfil Biométrico</h1>
        <p>Informe seus dados básicos para que nossa inteligência possa calcular suas metas nutricionais.</p>
      </header>

      <form @submit.prevent="finalizarTriagem" class="triagem-form">
        <div class="form-grid">
          <!-- Peso -->
          <div class="input-group">
            <label>Peso Corporal</label>
            <div class="custom-input">
              <input v-model="form.peso" type="number" step="0.1" placeholder="00.0" required />
              <span class="suffix">kg</span>
            </div>
          </div>
          
          <!-- Altura -->
          <div class="input-group">
            <label>Estatura</label>
            <div class="custom-input">
              <input v-model="form.altura" type="number" step="0.01" placeholder="0.00" required />
              <span class="suffix">m</span>
            </div>
          </div>

          <!-- Idade -->
          <div class="input-group">
            <label>Idade Atual</label>
            <div class="custom-input">
              <input v-model="form.idade" type="number" placeholder="--" required />
              <span class="suffix">anos</span>
            </div>
          </div>

          <!-- Gênero -->
          <div class="input-group">
            <label>Gênero Biológico</label>
            <div class="custom-input">
              <select v-model="form.genero" required>
                <option value="" disabled selected>Selecionar</option>
                <option value="masculino">Masculino</option>
                <option value="feminino">Feminino</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Seleção de Objetivo -->
        <div class="objective-section">
          <label class="section-label">Objetivo Principal</label>
          <div class="objective-options">
            <label class="option-item" :class="{ active: form.objetivo === 'perda' }">
              <input type="radio" v-model="form.objetivo" value="perda" />
              <div class="option-content">
                <span class="title">Redução de Gordura</span>
                <span class="desc">Foco em déficit calórico controlado</span>
              </div>
              <div class="check-mark"></div>
            </label>
            
            <label class="option-item" :class="{ active: form.objetivo === 'ganho' }">
              <input type="radio" v-model="form.objetivo" value="ganho" />
              <div class="option-content">
                <span class="title">Hipertrofia</span>
                <span class="desc">Superávit focado em construção muscular</span>
              </div>
              <div class="check-mark"></div>
            </label>
            
           
          </div>
        </div>

        <button type="submit" class="btn-submit">
          Gerar Planejamento Nutricional
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.triagem-wrapper {
  --primary: #2D5A27;
  --primary-hover: #24491f;
  --bg-subtle: #f9fafb;
  --border-color: #e5e7eb;
  --text-main: #111827;
  --text-secondary: #4b5563;
  --text-light: #9ca3af;

  min-height: 100vh;
  background: var(--bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  font-family: 'Inter', sans-serif;
}

.triagem-container {
  width: 100%;
  max-width: 640px;
  background: #ffffff;
  padding: 3rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1), 0 10px 15px -3px rgba(0,0,0,0.05);
}

.step-header {
  margin-bottom: 2.5rem;
}

.step-indicator {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
  display: block;
}

.step-header h1 {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-main);
  letter-spacing: -0.025em;
  margin-bottom: 0.75rem;
}

.step-header p {
  color: var(--text-secondary);
  font-size: 0.95rem;
  line-height: 1.5;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.input-group label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 0.5rem;
}

.custom-input {
  position: relative;
  display: flex;
  align-items: center;
}

.custom-input input, 
.custom-input select {
  width: 100%;
  padding: 0.75rem 1rem;
  background: #fff;
  border: 1.5px solid var(--border-color);
  border-radius: 8px;
  font-size: 1rem;
  color: var(--text-main);
  transition: all 0.2s ease;
}

.custom-input input:focus, 
.custom-input select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(45, 90, 39, 0.1);
}

.suffix {
  position: absolute;
  right: 1rem;
  font-size: 0.875rem;
  color: var(--text-light);
  pointer-events: none;
}

/* Objetivo */
.objective-section {
  margin-top: 2rem;
}

.section-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 1rem;
}

.objective-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem;
  border: 1.5px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: var(--text-light);
}

.option-item.active {
  border-color: var(--primary);
  background: rgba(45, 90, 39, 0.02);
}

.option-item input {
  display: none;
}

.option-content {
  display: flex;
  flex-direction: column;
}

.option-content .title {
  font-weight: 700;
  font-size: 1rem;
  color: var(--text-main);
}

.option-content .desc {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.check-mark {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-color);
  border-radius: 50%;
  position: relative;
}

.active .check-mark {
  background: var(--primary);
  border-color: var(--primary);
}

.active .check-mark::after {
  content: "";
  position: absolute;
  width: 6px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  top: 3px;
  left: 6px;
  transform: rotate(45deg);
}

.btn-submit {
  width: 100%;
  margin-top: 2.5rem;
  background: var(--primary);
  color: #fff;
  padding: 1rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-submit:hover {
  background: var(--primary-hover);
}

@media (max-width: 600px) {
  .triagem-container {
    padding: 2rem 1.5rem;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>