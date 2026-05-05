<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
// Importando serviços conforme a lógica da triagem
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';

const router = useRouter();
const carregando = ref(false);

// Listas de Seleção baseadas no que fizemos na Triagem
const listaCondicoes = ['Diabetes Tipo 1', 'Diabetes Tipo 2', 'Hipertensão', 'Colesterol Alto', 'Gastrite', 'Anemia', 'Hipotireoidismo'];
const listaAlergias = ['Glúten', 'Lactose', 'Frutos do Mar', 'Amendoim', 'Ovo', 'Soja', 'Proteína do Leite (APLV)'];

// Estado do Formulário espelhando os Requisitos do PDF e a Triagem
const form = ref({
  // Bio (RF006)
  dataNascimento: '2000-01-01', 
  genero: 'MALE',
  peso: 78,
  altura: 182,
  
  // Metas (RF007)
  objetivo: 'ganho',
  pesoAlvo: 85,
  
  // Rotina (RF008)
  nivelAtividade: 'MODERATE',
  qtdRefeicoes: 4,
  
  // Saúde (RF009)
  preferenciaAlimentar: 'ONIVORA',
  condicoesSaude: ['Nenhuma'],
  alergias: ['Lactose']
});

// Funções de Gerenciamento de Tags (Sistema de Chips da Triagem)
const toggleItem = (lista: 'condicoesSaude' | 'alergias', item: string) => {
  const index = form.value[lista].indexOf(item);
  if (index > -1) {
    form.value[lista].splice(index, 1);
  } else {
    form.value[lista].push(item);
  }
};

const salvarAlteracoes = async () => {
  try {
    carregando.value = true;

    // 1. Atualiza Perfil (RF006)
    await profileService.createOwn({
      dateOfBirth: form.value.dataNascimento,
      gender: form.value.genero.toUpperCase(),
      weightKg: form.value.peso,
      heightCm: form.value.altura,
      activityLevel: form.value.nivelAtividade
    });

    // 2. Atualiza Avaliação (RF007, RF008, RF009)
    await assessmentService.createOwn({
      goal: form.value.objetivo === 'perda' ? 'WEIGHT_LOSS' : 'MUSCLE_GAIN',
      dietaryRestrictions: form.value.preferenciaAlimentar,
      healthConditions: form.value.condicoesSaude.join(', '),
      allergies: form.value.alergias.join(', '),
      mealsPerDay: form.value.qtdRefeicoes
    });

    router.push('/perfil');
  } catch (error) {
    console.error('Erro ao atualizar perfil:', error);
  } finally {
    carregando.value = false;
  }
};

const cancelar = () => router.back();
</script>

<template>
  <div class="edit-wrapper">
    <!-- Header de Navegação Superior -->
    <nav class="edit-nav">
      <button @click="cancelar" class="btn-back">
        <span class="back-icon"></span>
        Voltar ao Perfil
      </button>
      <span class="nav-title">Ajustes de Perfil</span>
    </nav>

    <main class="edit-container">
      <header class="edit-intro">
        <h1>Editar Informações</h1>
        <p>Atualize seus dados biométricos e metas para que a IA recalcule seu plano.</p>
      </header>

      <form @submit.prevent="salvarAlteracoes" class="edit-form">
        
        <!-- SEÇÃO 1: BIOMETRIA (RF006) -->
        <section class="form-section">
          <div class="section-header">
            <span class="section-number">01</span>
            <h3>Dados Biométricos</h3>
          </div>
          <div class="input-grid">
            <div class="field">
              <label>Peso Atual (kg)</label>
              <input type="number" v-model="form.peso" required />
            </div>
            <div class="field">
              <label>Altura (cm)</label>
              <input type="number" v-model="form.altura" required />
            </div>
            <div class="field">
              <label>Gênero</label>
              <select v-model="form.genero">
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Feminino</option>
              </select>
            </div>
            <div class="field">
              <label>Data de Nascimento</label>
              <input type="date" v-model="form.dataNascimento" required />
            </div>
          </div>
        </section>

        <!-- SEÇÃO 2: PLANEJAMENTO (RF007 e RF008) -->
        <section class="form-section">
          <div class="section-header">
            <span class="section-number">02</span>
            <h3>Configuração do Plano</h3>
          </div>
          <div class="field">
            <label>Objetivo Principal</label>
            <div class="radio-tabs">
              <button type="button" :class="{ active: form.objetivo === 'perda' }" @click="form.objetivo = 'perda'">Emagrecimento</button>
              <button type="button" :class="{ active: form.objetivo === 'ganho' }" @click="form.objetivo = 'ganho'">Ganho de Massa</button>
            </div>
          </div>
          <div class="input-grid mt-4">
            <div class="field">
              <label>Peso Alvo (kg)</label>
              <input type="number" v-model="form.pesoAlvo" required />
            </div>
            <div class="field">
              <label>Refeições por dia</label>
              <input type="number" v-model="form.qtdRefeicoes" min="3" max="5" />
            </div>
          </div>
        </section>

        <!-- SEÇÃO 3: SAÚDE (RF009 - SISTEMA DE CHIPS) -->
        <section class="form-section">
          <div class="section-header">
            <span class="section-number">03</span>
            <h3>Restrições e Saúde</h3>
          </div>
          
          <div class="field">
            <label>Condições Clínicas</label>
            <div class="chip-container">
              <button 
                v-for="item in listaCondicoes" :key="item" type="button"
                class="chip" :class="{ selected: form.condicoesSaude.includes(item) }"
                @click="toggleItem('condicoesSaude', item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="field mt-4">
            <label>Alergias</label>
            <div class="chip-container">
              <button 
                v-for="item in listaAlergias" :key="item" type="button"
                class="chip" :class="{ selected: form.alergias.includes(item) }"
                @click="toggleItem('alergias', item)"
              >
                {{ item }}
              </button>
            </div>
          </div>
        </section>

        <!-- AÇÕES -->
        <div class="form-actions">
          <button type="submit" class="btn-save" :disabled="carregando">
            {{ carregando ? 'Salvando...' : 'Confirmar Alterações' }}
          </button>
          <button type="button" @click="cancelar" class="btn-cancel">Descartar mudanças</button>
        </div>

      </form>
    </main>
  </div>
</template>

<style scoped>
.edit-wrapper {
  --bg-dark: #0f172a;
  --card-dark: #1e293b;
  --accent: #10b981;
  --text-muted: #94a3b8;

  min-height: 100vh;
  background-color: var(--bg-dark);
  color: white;
  font-family: 'Inter', sans-serif;
}

/* NAV */
.edit-nav {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.5rem 5%; background: rgba(15, 23, 42, 0.8);
  border-bottom: 1px solid rgba(255,255,255,0.05); position: sticky; top: 0; z-index: 10;
}
.btn-back { background: transparent; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 8px; font-weight: 600; }
.btn-back:hover { color: var(--accent); }
.back-icon { width: 8px; height: 8px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
.nav-title { font-weight: 800; text-transform: uppercase; letter-spacing: 1px; font-size: 0.8rem; }

/* CONTAINER */
.edit-container { max-width: 700px; margin: 0 auto; padding: 4rem 1.5rem; }
.edit-intro h1 { font-size: 2.2rem; font-weight: 800; margin-bottom: 0.5rem; }
.edit-intro p { color: var(--text-muted); margin-bottom: 3rem; }

/* FORM SECTIONS */
.form-section { background: var(--card-dark); padding: 2.5rem; border-radius: 24px; margin-bottom: 2rem; border: 1px solid rgba(255,255,255,0.05); }
.section-header { display: flex; align-items: center; gap: 12px; margin-bottom: 2rem; }
.section-number { color: var(--accent); font-weight: 800; font-size: 0.9rem; background: rgba(16, 185, 129, 0.1); width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.section-header h3 { font-size: 1.1rem; font-weight: 700; }

.input-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.field { display: flex; flex-direction: column; gap: 8px; margin-bottom: 1.5rem; }
.field label { font-size: 0.85rem; font-weight: 600; color: var(--text-muted); }

input, select {
  background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(255,255,255,0.1);
  padding: 0.9rem; border-radius: 12px; color: white;
}
input:focus { border-color: var(--accent); outline: none; }

/* RADIO TABS E CHIPS (Estilo Triagem) */
.radio-tabs { display: flex; gap: 10px; }
.radio-tabs button {
  flex: 1; padding: 0.9rem; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1);
  background: transparent; color: white; cursor: pointer; font-weight: 600; transition: 0.3s;
}
.radio-tabs button.active { background: var(--accent); color: var(--bg-dark); border-color: var(--accent); }

.chip-container { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-muted); padding: 7px 14px; border-radius: 20px; font-size: 0.8rem; cursor: pointer;
}
.chip.selected { border-color: var(--accent); color: var(--accent); background: rgba(16, 185, 129, 0.1); }

/* AÇÕES */
.form-actions { display: flex; flex-direction: column; gap: 1rem; margin-top: 3rem; }
.btn-save {
  background: var(--accent); color: var(--bg-dark); border: none; padding: 1.2rem;
  border-radius: 16px; font-weight: 800; font-size: 1.1rem; cursor: pointer; transition: all 0.3s;
}
.btn-save:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2); }
.btn-cancel { background: transparent; border: none; color: var(--text-muted); padding: 0.5rem; cursor: pointer; font-weight: 600; }

@media (max-width: 600px) { .input-grid { grid-template-columns: 1fr; } }
</style>