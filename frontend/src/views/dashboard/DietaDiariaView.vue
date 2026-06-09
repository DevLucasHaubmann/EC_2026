<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { aiService } from '@/services/modules/aiRecommendation'
import { mealLogService } from '@/services/modules/mealLog'
import { useToast } from '@/composables/useToast'
import { MEAL_LABELS } from '@/constants/labels'
import type { MealType, Recommendation, MealLog } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

// ── Types ─────────────────────────────────────────────────────────────────────

interface MealCard {
  mealType: string
  optionNumber: number
  calories: number
  protein: number
  carbs: number
  items: Recommendation.MealPlanFoodItem[]
}

// ── State ─────────────────────────────────────────────────────────────────────

const loading = ref(false)
const error = ref<string | null>(null)
const logsError = ref<string | null>(null)
const recommendation = ref<Recommendation.Response | null>(null)
const logs = ref<MealLog.Response[]>([])
// Tracks mealTypes with an in-flight POST to prevent double-tap races.
const loggingInProgress = reactive(new Set<string>())

// ── Helpers ───────────────────────────────────────────────────────────────────

function getLocalDate(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

function mealLabel(type: string): string {
  return MEAL_LABELS[type as MealType] ?? type
}

// createdAt is expected to be a UTC ISO-8601 string from the backend.
function formatTime(createdAt: string): string {
  const d = new Date(createdAt)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function isConflict(err: unknown): boolean {
  return (err as { response?: { status?: number } })?.response?.status === 409
}

// ── Computed ──────────────────────────────────────────────────────────────────

const mealCards = computed<MealCard[]>(() => {
  if (!recommendation.value) return []
  return recommendation.value.plan.meals.flatMap(meal =>
    meal.options.map(opt => ({
      mealType: meal.mealType,
      optionNumber: opt.optionNumber,
      calories: opt.totalCalories,
      protein: opt.totalProtein,
      carbs: opt.totalCarbs,
      items: opt.items,
    }))
  )
})

const mealTypesWithMultipleOptions = computed<Set<string>>(() => {
  if (!recommendation.value) return new Set()
  return new Set(
    recommendation.value.plan.meals
      .filter(m => m.options.length > 1)
      .map(m => m.mealType)
  )
})

// Assumes logs is always scoped to today (enforced server-side by getByDate).
const loggedMealTypes = computed<Set<string>>(
  () => new Set(logs.value.map(l => l.mealType))
)

const totalKcal = computed(() =>
  Math.round(logs.value.reduce((sum, l) => sum + l.calories, 0))
)

// ── Data fetching ─────────────────────────────────────────────────────────────

async function fetchData(): Promise<void> {
  loading.value = true
  error.value = null
  logsError.value = null
  try {
    const today = getLocalDate()
    const [recResult, logsResult] = await Promise.allSettled([
      aiService.getLatest(),
      mealLogService.getByDate(today),
    ])

    if (recResult.status === 'fulfilled') {
      recommendation.value = recResult.value
    } else {
      const status = (recResult.reason as { response?: { status?: number } })?.response?.status
      if (status !== 404) {
        error.value = 'Erro ao carregar a dieta ativa. Tente novamente.'
      }
    }

    if (logsResult.status === 'fulfilled') {
      logs.value = logsResult.value
    } else {
      logsError.value = 'Não foi possível carregar as refeições efetivadas de hoje.'
    }
  } finally {
    loading.value = false
  }
}

// ── Actions ───────────────────────────────────────────────────────────────────

async function efetivarSugestao(card: MealCard): Promise<void> {
  if (!recommendation.value) return
  if (loggedMealTypes.value.has(card.mealType)) {
    toast.warning(`${mealLabel(card.mealType)} já registrado hoje.`)
    return
  }
  if (loggingInProgress.has(card.mealType)) return
  loggingInProgress.add(card.mealType)
  try {
    const newLog = await mealLogService.log({
      recommendationId: recommendation.value.id,
      mealDate: getLocalDate(),
      mealType: card.mealType,
      optionNumber: card.optionNumber,
    })
    logs.value.push(newLog)
    toast.success(`${mealLabel(card.mealType)} registrado!`)
  } catch (err) {
    if (isConflict(err)) {
      toast.warning(`${mealLabel(card.mealType)} já registrado hoje.`)
      try {
        logsError.value = null
        logs.value = await mealLogService.getByDate(getLocalDate())
      } catch {
        toast.warning('Não foi possível atualizar a lista de refeições.')
      }
    } else {
      toast.error('Erro ao registrar refeição. Tente novamente.')
    }
  } finally {
    loggingInProgress.delete(card.mealType)
  }
}

async function removerRefeicao(id: number): Promise<void> {
  try {
    await mealLogService.remove(id)
    logs.value = logs.value.filter(l => l.id !== id)
  } catch {
    toast.error('Erro ao remover refeição. Tente novamente.')
  }
}

onMounted(fetchData)

const voltar = () => router.back()
const logout = () => authStore.logout()
</script>

<template>
  <div class="registro-wrapper">
    <!-- NAVBAR PADRONIZADA -->
    <nav class="top-nav-standard">
      <button class="btn-nav-back" @click="voltar">
        <span class="chevron-left"></span>
        Voltar
      </button>
      <span class="nav-brand-text">Tukan <span></span></span>
      <button class="btn-nav-logout" @click="logout">Sair</button>
    </nav>

    <main class="content-container">
      <!-- Cabeçalho da Página -->
      <header class="page-intro">
        <span class="intro-tag">Log de Consumo</span>
        <h1>Registro de Refeições</h1>
        <p>Confirme o que você consumiu hoje para atualizar seu saldo calórico.</p>
      </header>

      <!-- Card de Saldo -->
      <section class="balance-highlight-card">
        <div class="balance-content">
          <label>Saldo Calórico do Dia</label>
          <h2 class="total-display">{{ (loading || logsError) ? '--' : totalKcal }} <span>kcal</span></h2>
          <p class="balance-status">{{ logsError ? 'Erro ao carregar consumo do dia.' : 'Consumo registrado até o momento.' }}</p>
        </div>
        <div class="decoration-circle"></div>
      </section>

      <!-- Estado: carregando -->
      <div v-if="loading" class="state-feedback">
        <p>Carregando seu plano do dia...</p>
      </div>

      <!-- Estado: erro crítico (recomendação não pôde ser carregada) -->
      <div v-else-if="error" class="state-feedback state-feedback--error">
        <p>{{ error }}</p>
        <button class="retry-btn" @click="fetchData">Tentar novamente</button>
      </div>

      <!-- Estado: carregado -->
      <div v-else class="registro-grid">

        <!-- Coluna: Sugestões do Plano -->
        <section class="column-section">
          <div class="column-header">
            <h3>Sugestões do Plano</h3>
            <p>Rápida efetivação</p>
          </div>

          <!-- Sem dieta ativa -->
          <div v-if="!recommendation" class="state-feedback state-feedback--empty">
            <p>Nenhuma dieta ativa encontrada.</p>
            <p class="state-feedback__hint">Gere uma dieta personalizada para começar a registrar refeições.</p>
          </div>

          <!-- Cards de opções -->
          <div v-else class="sugestoes-stack">
            <article
              v-for="card in mealCards"
              :key="`${card.mealType}-${card.optionNumber}`"
              class="sugestao-card"
            >
              <div class="sugestao-info">
                <strong>
                  {{ mealLabel(card.mealType) }}
                  <span v-if="mealTypesWithMultipleOptions.has(card.mealType)" class="option-tag">
                    Op. {{ card.optionNumber }}
                  </span>
                </strong>
                <div class="macros-badges">
                  <span>{{ Math.round(card.calories) }} kcal</span>
                  <span>{{ Math.round(card.protein) }}g P</span>
                  <span>{{ Math.round(card.carbs) }}g C</span>
                </div>
                <ul v-if="card.items.length" class="items-list">
                  <li v-for="item in card.items" :key="item.foodId" class="item-entry">
                    {{ item.displayName || item.name }}
                    <span class="item-portion">{{ item.portionGrams }}g</span>
                  </li>
                </ul>
              </div>
              <button
                v-if="loggedMealTypes.has(card.mealType)"
                class="btn-logged"
                disabled
              >✓ Registrado</button>
              <button
                v-else
                @click="efetivarSugestao(card)"
                :disabled="loggingInProgress.has(card.mealType)"
                class="btn-add-plus"
              >Adicionar</button>
            </article>
          </div>
        </section>

        <!-- Coluna: Consumido Hoje -->
        <section class="column-section">
          <div class="column-header">
            <h3>Consumido Hoje</h3>
            <p>Linha do tempo</p>
          </div>

          <div class="history-timeline">
            <div v-if="logsError" class="state-feedback state-feedback--error">
              <p>{{ logsError }}</p>
              <button class="retry-btn" @click="fetchData">Tentar novamente</button>
            </div>
            <template v-else>
              <article v-for="log in logs" :key="log.id" class="log-entry">
                <div class="log-time-box">{{ formatTime(log.createdAt) }}</div>
                <div class="log-main-card">
                  <div class="log-text">
                    <strong>{{ mealLabel(log.mealType) }}</strong>
                    <p>Opção {{ log.optionNumber }} — {{ Math.round(log.calories) }} kcal</p>
                  </div>
                  <button @click="removerRefeicao(log.id)" class="btn-remove-log">
                    Remover
                  </button>
                </div>
              </article>

              <div v-if="logs.length === 0" class="empty-log-msg">
                Nenhum registro encontrado para hoje.
              </div>
            </template>
          </div>
        </section>

      </div>
    </main>
  </div>
</template>

<style scoped>
.registro-wrapper {
  --bg-deep: #0f172a;
  --bg-card: #1e293b;
  --accent: #10b981;
  --text-muted: #94a3b8;

  min-height: 100vh;
  background-color: var(--bg-deep);
  color: white;
  font-family: 'Inter', sans-serif;
}

/* NAVBAR PADRONIZADA */
.top-nav-standard {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.2rem 5%; background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(12px); border-bottom: 1px solid rgba(255,255,255,0.05);
  position: sticky; top: 0; z-index: 100;
}

.btn-nav-back {
  background: transparent; border: none; color: var(--text-muted);
  display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 600;
}
.btn-nav-back:hover { color: var(--accent); }
.chevron-left { width: 8px; height: 8px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }

.nav-brand-text { font-weight: 800; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem; }
.nav-brand-text span { color: var(--accent); font-weight: 400; }

.btn-nav-logout { background: rgba(239, 68, 68, 0.1); color: #f87171; border: none; padding: 0.5rem 1.2rem; border-radius: 10px; font-weight: 700; cursor: pointer; }

/* LAYOUT CONTEÚDO */
.content-container { max-width: 1100px; margin: 0 auto; padding: 4rem 5%; }

.page-intro { margin-bottom: 3.5rem; }
.intro-tag { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
.page-intro h1 { font-size: 2.5rem; font-weight: 900; margin: 0.5rem 0; letter-spacing: -1px; }
.page-intro p { color: var(--text-muted); font-size: 1.1rem; }

/* CARD DE DESTAQUE */
.balance-highlight-card {
  background: var(--accent); color: var(--bg-deep);
  padding: 3rem; border-radius: 30px; margin-bottom: 4rem;
  display: flex; justify-content: space-between; align-items: center;
  position: relative; overflow: hidden;
  box-shadow: 0 20px 40px rgba(16, 185, 129, 0.2);
}

.balance-content label { font-weight: 800; text-transform: uppercase; font-size: 0.8rem; opacity: 0.7; }
.total-display { font-size: 4rem; font-weight: 900; margin: 0.5rem 0; }
.total-display span { font-size: 1.2rem; font-weight: 500; }
.balance-status { font-weight: 700; }

.decoration-circle { position: absolute; right: -50px; width: 200px; height: 200px; background: rgba(255,255,255,0.1); border-radius: 50%; }

/* ESTADOS DE FEEDBACK */
.state-feedback {
  background: var(--bg-card);
  border-radius: 24px;
  padding: 3rem 2rem;
  text-align: center;
  color: var(--text-muted);
  margin-bottom: 2rem;
}
.state-feedback--error { border: 1px solid rgba(239, 68, 68, 0.3); color: #f87171; }
.state-feedback--empty { border: 1px solid rgba(255,255,255,0.05); }
.state-feedback__hint { font-size: 0.9rem; margin-top: 0.5rem; }
.retry-btn {
  margin-top: 1rem;
  background: transparent;
  border: 1px solid #f87171;
  color: #f87171;
  padding: 0.5rem 1.25rem;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 700;
  font-size: 0.85rem;
}
.retry-btn:hover { background: rgba(239, 68, 68, 0.1); }

/* GRID DE REGISTRO */
.registro-grid { display: grid; grid-template-columns: 1fr 1.3fr; gap: 4rem; }

.column-header { margin-bottom: 2rem; }
.column-header h3 { font-size: 1.4rem; font-weight: 800; }
.column-header p { color: var(--text-muted); font-size: 0.85rem; }

/* SUGESTÕES */
.sugestoes-stack { display: flex; flex-direction: column; gap: 1rem; }
.sugestao-card {
  background: var(--bg-card); padding: 1.5rem; border-radius: 20px;
  display: flex; justify-content: space-between; align-items: flex-start;
  border: 1px solid rgba(255,255,255,0.03);
}

.items-list {
  list-style: none;
  padding: 0;
  margin: 0.5rem 0 0 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.item-entry {
  font-size: 0.75rem;
  color: var(--text-muted);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.item-portion {
  font-weight: 600;
  color: rgba(148, 163, 184, 0.7);
  margin-left: 8px;
  white-space: nowrap;
}

.sugestao-info strong { display: block; font-size: 1.1rem; margin-bottom: 0.5rem; }
.option-tag { font-size: 0.7rem; font-weight: 600; color: var(--text-muted); background: rgba(255,255,255,0.05); padding: 2px 6px; border-radius: 4px; margin-left: 6px; vertical-align: middle; }
.macros-badges { display: flex; gap: 8px; }
.macros-badges span { background: rgba(255,255,255,0.05); padding: 2px 8px; border-radius: 6px; font-size: 0.75rem; color: var(--text-muted); }

.btn-add-plus {
  background: rgba(16, 185, 129, 0.1); color: var(--accent); border: none;
  padding: 0.6rem 1rem; border-radius: 10px; font-weight: 700; cursor: pointer; transition: 0.3s;
}
.btn-add-plus:hover { background: var(--accent); color: var(--bg-deep); }

.btn-logged {
  background: rgba(16, 185, 129, 0.05); color: var(--accent); border: 1px solid rgba(16, 185, 129, 0.2);
  padding: 0.6rem 1rem; border-radius: 10px; font-weight: 700; font-size: 0.85rem;
  cursor: default; opacity: 0.7;
}

/* HISTÓRICO / TIMELINE */
.history-timeline { display: flex; flex-direction: column; gap: 1.5rem; }
.log-entry { display: flex; gap: 1.5rem; align-items: flex-start; }
.log-time-box { width: 50px; padding-top: 1.2rem; font-weight: 800; font-size: 0.85rem; color: var(--text-muted); }

.log-main-card {
  flex: 1; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05);
  padding: 1.5rem; border-radius: 20px; display: flex; justify-content: space-between; align-items: center;
}

.log-text strong { display: block; font-size: 1rem; }
.log-text p { font-size: 0.85rem; color: var(--text-muted); margin-top: 2px; }

.btn-remove-log {
  background: transparent; border: 1px solid rgba(239, 68, 68, 0.2);
  color: #f87171; padding: 0.4rem 0.8rem; border-radius: 8px; font-size: 0.75rem;
  font-weight: 700; cursor: pointer; transition: 0.2s;
}
.btn-remove-log:hover { background: #ef4444; color: white; border-color: #ef4444; }

.empty-log-msg { color: var(--text-muted); font-style: italic; font-size: 0.9rem; padding: 2rem 0; }

@media (max-width: 900px) {
  .registro-grid { grid-template-columns: 1fr; }
  .balance-highlight-card { flex-direction: column; text-align: center; gap: 2rem; padding: 2rem; }
}
</style>
