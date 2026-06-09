<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { profileService } from '@/services/modules/profile';
import { assessmentService } from '@/services/modules/assessment';
import { meService } from '@/services/modules/me';
import { HEALTH_CONDITION_OPTIONS, ALLERGY_OPTIONS, NUTRITIONAL_GOAL_OPTIONS, ACTIVITY_LEVEL_OPTIONS, DIET_TYPE_OPTIONS, isRestrictiveDiet } from '@/constants/assessmentOptions';
import { toggleArrayItem } from '@/utils/array';
import { ASSESSMENT_MESSAGES } from '@/constants/messages';
import { VALIDATION_MESSAGES } from '@/constants/messages/validationMessages';
import { useModal } from '@/composables/useModal';
import { useToast } from '@/composables/useToast';
import { useDietGeneration } from '@/composables/useDietGeneration';
import DietGenerationProgress from '@/components/ui/DietGenerationProgress.vue';
import type { CreateProfileRequest } from '@/types/profile';
import type { CreateAssessmentRequest } from '@/types/assessment';
import type { Dashboard, DietType } from '@/types/api';

const router = useRouter();
const modal = useModal();
const toast = useToast();

const {
  status: generationStatus,
  error: generationError,
  currentMessage: generationMessage,
  recommendationId: generationRecommendationId,
  start: startGeneration,
} = useDietGeneration();

// COMPLETED is the only success exit: navigate to the diet, passing the id when known so
// DietaView can fetch it directly (falling back to getLatest if the id is absent/unsupported).
watch(generationStatus, (status) => {
  if (status !== 'COMPLETED') return;
  toast.success(ASSESSMENT_MESSAGES.CREATE_SUCCESS);
  const rid = generationRecommendationId.value;
  router.push({ name: 'dieta', query: rid != null ? { rid: String(rid) } : {} });
});

const MIN_WEIGHT_KG = 20
const MAX_WEIGHT_KG = 500
const MIN_HEIGHT_CM = 50
const MAX_HEIGHT_CM = 300
const MAX_AGE_YEARS = 120

function todayDateString(): string {
  return new Date().toISOString().substring(0, 10)
}

function minBirthDateString(): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() - MAX_AGE_YEARS)
  return d.toISOString().substring(0, 10)
}

const todayDate = todayDateString()
const minBirthDate = minBirthDateString()

// Estado de carregamento inicial (verificação de /me)
const inicializando = ref(true);
const erroInicial = ref<string | null>(null);

// true quando o usuário já tem profile — pula etapas 1-3 de biometria
const temProfile = ref(false);

// Primeira etapa disponível: 1 (fluxo completo) ou 2 (só assessment)
const etapaInicial = ref(1);

const carregando = ref(false);
const etapaAtual = ref(1);


const form = ref({
  dataNascimento: '',
  genero: '',
  peso: null as number | null,
  altura: null as number | null,
  objetivo: '',
  pesoAlvo: null as number | null,
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

const tentouAvancar = ref(false)

const proximaEtapa = () => {
  if (etapaValida.value) {
    tentouAvancar.value = false
    etapaAtual.value++
    return
  }
  if (etapaAtual.value === 2) {
    toast.warning(VALIDATION_MESSAGES.REQUIRED_NUTRITION_GOAL)
    return
  }
  if (etapaAtual.value === 1) tentouAvancar.value = true
};
const etapaAnterior = () => { if (etapaAtual.value > etapaInicial.value) etapaAtual.value--; };
const cancelar = async () => {
  const confirmed = await modal.open({
    title: 'Sair da triagem?',
    message: ASSESSMENT_MESSAGES.EXIT_CONFIRM,
    confirmLabel: 'Sair',
    cancelLabel: 'Continuar',
    variant: 'warning',
  });
  if (confirmed) router.push('/dashboard');
};

function isPastOrToday(dateStr: string): boolean {
  return !!dateStr && dateStr <= todayDateString()
}

function isWithinRange(value: number | null, min: number, max: number): boolean {
  return value !== null && value >= min && value <= max
}

const etapaValida = computed(() => {
  if (etapaAtual.value === 1) {
    return (
      !!form.value.dataNascimento &&
      isPastOrToday(form.value.dataNascimento) &&
      form.value.dataNascimento >= minBirthDate &&
      !!form.value.genero &&
      isWithinRange(form.value.peso, MIN_WEIGHT_KG, MAX_WEIGHT_KG) &&
      isWithinRange(form.value.altura, MIN_HEIGHT_CM, MAX_HEIGHT_CM)
    )
  }
  if (etapaAtual.value === 2) return !!form.value.objetivo
  if (etapaAtual.value === 3) return form.value.qtdRefeicoes >= 3
  return true
})

const dateOfBirthError = computed((): string | null => {
  const d = form.value.dataNascimento
  if (!d) return VALIDATION_MESSAGES.REQUIRED_BIRTHDATE
  if (d > todayDateString()) return VALIDATION_MESSAGES.PAST_BIRTHDATE
  if (d < minBirthDate) return VALIDATION_MESSAGES.BIRTHDATE_TOO_OLD
  return null
})

const genderError = computed((): string | null =>
  !form.value.genero ? VALIDATION_MESSAGES.REQUIRED_GENDER : null
)

const weightError = computed((): string | null =>
  !isWithinRange(form.value.peso, MIN_WEIGHT_KG, MAX_WEIGHT_KG)
    ? VALIDATION_MESSAGES.WEIGHT_RANGE
    : null
)

const heightError = computed((): string | null =>
  !isWithinRange(form.value.altura, MIN_HEIGHT_CM, MAX_HEIGHT_CM)
    ? VALIDATION_MESSAGES.HEIGHT_RANGE
    : null
)

const mostrarCautelaDieta = computed(() =>
  isRestrictiveDiet(form.value.preferenciaAlimentar as DietType)
)

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
    erroInicial.value = ASSESSMENT_MESSAGES.LOAD_ERROR
  } finally {
    inicializando.value = false
  }
})

function buildProfilePayload(): CreateProfileRequest | null {
  const { dataNascimento, genero, peso, altura, nivelAtividade } = form.value
  if (
    !isPastOrToday(dataNascimento) ||
    dataNascimento < minBirthDate ||
    !genero ||
    !isWithinRange(peso, MIN_WEIGHT_KG, MAX_WEIGHT_KG) ||
    !isWithinRange(altura, MIN_HEIGHT_CM, MAX_HEIGHT_CM)
  ) {
    return null
  }
  return {
    dateOfBirth: dataNascimento,
    gender: genero.toUpperCase() as Dashboard.Gender,
    weightKg: peso as number,
    heightCm: altura as number,
    activityLevel: nivelAtividade as Dashboard.ActivityLevel,
  }
}

function buildAssessmentPayload(): CreateAssessmentRequest {
  return {
    goal: form.value.objetivo as Dashboard.NutritionalGoal,
    dietType: form.value.preferenciaAlimentar as DietType,
    healthConditions: form.value.condicoesSaude.join(', '),
    allergies: form.value.alergias.join(', '),
    mealsPerDay: form.value.qtdRefeicoes,
    targetWeightKg: form.value.pesoAlvo ?? undefined,
  }
}

const finalizarTriagem = async () => {
  let profilePayload: CreateProfileRequest | null = null
  if (!temProfile.value) {
    profilePayload = buildProfilePayload()
    if (!profilePayload) return
  }
  carregando.value = true
  try {
    if (profilePayload) await profileService.createOwn(profilePayload)
    await assessmentService.createOwn(buildAssessmentPayload())
  } catch (error) {
    console.error('Triagem submission failed:', error instanceof Error ? error.message : String(error))
    toast.error(ASSESSMENT_MESSAGES.SAVE_ERROR)
    return
  } finally {
    carregando.value = false
  }
  // Profile/assessment are persisted; hand off to the async generation flow (job + SSE).
  await startGeneration()
};

// Retry after a FAILED generation: profile/assessment are already saved, so only the
// generation job is restarted.
const retryGeneration = () => { startGeneration(); };
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
      <p>{{ ASSESSMENT_MESSAGES.LOADING }}</p>
    </div>

    <!-- Erro inicial -->
    <div v-else-if="erroInicial" class="triagem-state">
      <p class="state-error">{{ erroInicial }}</p>
    </div>

    <!-- Geração assíncrona em andamento (job + SSE): status reais, sem percentual falso -->
    <div v-else-if="generationStatus !== null" class="triagem-state">
      <DietGenerationProgress
        :message="generationStatus === 'FAILED' && generationError ? generationError : generationMessage"
        :failed="generationStatus === 'FAILED'"
        @retry="retryGeneration"
      />
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
              <input type="date" v-model="form.dataNascimento" :max="todayDate" :min="minBirthDate" />
              <span v-if="tentouAvancar && dateOfBirthError" class="field-error">{{ dateOfBirthError }}</span>
            </div>
            <div class="field">
              <label>Gênero Biológico</label>
              <select v-model="form.genero">
                <option value="" disabled>Selecione</option>
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Feminino</option>
              </select>
              <span v-if="tentouAvancar && genderError" class="field-error">{{ genderError }}</span>
            </div>
            <div class="field">
              <label>Peso Atual (kg)</label>
              <input type="number" v-model="form.peso" placeholder="70.0" :min="MIN_WEIGHT_KG" :max="MAX_WEIGHT_KG" step="0.1" />
              <span v-if="tentouAvancar && weightError" class="field-error">{{ weightError }}</span>
            </div>
            <div class="field">
              <label>Altura (cm)</label>
              <input type="number" v-model="form.altura" placeholder="175" :min="MIN_HEIGHT_CM" :max="MAX_HEIGHT_CM" step="1" />
              <span v-if="tentouAvancar && heightError" class="field-error">{{ heightError }}</span>
            </div>
          </div>
        </div>

        <!-- ETAPA 2: OBJETIVO (RF007) -->
        <div v-if="etapaAtual === 2" class="animate-in">
          <h2 class="form-title">Qual seu foco atual?</h2>
          <p class="form-subtitle">Escolha o objetivo que melhor descreve o que você quer alcançar.</p>
          <div class="goal-grid">
            <article
              v-for="obj in NUTRITIONAL_GOAL_OPTIONS"
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
          <div class="field mt-large" v-if="form.objetivo">
            <label>Peso alvo (kg) <span class="label-optional">— opcional</span></label>
            <input type="number" step="0.1" v-model="form.pesoAlvo" placeholder="ex: 75" class="large-input" />
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
              <option v-for="opt in ACTIVITY_LEVEL_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
        </div>

        <!-- ETAPA 4: SAÚDE (RF009) -->
        <div v-if="etapaAtual === 4" class="animate-in">
          <h2 class="form-title">Saúde e Restrições</h2>
          <div class="field mb-large">
            <label>Preferência de Dieta</label>
            <div class="segmented-control segmented-diet">
              <button v-for="opt in DIET_TYPE_OPTIONS" :key="opt.value" :class="{ active: form.preferenciaAlimentar === opt.value }" @click="form.preferenciaAlimentar = opt.value">{{ opt.label }}</button>
            </div>
            <p v-if="mostrarCautelaDieta" class="diet-caution">{{ ASSESSMENT_MESSAGES.RESTRICTIVE_DIET_CAUTION }}</p>
          </div>
          
          <div class="field">
            <label>Condições Clínicas</label>
            <div class="chip-container">
              <button v-for="c in HEALTH_CONDITION_OPTIONS" :key="c" class="chip" :class="{ active: form.condicoesSaude.includes(c) }" @click="toggleArrayItem(form.condicoesSaude, c)">{{ c }}</button>
            </div>
          </div>

          <div class="field mt-large">
            <label>Alergias</label>
            <div class="chip-container">
              <button v-for="a in ALLERGY_OPTIONS" :key="a" class="chip" :class="{ active: form.alergias.includes(a) }" @click="toggleArrayItem(form.alergias, a)">{{ a }}</button>
            </div>
          </div>
        </div>

        <!-- FOOTER AÇÕES -->
        <footer class="form-footer">
          <button v-if="etapaAtual < 4" @click="proximaEtapa" class="btn-primary">
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
  --bg-deep: #0f172a; --bg-card: #1e293b; --emerald: #10b981; --text-dim: #94a3b8; --error: #f87171;
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
.goal-grid { display: flex; flex-wrap: wrap; justify-content: center; gap: 1.1rem; width: 100%; max-width: 760px; margin: 0 auto; }
.selectable-card { flex: 0 1 220px; min-width: 200px; max-width: 230px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); padding: 2rem; border-radius: 24px; cursor: pointer; transition: 0.3s; text-align: left; align-items: flex-start; }
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
.segmented-diet button { flex: 0 0 auto; min-width: 116px; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; }
.diet-caution { margin: 12px 4px 0; font-size: 0.8rem; line-height: 1.45; color: #fbbf24; opacity: 0.9; display: flex; gap: 8px; }

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
.label-optional { font-size: 0.8rem; color: var(--text-dim); font-weight: 400; text-transform: none; letter-spacing: 0; }
.field-error { color: var(--error); font-size: 0.82rem; margin-top: 2px; display: block; }

@media (max-width: 650px) { .input-grid { grid-template-columns: 1fr 1fr; } .form-card { padding: 2rem; } .selectable-card { flex: 0 1 calc(50% - 0.6rem); min-width: 0; max-width: none; } }
@media (max-width: 420px) { .selectable-card { flex: 0 1 100%; max-width: none; } }

/* ANIMATION */
.animate-in { animation: fadeInSlide 0.4s ease-out forwards; }
@keyframes fadeInSlide { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>