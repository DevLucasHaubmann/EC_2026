<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';
import { aiService } from '@/services/modules/aiRecommendation';
import { meService } from '@/services/modules/me';

const router = useRouter();

// Estado de carregamento inicial (verificação de /me)
const inicializando = ref(true);
const erroInicial = ref<string | null>(null);

// true quando o usuário já tem profile — pula etapas 1-3 de biometria
const temProfile = ref(false);

// Primeira etapa disponível: 1 (fluxo completo) ou 2 (só assessment)
const etapaInicial = ref(1);

const carregando = ref(false);
const erroSubmissao = ref<string | null>(null);
const etapaAtual = ref(1);

const listaCondicoes = [
  'Diabetes Tipo 1', 'Diabetes Tipo 2', 'Hipertensão', 'Colesterol Alto',
  'Gastrite', 'Anemia', 'Hipotireoidismo', 'Hipertireoidismo', 'Síndrome do Intestino Irritável',
];
const listaAlergias = [
  'Glúten', 'Lactose', 'Frutos do Mar', 'Amendoim', 'Ovo',
  'Soja', 'Proteína do Leite (APLV)', 'Trigo', 'Nozes',
];

const OBJETIVOS = [
  { value: 'WEIGHT_LOSS',         label: 'Emagrecimento',         desc: 'Redução de gordura com déficit calórico.' },
  { value: 'MUSCLE_GAIN',         label: 'Ganho de Massa',         desc: 'Aumento de volume muscular com superávit.' },
  { value: 'MAINTENANCE',         label: 'Manutenção',             desc: 'Manter o peso e a composição atual.' },
  { value: 'DIETARY_REEDUCATION', label: 'Reeducação Alimentar',   desc: 'Melhora dos hábitos alimentares.' },
  { value: 'SPORTS_PERFORMANCE',  label: 'Performance Esportiva',  desc: 'Otimização para rendimento atlético.' },
];

const form = ref({
  dataNascimento: '',
  genero: '',
  peso: null as number | null,
  altura: null as number | null,
  objetivo: '',
  nivelAtividade: 'MODERATE',
  qtdRefeicoes: 3,
  preferenciaAlimentar: 'ONIVORA',
  condicoesSaude: [] as string[],
  alergias: [] as string[],
});

// Quantas etapas o usuário vai ver de fato
const totalEtapas = computed(() => 4 - etapaInicial.value + 1)
// Número relativo da etapa atual para exibição ("Etapa 1 de 3" em vez de "Etapa 2 de 4")
const etapaRelativa = computed(() => etapaAtual.value - etapaInicial.value + 1)

const proximaEtapa = () => { if (etapaValida.value) etapaAtual.value++; };
const etapaAnterior = () => { if (etapaAtual.value > etapaInicial.value) etapaAtual.value--; };
const cancelar = () => { if (confirm('Deseja sair? Seus dados não serão salvos.')) router.push('/dashboard'); };

const toggleItem = (lista: 'condicoesSaude' | 'alergias', item: string) => {
  const index = form.value[lista].indexOf(item);
  if (index > -1) form.value[lista].splice(index, 1);
  else form.value[lista].push(item);
};

const etapaValida = computed(() => {
  if (etapaAtual.value === 1) return form.value.dataNascimento && form.value.genero && form.value.peso && form.value.altura;
  if (etapaAtual.value === 2) return !!form.value.objetivo;
  if (etapaAtual.value === 3) return form.value.qtdRefeicoes >= 3;
  return true;
});

onMounted(async () => {
  try {
    const me = await meService.getMe()

    if (me.profile && me.assessment) {
      // Onboarding completo — não deveria estar aqui, manda para dieta
      router.replace({ name: 'dieta' })
      return
    }

    if (me.profile) {
      // Só falta o assessment: pula as etapas de biometria
      temProfile.value = true
      etapaInicial.value = 2
      etapaAtual.value = 2
    }
  } catch {
    erroInicial.value = 'Não foi possível verificar seu cadastro. Tente novamente.'
  } finally {
    inicializando.value = false
  }
})

const finalizarTriagem = async () => {
  erroSubmissao.value = null
  try {
    carregando.value = true;

    if (!temProfile.value) {
      await profileService.createOwn({
        dateOfBirth: form.value.dataNascimento,
        gender: form.value.genero.toUpperCase(),
        weightKg: form.value.peso,
        heightCm: form.value.altura,
        activityLevel: form.value.nivelAtividade
      });
    }

    await assessmentService.createOwn({
      goal: form.value.objetivo,
      dietaryRestrictions: form.value.preferenciaAlimentar,
      healthConditions: form.value.condicoesSaude.join(', '),
      allergies: form.value.alergias.join(', '),
    });
    await aiService.generateNew();
    router.push({ name: 'dieta' });
  } catch (error) {
    console.error(error);
    erroSubmissao.value = 'Ocorreu um erro ao salvar seus dados. Tente novamente.';
  } finally {
    carregando.value = false;
  }
};
</script>

<template>
  <main class="triagem-layout">
    <!-- NAVBAR PADRONIZADA -->
    <nav class="triagem-nav">
      <div class="nav-left">
        <button v-if="etapaAtual > etapaInicial" @click="etapaAnterior" class="btn-nav">
          <span class="chevron-left"></span> Voltar
        </button>
      </div>
      <div class="nav-center">
        <span class="brand">Tukan <span></span></span>
      </div>
      <div class="nav-right">
        <button @click="cancelar" class="btn-cancel">Cancelar</button>
      </div>
    </nav>

    <!-- Verificando cadastro -->
    <div v-if="inicializando" class="triagem-state">
      <p>Verificando seu cadastro...</p>
    </div>

    <!-- Erro inicial -->
    <div v-else-if="erroInicial" class="triagem-state">
      <p class="state-error">{{ erroInicial }}</p>
    </div>

    <!-- Formulário -->
    <div v-else class="content-container">
      <!-- PROGRESSO -->
      <header class="step-progress">
        <div class="progress-labels">
          <span class="step-tag">Etapa {{ etapaRelativa }} de {{ totalEtapas }}</span>
          <span class="step-name">
            {{ etapaAtual === 1 ? 'Biometria' : etapaAtual === 2 ? 'Objetivos' : etapaAtual === 3 ? 'Rotina' : 'Saúde' }}
          </span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" :style="{ width: (etapaRelativa / totalEtapas * 100) + '%' }"></div>
        </div>
      </header>

      <!-- FORMULÁRIO -->
      <section class="form-card">
        
        <!-- ETAPA 1: BIOMETRIA (RF006) -->
        <div v-if="etapaAtual === 1" class="animate-in">
          <h2 class="form-title">Dados Biométricos</h2>
          <p class="form-subtitle">Essas informações são cruciais para calcular sua taxa metabólica.</p>
          
          <div class="input-grid">
            <div class="field">
              <label>Data de Nascimento</label>
              <input type="date" v-model="form.dataNascimento" />
            </div>
            <div class="field">
              <label>Gênero Biológico</label>
              <select v-model="form.genero">
                <option value="" disabled>Selecione</option>
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Feminino</option>
              </select>
            </div>
            <div class="field">
              <label>Peso Atual (kg)</label>
              <input type="number" v-model="form.peso" placeholder="70.0" />
            </div>
            <div class="field">
              <label>Altura (cm)</label>
              <input type="number" v-model="form.altura" placeholder="175" />
            </div>
          </div>
        </div>

        <!-- ETAPA 2: OBJETIVO (RF007) -->
        <div v-if="etapaAtual === 2" class="animate-in">
          <h2 class="form-title">Qual seu foco atual?</h2>
          <p class="form-subtitle">Escolha o objetivo que melhor descreve o que você quer alcançar.</p>
          <div class="goal-grid">
            <article
              v-for="obj in OBJETIVOS"
              :key="obj.value"
              class="selectable-card"
              :class="{ active: form.objetivo === obj.value }"
              @click="form.objetivo = obj.value"
            >
              <div class="card-indicator"></div>
              <h3>{{ obj.label }}</h3>
              <p>{{ obj.desc }}</p>
            </article>
          </div>
        </div>

        <!-- ETAPA 3: ROTINA (RF008) -->
        <div v-if="etapaAtual === 3" class="animate-in">
          <h2 class="form-title">Sua Rotina</h2>
          <div class="field mb-large">
            <label>Refeições diárias</label>
            <div class="segmented-control">
              <button v-for="n in [3,4,5]" :key="n" :class="{ active: form.qtdRefeicoes === n }" @click="form.qtdRefeicoes = n">
                {{ n }} Refeições
              </button>
            </div>
          </div>
          <div class="field">
            <label>Nível de Atividade Física</label>
            <select v-model="form.nivelAtividade">
              <option value="SEDENTARY">Sedentário (Pouco movimento)</option>
              <option value="MODERATE">Moderado (3-5x na semana)</option>
              <option value="INTENSE">Ativo (Treino Diário)</option>
            </select>
          </div>
        </div>

        <!-- ETAPA 4: SAÚDE (RF009) -->
        <div v-if="etapaAtual === 4" class="animate-in">
          <h2 class="form-title">Saúde e Restrições</h2>
          <div class="field mb-large">
            <label>Preferência de Dieta</label>
            <div class="segmented-control segmented-diet">
              <button :class="{ active: form.preferenciaAlimentar === 'ONIVORA' }"     @click="form.preferenciaAlimentar = 'ONIVORA'">Onívora</button>
              <button :class="{ active: form.preferenciaAlimentar === 'VEGETARIANA' }" @click="form.preferenciaAlimentar = 'VEGETARIANA'">Vegetariana</button>
              <button :class="{ active: form.preferenciaAlimentar === 'VEGANA' }"      @click="form.preferenciaAlimentar = 'VEGANA'">Vegana</button>
              <button :class="{ active: form.preferenciaAlimentar === 'PESCATARIANA' }" @click="form.preferenciaAlimentar = 'PESCATARIANA'">Pescatariana</button>
            </div>
          </div>
          
          <div class="field">
            <label>Condições Clínicas</label>
            <div class="chip-container">
              <button v-for="c in listaCondicoes" :key="c" class="chip" :class="{ active: form.condicoesSaude.includes(c) }" @click="toggleItem('condicoesSaude', c)">{{ c }}</button>
            </div>
          </div>

          <div class="field mt-large">
            <label>Alergias</label>
            <div class="chip-container">
              <button v-for="a in listaAlergias" :key="a" class="chip" :class="{ active: form.alergias.includes(a) }" @click="toggleItem('alergias', a)">{{ a }}</button>
            </div>
          </div>
        </div>

        <!-- FOOTER AÇÕES -->
        <footer class="form-footer">
          <p v-if="erroSubmissao" class="erro-submissao">{{ erroSubmissao }}</p>
          <button v-if="etapaAtual < 4" @click="proximaEtapa" class="btn-primary" :disabled="!etapaValida">
            Próximo Passo
          </button>
          <button v-else @click="finalizarTriagem" class="btn-finish" :disabled="carregando">
            {{ carregando ? 'Gerando Plano...' : 'Finalizar Triagem' }}
          </button>
        </footer>
      </section>
    </div>
  </main>
</template>

<style scoped>
.triagem-layout {
  --bg-deep: #0f172a; --bg-card: #1e293b; --emerald: #10b981; --text-dim: #94a3b8;
  min-height: 100vh; background-color: var(--bg-deep); color: white; font-family: 'Inter', sans-serif;
}

/* NAVBAR */
.triagem-nav {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.5rem 5%; background: rgba(15, 23, 42, 0.9); backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255,255,255,0.05); position: sticky; top: 0; z-index: 100;
}
.btn-nav { background: transparent; border: none; color: var(--text-dim); cursor: pointer; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.btn-nav:hover { color: var(--emerald); }
.chevron-left { width: 8px; height: 8px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
.brand { font-weight: 900; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem; }
.brand span { color: var(--emerald); font-weight: 400; }
.btn-cancel { background: transparent; border: 1px solid rgba(248, 113, 113, 0.2); color: #f87171; padding: 0.5rem 1rem; border-radius: 10px; font-weight: 700; cursor: pointer; font-size: 0.8rem; }
.btn-cancel:hover { background: #ef4444; color: white; }

/* Estado inicial (loading / erro) */
.triagem-state {
  min-height: 60vh; display: flex; align-items: center; justify-content: center;
  color: var(--text-dim); font-size: 1rem;
}
.state-error { color: #f87171; }

/* Erro de submissão no footer */
.erro-submissao {
  color: #f87171; font-size: 0.85rem; margin-bottom: 0.8rem; text-align: right;
}

/* CONTAINER */
.content-container { max-width: 700px; margin: 0 auto; padding: 4rem 1.5rem; }

/* PROGRESS */
.step-progress { margin-bottom: 3.5rem; }
.progress-labels { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1rem; }
.step-tag { color: var(--emerald); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
.step-name { font-weight: 700; font-size: 1.2rem; }
.progress-bar-bg { height: 6px; background: rgba(255,255,255,0.05); border-radius: 10px; }
.progress-bar-fill { height: 100%; background: var(--emerald); border-radius: 10px; transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1); box-shadow: 0 0 15px rgba(16, 185, 129, 0.3); }

/* CARD FORM */
.form-card { background: var(--bg-card); padding: 3.5rem; border-radius: 32px; border: 1px solid rgba(255,255,255,0.05); box-shadow: 0 30px 60px rgba(0,0,0,0.4); }
.form-title { font-size: 2rem; font-weight: 800; letter-spacing: -1px; margin-bottom: 0.5rem; }
.form-subtitle { color: var(--text-dim); margin-bottom: 2.5rem; }

/* INPUTS */
.input-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
.field { display: flex; flex-direction: column; gap: 10px; }
.field label { font-size: 0.85rem; font-weight: 700; color: #cbd5e1; text-transform: uppercase; letter-spacing: 0.5px; }
input, select { background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(255,255,255,0.1); padding: 1rem; border-radius: 14px; color: white; font-size: 1rem; }
input:focus { border-color: var(--emerald); outline: none; }

/* GOAL CARDS */
.goal-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.2rem; }
.selectable-card { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); padding: 2rem; border-radius: 24px; cursor: pointer; transition: 0.3s; }
.selectable-card.active { border-color: var(--emerald); background: rgba(16, 185, 129, 0.1); }
.card-indicator { width: 12px; height: 12px; border: 2px solid var(--text-dim); border-radius: 50%; margin-bottom: 1rem; }
.selectable-card.active .card-indicator { background: var(--emerald); border-color: var(--emerald); box-shadow: 0 0 10px var(--emerald); }
.selectable-card h3 { font-size: 1.1rem; margin-bottom: 8px; }
.selectable-card p { font-size: 0.85rem; color: var(--text-dim); line-height: 1.4; }

/* CONTROLS */
.segmented-control { display: flex; background: rgba(15, 23, 42, 0.6); padding: 5px; border-radius: 14px; }
.segmented-control button { flex: 1; background: transparent; border: none; padding: 0.8rem; color: var(--text-dim); font-weight: 700; cursor: pointer; border-radius: 10px; transition: 0.2s; }
.segmented-control button.active { background: var(--emerald); color: var(--bg-deep); }

.segmented-diet { background: transparent; padding: 0; justify-content: center; flex-wrap: wrap; gap: 8px; }
.segmented-diet button { flex: 0 0 auto; min-width: 130px; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; }

.chip-container { display: flex; flex-wrap: wrap; gap: 8px; }
.chip { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); color: var(--text-dim); padding: 8px 16px; border-radius: 20px; font-size: 0.85rem; cursor: pointer; }
.chip.active { background: rgba(16, 185, 129, 0.2); border-color: var(--emerald); color: var(--emerald); }

/* ACTIONS */
.form-footer { margin-top: 4rem; display: flex; justify-content: flex-end; }
.btn-primary, .btn-finish { background: var(--emerald); color: var(--bg-deep); border: none; padding: 1.2rem 3rem; border-radius: 16px; font-weight: 800; font-size: 1rem; cursor: pointer; transition: 0.3s; }
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2); }
.btn-primary:disabled { opacity: 0.3; cursor: not-allowed; transform: none; }

.mt-large { margin-top: 2rem; }
.mb-large { margin-bottom: 2rem; }

@media (max-width: 650px) { .input-grid, .goal-grid { grid-template-columns: 1fr 1fr; } .form-card { padding: 2rem; } }
@media (max-width: 420px) { .goal-grid { grid-template-columns: 1fr; } }

/* ANIMATION */
.animate-in { animation: fadeInSlide 0.4s ease-out forwards; }
@keyframes fadeInSlide { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>