<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';
import { aiService } from '@/services/modules/aiRecommendation';

const router = useRouter();
const carregando = ref(false);
const etapa = ref(0);

const listaCondicoes = ['Diabetes Tipo 1', 'Diabetes Tipo 2', 'Hipertensão', 'Gastrite', 'Hipotireoidismo'];
const listaAlergias = ['Glúten', 'Lactose', 'Frutos do Mar', 'Amendoim', 'Ovo', 'Soja'];

const form = ref({
  dataNascimento: '',
  genero: '',
  peso: null,
  altura: null,
  objetivo: '',
  pesoAlvo: null,
  nivelAtividade: 'MODERATE',
  qtdRefeicoes: 3,
  preferenciaAlimentar: 'ONIVORA',
  condicoesSaude: [] as string[],
  alergias: [] as string[]
});

// Lógica de seleção corrigida
const toggleItem = (lista: 'condicoesSaude' | 'alergias', item: string) => {
  const index = form.value[lista].indexOf(item);
  if (index > -1) form.value[lista].splice(index, 1);
  else form.value[lista].push(item);
};

const proximo = () => etapa.value++;
const voltar = () => etapa.value--;

const finalizar = async () => {
  try {
    carregando.value = true;
    await profileService.createOwn({
      dateOfBirth: form.value.dataNascimento,
      gender: form.value.genero.toUpperCase(),
      weightKg: form.value.peso,
      heightCm: form.value.altura,
      activityLevel: form.value.nivelAtividade
    });
    await assessmentService.createOwn({
      goal: form.value.objetivo === 'perda' ? 'WEIGHT_LOSS' : 'MUSCLE_GAIN',
      dietaryRestrictions: form.value.preferenciaAlimentar,
      healthConditions: form.value.condicoesSaude.join(', '),
      allergies: form.value.alergias.join(', '),
      mealsPerDay: form.value.qtdRefeicoes
    });
    await aiService.generateNew();
    router.push('/dashboard');
  } catch (e) {
    console.error(e);
  } finally {
    carregando.value = false;
  }
};
</script>

<template>
  <div class="onboarding-wrapper">
    <div class="bg-glow"></div>

    <div class="onboarding-card">
      <transition name="step-fade" mode="out-in">
        
        <!-- ETAPA 0: BOAS-VINDAS -->
        <section v-if="etapa === 0" class="step-container welcome-step" key="step0">
          <div class="brand-icon">🦜</div>
          <div class="text-content">
            <h1>Sua jornada para uma vida saudável começa aqui.</h1>
            <p>O Tukan utiliza inteligência artificial para entender suas necessidades e criar um plano alimentar que se adapta à sua realidade.</p>
          </div>
          <div class="meta-info">
            <span class="time-estimate">Estimativa: 2 min</span>
          </div>
          <button @click="proximo" class="btn-primary">Configurar meu perfil</button>
        </section>

        <!-- ETAPA 1: BIOMETRIA -->
        <section v-else-if="etapa === 1" class="step-container" key="step1">
          <header class="step-header">
            <span class="step-indicator">Passo 01/04</span>
            <h2>Dados Biométricos</h2>
            <p>Essas informações são a base para calcularmos suas necessidades calóricas exatas.</p>
          </header>

          <div class="form-grid">
            <div class="form-group">
              <label>Data de Nascimento</label>
              <input type="date" v-model="form.dataNascimento" />
            </div>
            <div class="form-group">
              <label>Gênero</label>
              <select v-model="form.genero">
                <option value="" disabled selected>Selecione</option>
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Feminino</option>
              </select>
            </div>
            <div class="form-group">
              <label>Peso Atual (kg)</label>
              <input type="number" v-model="form.peso" placeholder="Ex: 75.5" />
            </div>
            <div class="form-group">
              <label>Altura (cm)</label>
              <input type="number" v-model="form.altura" placeholder="Ex: 175" />
            </div>
          </div>
          
          <footer class="step-actions">
            <button @click="voltar" class="btn-secondary">Voltar</button>
            <button @click="proximo" class="btn-primary" :disabled="!form.peso || !form.altura">Continuar</button>
          </footer>
        </section>

        <!-- ETAPA 2: OBJETIVO -->
        <section v-else-if="etapa === 2" class="step-content" key="step2">
          <header class="step-header">
            <span class="step-indicator">Passo 02/04</span>
            <h2>Qual seu objetivo?</h2>
            <p>Personalizamos o balanço de macronutrientes com base na sua meta principal.</p>
          </header>

          <div class="cards-grid">
            <div class="selectable-card" :class="{ active: form.objetivo === 'perda' }" @click="form.objetivo = 'perda'">
              <span class="card-emoji">📉</span>
              <div class="card-info">
                <h3>Emagrecimento</h3>
                <p>Foco em queima de gordura.</p>
              </div>
            </div>
            <div class="selectable-card" :class="{ active: form.objetivo === 'ganho' }" @click="form.objetivo = 'ganho'">
              <span class="card-emoji">💪</span>
              <div class="card-info">
                <h3>Ganho de Massa</h3>
                <p>Foco em hipertrofia.</p>
              </div>
            </div>
          </div>

          <div class="form-group mt-large" v-if="form.objetivo">
            <label>Qual o seu peso alvo? (kg)</label>
            <input type="number" v-model="form.pesoAlvo" placeholder="Ex: 70" class="large-input" />
          </div>

          <footer class="step-actions">
            <button @click="voltar" class="btn-secondary">Voltar</button>
            <button @click="proximo" class="btn-primary" :disabled="!form.objetivo || !form.pesoAlvo">Próximo</button>
          </footer>
        </section>

        <!-- ETAPA 3: ROTINA -->
        <section v-else-if="etapa === 3" class="step-content" key="step3">
          <header class="step-header">
            <span class="step-indicator">Passo 03/04</span>
            <h2>Sua Rotina</h2>
            <p>O melhor plano é aquele que você consegue manter. Como é seu dia a dia?</p>
          </header>

          <div class="form-group mb-large">
            <label>Refeições por dia</label>
            <div class="segment-control">
              <button v-for="n in [3,4,5]" :key="n" :class="{ active: form.qtdRefeicoes === n }" @click="form.qtdRefeicoes = n">
                {{ n }} Refeições
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>Nível de Atividade Física</label>
            <select v-model="form.nivelAtividade">
              <option value="SEDENTARY">Sedentário (Pouco movimento)</option>
              <option value="MODERATE">Moderado (Exercício 3x/semana)</option>
              <option value="INTENSE">Intenso (Atleta ou Treino Diário)</option>
            </select>
          </div>

          <footer class="step-actions">
            <button @click="voltar" class="btn-secondary">Voltar</button>
            <button @click="proximo" class="btn-primary">Continuar</button>
          </footer>
        </section>

        <!-- ETAPA 4: SAÚDE -->
        <section v-else-if="etapa === 4" class="step-content" key="step4">
          <header class="step-header">
            <span class="step-indicator">Passo 04/04</span>
            <h2>Saúde e Restrições</h2>
            <p>Segurança em primeiro lugar. Filtramos ingredientes conforme sua saúde.</p>
          </header>

          <div class="chip-section">
            <label>Condições Clínicas</label>
            <div class="chip-group">
              <button v-for="c in listaCondicoes" :key="c" :class="{ selected: form.condicoesSaude.includes(c) }" @click="toggleItem('condicoesSaude', c)">{{ c }}</button>
            </div>
          </div>

          <div class="chip-section mt-large">
            <label>Alergias Alimentares</label>
            <div class="chip-group">
              <button v-for="a in listaAlergias" :key="a" :class="{ selected: form.alergias.includes(a) }" @click="toggleItem('alergias', a)">{{ a }}</button>
            </div>
          </div>

          <footer class="step-actions">
            <button @click="voltar" class="btn-secondary">Voltar</button>
            <button @click="finalizar" class="btn-finish" :disabled="carregando">
              {{ carregando ? 'Gerando Plano...' : 'Finalizar e Ver Dieta' }}
            </button>
          </footer>
        </section>

      </transition>
    </div>
  </div>
</template>

<style scoped>
/* Cores e Variáveis */
.onboarding-wrapper {
  --bg: #0f172a;
  --card: #1e293b;
  --accent: #10b981;
  --text-dim: #94a3b8;
  --border: rgba(255, 255, 255, 0.08);

  min-height: 100vh;
  background-color: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  font-family: 'Inter', sans-serif;
  color: white;
  position: relative;
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.1) 0%, transparent 70%);
  top: -100px; left: -100px;
  z-index: 1;
}

.onboarding-card {
  width: 100%;
  max-width: 650px;
  background: var(--card);
  border-radius: 32px;
  padding: 3.5rem;
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.4);
  border: 1px solid var(--border);
  z-index: 10;
}

/* Tipografia e Cabeçalhos */
.step-header { margin-bottom: 2.5rem; }
.step-indicator { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
h1, h2 { font-size: 2.2rem; font-weight: 800; letter-spacing: -1px; margin: 0.8rem 0; line-height: 1.2; }
p { color: var(--text-dim); line-height: 1.6; font-size: 1rem; }

/* Grupos de Formulário */
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; margin-top: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 12px; }
.form-group label { font-size: 0.85rem; font-weight: 700; color: #cbd5e1; text-transform: uppercase; letter-spacing: 0.5px; }

input, select {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--border);
  padding: 1rem 1.2rem;
  border-radius: 14px;
  color: white;
  font-size: 1rem;
  transition: all 0.3s ease;
}
input:focus, select:focus { border-color: var(--accent); outline: none; box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1); }

/* Cards de Objetivo */
.cards-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-top: 1rem; }
.selectable-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border);
  padding: 2rem;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.selectable-card:hover { transform: translateY(-5px); border-color: rgba(16, 185, 129, 0.3); }
.selectable-card.active { background: rgba(16, 185, 129, 0.1); border-color: var(--accent); }
.card-emoji { font-size: 2.5rem; margin-bottom: 1rem; }
.card-info h3 { font-size: 1.1rem; font-weight: 700; margin-bottom: 5px; }
.card-info p { font-size: 0.85rem; }

/* Segmented Control (Refeições) */
.segment-control {
  display: flex;
  background: rgba(15, 23, 42, 0.6);
  padding: 6px;
  border-radius: 14px;
  border: 1px solid var(--border);
}
.segment-control button {
  flex: 1;
  background: transparent;
  border: none;
  padding: 0.8rem;
  color: var(--text-dim);
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  border-radius: 10px;
  transition: 0.2s;
}
.segment-control button.active { background: var(--accent); color: var(--bg); box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2); }

/* Chips */
.chip-group { display: flex; flex-wrap: wrap; gap: 10px; }
.chip-group button {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border);
  color: var(--text-dim);
  padding: 10px 18px;
  border-radius: 30px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}
.chip-group button.selected { background: rgba(16, 185, 129, 0.2); border-color: var(--accent); color: var(--accent); }

/* Ações e Botões */
.step-actions { margin-top: 3.5rem; display: flex; justify-content: space-between; align-items: center; }
.btn-primary, .btn-finish {
  background: var(--accent);
  color: var(--bg);
  border: none;
  padding: 1.1rem 2.8rem;
  border-radius: 16px;
  font-weight: 800;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}
.btn-primary:hover { filter: brightness(1.1); transform: translateY(-2px); }
.btn-primary:disabled { opacity: 0.3; cursor: not-allowed; transform: none; }

.btn-secondary { background: transparent; border: none; color: var(--text-dim); font-weight: 700; cursor: pointer; }

/* Intro (Welcome) */
.welcome-step { text-align: center; }
.brand-icon { font-size: 4.5rem; margin-bottom: 1.5rem; }
.meta-info { margin: 2rem 0; }
.time-estimate { background: rgba(255, 255, 255, 0.05); padding: 8px 18px; border-radius: 20px; font-size: 0.8rem; font-weight: 700; color: var(--accent); }

/* Animações de Transição */
.step-fade-enter-active, .step-fade-leave-active { transition: all 0.3s ease; }
.step-fade-enter-from { opacity: 0; transform: translateX(20px); }
.step-fade-leave-to { opacity: 0; transform: translateX(-20px); }

/* Helpers */
.mt-large { margin-top: 2rem; }
.mb-large { margin-bottom: 2rem; }
</style>