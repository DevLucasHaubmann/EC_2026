<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

// Simulando dados que viriam do perfil atual
const form = ref({
  nome: 'Adrian Antônio',
  objetivo: 'ganho', // 'perda', 'ganho', 'manter'
  nivelAtividade: 'moderado'
});

const salvarAlteracoes = () => {
  console.log('Dados atualizados:', form.value);
  // Aqui você faria o PUT para o seu backend Spring Boot
  router.push('/perfil');
};

const cancelar = () => {
  router.back();
};
</script>

<template>
  <div class="edit-profile-container">
    <header class="edit-header">
      <button @click="cancelar" class="btn-back">← Voltar</button>
      <h2>Editar Perfil</h2>
    </header>

    <form @submit.prevent="salvarAlteracoes" class="edit-form">
      <!-- Seção: Dados Pessoais -->
      <section class="form-section">
        <h3>Dados Pessoais</h3>
        <div class="input-group">
          <label>Nome Completo</label>
          <input v-model="form.nome" type="text" placeholder="Seu nome" required />
        </div>
      </section>

      <!-- Seção: Plano Alimentar -->
      <section class="form-section">
        <h3>Configuração do Plano</h3>
        <p class="section-desc">Mude seu objetivo para que a IA gere novas sugestões.</p>
        
        <div class="input-group">
          <label>Objetivo Principal</label>
          <select v-model="form.objetivo">
            <option value="perda">Perda de Peso</option>
            <option value="ganho">Ganho de Massa</option>
            <option value="manter">Manter Saúde / Longevidade</option>
          </select>
        </div>

        <div class="input-group">
          <label>Nível de Atividade Física</label>
          <select v-model="form.nivelAtividade">
            <option value="sedentario">Sedentário</option>
            <option value="leve">Leve (1-2x na semana)</option>
            <option value="moderado">Moderado (3-5x na semana)</option>
            <option value="intenso">Intenso (Todos os dias)</option>
          </select>
        </div>
      </section>

      <div class="form-actions">
        <button type="submit" class="btn-save">Salvar Alterações</button>
        <button type="button" @click="cancelar" class="btn-cancel">Descartar</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.edit-profile-container {
  max-width: 600px;
  margin: 2rem auto;
  padding: 0 1rem;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 2rem;
}

.btn-back {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 1rem;
}

.edit-form {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
}

.form-section {
  margin-bottom: 2rem;
}

.form-section h3 {
  font-size: 1.1rem;
  color: #333;
  margin-bottom: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #eee;
}

.section-desc {
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 1rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.input-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #555;
}

.input-group input, 
.input-group select {
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 2rem;
}

.btn-save {
  background: var(--primary-color, #2D5A27);
  color: white;
  border: none;
  padding: 1rem;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: filter 0.2s;
}

.btn-save:hover {
  filter: brightness(1.2);
}

.btn-cancel {
  background: transparent;
  border: none;
  color: #999;
  padding: 0.5rem;
  cursor: pointer;
}
</style>