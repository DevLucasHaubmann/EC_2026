<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardService } from '@/services/modules/dashboard'
import { activityService } from '@/services/modules/activity'
import { aiService } from '@/services/modules/aiRecommendation'
import { evolutionService } from '@/services/modules/evolution'
import type { Dashboard, Activity, Recommendation, Evolution } from '@/types/api'
import { GOAL_LABELS } from '@/constants/labels'
import { COMMON_MESSAGES } from '@/constants/messages'

const router = useRouter()

// ── Estado: dados principais do dashboard ──────────────────────────────────
const loading = ref(true)
const erro = ref<string | null>(null)
const dados = ref<Dashboard.Response | null>(null)

// ── Estado: streak/atividade ────────────────────────────────────────────────
const streakLoading = ref(true)
const streakErro = ref(false)
const streak = ref<Activity.StreakResponse | null>(null)

// ── Estado: dieta atual ─────────────────────────────────────────────────────
const dietaLoading = ref(true)
const dietaErro = ref(false)
const dietaSemPlano = ref(false)
const dieta = ref<Recommendation.Response | null>(null)

// ── Estado: semana ──────────────────────────────────────────────────────────
const semanaLoading = ref(true)
const semanaErro = ref(false)
const semana = ref<Evolution.WeeklySummary | null>(null)

// ── Helpers de data ─────────────────────────────────────────────────────────

/** Retorna a segunda-feira da semana corrente em formato YYYY-MM-DD (data local). */
function getMondayOfCurrentWeek(): string {
  const today = new Date()
  const day = today.getDay() // 0=dom, 1=seg, ..., 6=sab
  const diffToMonday = day === 0 ? -6 : 1 - day
  const monday = new Date(today)
  monday.setDate(today.getDate() + diffToMonday)
  const y = monday.getFullYear()
  const m = String(monday.getMonth() + 1).padStart(2, '0')
  const d = String(monday.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** Retorna a data local de hoje em formato YYYY-MM-DD. */
function getTodayLocalDate(): string {
  const today = new Date()
  const y = today.getFullYear()
  const m = String(today.getMonth() + 1).padStart(2, '0')
  const d = String(today.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// ── Conversão de rota de onboarding ─────────────────────────────────────────
function resolveOnboardingRoute(nextStep: string) {
  if (nextStep === '/profiles/first-access') return { name: 'triagem' }
  if (nextStep === '/assessments') return { name: 'triagem' }
  return null
}

// ── Computed: barra de progresso de IMC ────────────────────────────────────
const bmiProgressWidth = computed(() => {
  const bmi = dados.value?.profile?.bmi
  if (!bmi) return '0%'
  return Math.min(Math.round((bmi / 40) * 100), 100) + '%'
})

// ── Computed: métricas do dia de hoje na semana ────────────────────────────
const hoje = computed((): Evolution.DailyMetrics | null => {
  if (!semana.value) return null
  const todayStr = getTodayLocalDate()
  return semana.value.days.find((d) => d.date === todayStr) ?? null
})

// ── Computed: aderência formatada ──────────────────────────────────────────
const aderenciaFormatada = computed((): string | null => {
  const avg = semana.value?.averageAdherencePercentage
  if (avg === null || avg === undefined) return null
  return Math.round(avg) + '%'
})

// ── onMounted: carrega todos os dados em paralelo ──────────────────────────
onMounted(async () => {
  // 1. Dashboard principal (guarda onboarding)
  try {
    const res = await dashboardService.getDashboard()
    if (res.onboarding.onboardingRequired) {
      const destino = resolveOnboardingRoute(res.onboarding.nextStep)
      if (destino) {
        router.replace(destino)
        return
      }
    }
    dados.value = res
  } catch {
    erro.value = COMMON_MESSAGES.LOAD_ERROR
  } finally {
    loading.value = false
  }

  // Se o dashboard principal falhou, não faz sentido carregar o restante
  if (erro.value) {
    streakLoading.value = false
    dietaLoading.value = false
    semanaLoading.value = false
    return
  }

  // 2. Streak, dieta e semana: degradam individualmente
  const [streakResult, dietaResult, semanaResult] = await Promise.allSettled([
    activityService.recordAccess(),
    aiService.getLatest(),
    evolutionService.getWeekly(getMondayOfCurrentWeek()),
  ])

  // Streak
  if (streakResult.status === 'fulfilled') {
    streak.value = streakResult.value
  } else {
    streakErro.value = true
  }
  streakLoading.value = false

  // Dieta
  if (dietaResult.status === 'fulfilled') {
    dieta.value = dietaResult.value
  } else {
    const err = dietaResult.reason
    if (err?.response?.status === 404) {
      dietaSemPlano.value = true
    } else {
      dietaErro.value = true
    }
  }
  dietaLoading.value = false

  // Semana
  if (semanaResult.status === 'fulfilled') {
    semana.value = semanaResult.value
  } else {
    semanaErro.value = true
  }
  semanaLoading.value = false
})
</script>

<template>
  <main class="dashboard-wrapper">
    <div class="dashboard-container">

      <!-- Estado: carregando dados principais -->
      <div v-if="loading" class="state-center">
        <p class="state-msg">{{ COMMON_MESSAGES.LOADING_DATA }}</p>
      </div>

      <!-- Estado: erro fatal no dashboard principal -->
      <div v-else-if="erro" class="state-center">
        <p class="state-msg state-error">{{ erro }}</p>
        <button class="btn-action btn-outline" style="margin-top: 1rem" @click="router.push('/auth')">
          Voltar ao início
        </button>
      </div>

      <!-- Conteúdo real -->
      <template v-else-if="dados">

        <!-- Cabeçalho -->
        <header class="dashboard-header">
          <div class="header-left">
            <span class="context-tag">Visão Geral</span>
            <h1>Olá, {{ dados.name }}</h1>
            <p class="subtitle">Bem-vindo ao <strong>Tukan</strong>. Vamos manter o foco hoje?</p>
          </div>
        </header>

        <!-- Grade de métricas -->
        <div class="metrics-grid">

          <!-- Card 1: IMC -->
          <section class="metric-card metric-card--wide" aria-label="Índice de Massa Corporal">
            <header class="card-header">
              <h2 class="card-title">Índice de Massa Corporal</h2>
              <p class="card-subtitle">
                Objetivo: {{ dados.assessment ? GOAL_LABELS[dados.assessment.goal] : '—' }}
              </p>
            </header>
            <div class="bmi-numbers">
              <span class="bmi-value">{{ dados.profile?.bmi ?? '—' }}</span>
              <span class="bmi-sep">/</span>
              <span class="bmi-class">{{ dados.profile?.bmiClassification ?? '—' }}</span>
            </div>
            <div class="progress-bar" role="progressbar" :aria-valuenow="dados.profile?.bmi ?? 0" aria-valuemin="0" aria-valuemax="40">
              <div class="progress-fill" :style="{ width: bmiProgressWidth }"></div>
            </div>
          </section>

          <!-- Card 2: Streak -->
          <section class="metric-card" aria-label="Dias ativos">
            <header class="card-header">
              <h2 class="card-title">Sequência de Dias</h2>
              <p class="card-subtitle">Consistência é a chave</p>
            </header>

            <div v-if="streakLoading" class="card-loading">
              <span class="loading-dot"></span>
            </div>

            <div v-else-if="streakErro" class="card-empty">
              <p class="empty-msg">Não foi possível carregar a sequência agora.</p>
            </div>

            <template v-else-if="streak">
              <div class="streak-main">
                <span class="streak-number" :class="{ 'streak-number--zero': streak.currentStreak === 0 }">
                  {{ streak.currentStreak }}
                </span>
                <span class="streak-label">dia{{ streak.currentStreak !== 1 ? 's' : '' }} seguido{{ streak.currentStreak !== 1 ? 's' : '' }}</span>
              </div>
              <p v-if="streak.currentStreak === 0" class="streak-incentive">
                Comece hoje e construa sua sequência!
              </p>
              <div class="streak-stats">
                <div class="streak-stat">
                  <span class="stat-value">{{ streak.longestStreak }}</span>
                  <span class="stat-label">Recorde</span>
                </div>
                <div class="streak-stat">
                  <span class="stat-value">{{ streak.totalActiveDays }}</span>
                  <span class="stat-label">Total de dias</span>
                </div>
              </div>
            </template>
          </section>

          <!-- Card 3: Dieta atual -->
          <section class="metric-card" aria-label="Plano alimentar atual">
            <header class="card-header">
              <h2 class="card-title">Plano Alimentar</h2>
              <p class="card-subtitle">Sua dieta personalizada</p>
            </header>

            <div v-if="dietaLoading" class="card-loading">
              <span class="loading-dot"></span>
            </div>

            <div v-else-if="dietaErro" class="card-empty">
              <p class="empty-msg">Não foi possível carregar o plano agora.</p>
            </div>

            <div v-else-if="dietaSemPlano" class="card-empty">
              <p class="empty-msg">Você ainda não tem um plano alimentar.</p>
              <button class="btn-cta" @click="router.push({ name: 'dieta' })">
                Ver Dieta
              </button>
            </div>

            <template v-else-if="dieta">
              <div class="dieta-status">
                <span class="status-badge status-badge--active">Plano ativo</span>
              </div>
              <div class="dieta-meta">
                <div class="dieta-stat">
                  <span class="stat-value accent">{{ dieta.plan.dailyCalorieTarget }}</span>
                  <span class="stat-label">kcal / dia</span>
                </div>
              </div>
              <p class="dieta-date">
                Gerado em {{ new Date(dieta.createdAt).toLocaleDateString('pt-BR') }}
              </p>
            </template>
          </section>

          <!-- Card 4: Semana -->
          <section class="metric-card" aria-label="Resumo da semana">
            <header class="card-header">
              <h2 class="card-title">Esta Semana</h2>
              <p class="card-subtitle">Aderência e atividade</p>
            </header>

            <div v-if="semanaLoading" class="card-loading">
              <span class="loading-dot"></span>
            </div>

            <div v-else-if="semanaErro" class="card-empty">
              <p class="empty-msg">Não foi possível carregar os dados da semana.</p>
            </div>

            <template v-else-if="semana">
              <div class="semana-stats">
                <div class="semana-stat">
                  <span class="stat-value" :class="{ 'text-muted': !aderenciaFormatada }">
                    {{ aderenciaFormatada ?? '—' }}
                  </span>
                  <span class="stat-label">Aderência média</span>
                </div>
                <div class="semana-stat">
                  <span class="stat-value">{{ semana.activeDays }}<span class="stat-unit">/7</span></span>
                  <span class="stat-label">Dias ativos</span>
                </div>
              </div>
              <template v-if="hoje">
                <div class="hoje-info">
                  <span class="hoje-label">Hoje</span>
                  <span class="hoje-refeicoes">
                    {{ hoje.completedMeals }}/{{ hoje.plannedMeals }} refeições
                  </span>
                </div>
              </template>
              <p v-else-if="!aderenciaFormatada && semana.activeDays === 0" class="empty-msg">
                Sem registros esta semana ainda.
              </p>
            </template>
          </section>

        </div>

      </template>

    </div>
  </main>
</template>

<style scoped>
.dashboard-wrapper {
  --bg-deep: #0f172a;
  --bg-card: #1e293b;
  --bg-card-hover: #243347;
  --accent: #10b981;
  --accent-light: #34d399;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --danger: #f87171;
  --border: rgba(255, 255, 255, 0.05);

  min-height: 100vh;
  background-color: var(--bg-deep);
  color: var(--text-main);
  font-family: 'Inter', sans-serif;
  padding: 2rem 0;
}

.dashboard-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

/* ── Cabeçalho ─────────────────────────────────────────────────────────── */
.dashboard-header {
  margin-bottom: 3rem;
}

.context-tag {
  color: var(--accent);
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 2px;
  display: block;
  margin-bottom: 0.5rem;
}

.header-left h1 {
  font-size: 2.2rem;
  font-weight: 900;
  letter-spacing: -1.5px;
  margin: 0;
}

.subtitle {
  color: var(--text-muted);
  font-size: 1rem;
  margin-top: 0.4rem;
}

/* ── Grade de métricas ─────────────────────────────────────────────────── */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

/* ── Cards ─────────────────────────────────────────────────────────────── */
.metric-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 180px;
}

.metric-card--wide {
  grid-column: 1 / -1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
}

.card-title {
  font-size: 1rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0;
}

.card-subtitle {
  color: var(--text-muted);
  font-size: 0.8rem;
  margin: 0;
  flex-shrink: 0;
}

/* ── Card loading / vazio ──────────────────────────────────────────────── */
.card-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background: var(--text-muted);
  border-radius: 50%;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1); }
}

.card-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  justify-content: center;
}

.empty-msg {
  color: var(--text-muted);
  font-size: 0.85rem;
  margin: 0;
}

.btn-cta {
  align-self: flex-start;
  background: rgba(16, 185, 129, 0.12);
  color: var(--accent);
  border: 1px solid rgba(16, 185, 129, 0.25);
  padding: 0.5rem 1.25rem;
  border-radius: 10px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn-cta:hover {
  background: rgba(16, 185, 129, 0.22);
  border-color: var(--accent);
}

/* ── IMC ───────────────────────────────────────────────────────────────── */
.bmi-numbers {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  font-size: 2rem;
  font-weight: 900;
}

.bmi-sep {
  color: var(--text-muted);
  font-weight: 300;
}

.bmi-class {
  color: var(--text-muted);
  font-size: 1.1rem;
}

.progress-bar {
  height: 10px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  overflow: hidden;
  margin-top: 0.25rem;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent-light));
  border-radius: 20px;
  transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 15px rgba(16, 185, 129, 0.3);
}

/* ── Streak ────────────────────────────────────────────────────────────── */
.streak-main {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.streak-number {
  font-size: 3rem;
  font-weight: 900;
  color: var(--accent);
  line-height: 1;
}

.streak-number--zero {
  color: var(--text-muted);
}

.streak-label {
  font-size: 0.9rem;
  color: var(--text-muted);
  font-weight: 600;
}

.streak-incentive {
  font-size: 0.82rem;
  color: var(--accent-light);
  margin: 0;
}

.streak-stats {
  display: flex;
  gap: 1.5rem;
  margin-top: auto;
}

.streak-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* ── Stats genéricos ───────────────────────────────────────────────────── */
.stat-value {
  font-size: 1.4rem;
  font-weight: 800;
  line-height: 1;
}

.stat-value.accent {
  color: var(--accent);
}

.stat-value.text-muted {
  color: var(--text-muted);
}

.stat-unit {
  font-size: 0.9rem;
  color: var(--text-muted);
  font-weight: 500;
}

.stat-label {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}

/* ── Dieta ─────────────────────────────────────────────────────────────── */
.dieta-status {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.status-badge {
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 3px 10px;
  border-radius: 20px;
}

.status-badge--active {
  background: rgba(16, 185, 129, 0.12);
  color: var(--accent);
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.dieta-meta {
  display: flex;
  gap: 1.5rem;
}

.dieta-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dieta-date {
  font-size: 0.78rem;
  color: var(--text-muted);
  margin: 0;
  margin-top: auto;
}

/* ── Semana ────────────────────────────────────────────────────────────── */
.semana-stats {
  display: flex;
  gap: 2rem;
}

.semana-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hoje-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0.6rem 1rem;
  margin-top: auto;
}

.hoje-label {
  font-size: 0.72rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}

.hoje-refeicoes {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--text-main);
}

/* ── Loading / Erro globais ────────────────────────────────────────────── */
.state-center {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.state-msg {
  color: var(--text-muted);
  font-size: 1rem;
}

.state-error {
  color: var(--danger);
}

.btn-action {
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: white;
}

.btn-outline:hover {
  background: var(--bg-card);
  border-color: var(--accent);
}

/* ── Responsividade ────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .metric-card--wide {
    grid-column: 1;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }
}
</style>
