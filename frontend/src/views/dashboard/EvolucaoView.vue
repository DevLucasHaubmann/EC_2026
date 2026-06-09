<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { evolutionService } from '@/services/modules/evolution'
import { bodyWeightService } from '@/services/modules/bodyWeight'
import type { Evolution, BodyWeight } from '@/types/api'

// ── Aderência semanal ─────────────────────────────────────────────────────────
const loadingAdherencia = ref(false)
const errorAdherencia = ref<string | null>(null)
const summary = ref<Evolution.WeeklySummary | null>(null)

function getCurrentWeekStart(): string {
  const today = new Date()
  const day = today.getDay()
  const diffToMonday = day === 0 ? -6 : 1 - day
  const monday = new Date(today)
  monday.setDate(today.getDate() + diffToMonday)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${monday.getFullYear()}-${pad(monday.getMonth() + 1)}-${pad(monday.getDate())}`
}

const DAY_LABELS: Record<number, string> = { 1: 'Seg', 2: 'Ter', 3: 'Qua', 4: 'Qui', 5: 'Sex', 6: 'Sáb', 0: 'Dom' }

function dayLabel(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number) as [number, number, number]
  const d = new Date(year, month - 1, day)
  return DAY_LABELS[d.getDay()] ?? isoDate
}

const aderenciaSemanal = computed<{ dia: string; pct: number | null }[]>(() => {
  if (!summary.value) return []
  return summary.value.days.map((d) => ({
    dia: dayLabel(d.date),
    pct: d.adherencePercentage,
  }))
})

const mediaAdherencia = computed<string>(() => {
  const avg = summary.value?.averageAdherencePercentage
  return avg != null ? `${avg.toFixed(1)}% Média` : '-- Média'
})

const caloriasMedias = computed<string>(() => {
  if (!summary.value || summary.value.activeDays === 0) return '--'
  const avg = summary.value.totalCalories / summary.value.activeDays
  return `${Math.round(avg)} kcal`
})

const diasAtivos = computed<string>(() => {
  if (!summary.value) return '--'
  return `${summary.value.activeDays} dias / semana`
})

const isEmpty = computed(() => summary.value !== null && summary.value.activeDays === 0)

async function fetchWeeklyData(): Promise<void> {
  loadingAdherencia.value = true
  errorAdherencia.value = null
  try {
    summary.value = await evolutionService.getWeekly(getCurrentWeekStart())
  } catch {
    errorAdherencia.value = 'Não foi possível carregar os dados de evolução. Tente novamente.'
  } finally {
    loadingAdherencia.value = false
  }
}

// ── Peso corporal ─────────────────────────────────────────────────────────────
const loadingPeso = ref(false)
const errorPeso = ref<string | null>(null)
const weightSummary = ref<BodyWeight.SummaryResponse | null>(null)

// Form de registro
const novopeso = ref<number | null>(null)
const registrandoPeso = ref(false)
// Controla a revelação do formulário inline a partir do CTA "Registrar novo peso"
// (substitui o antigo <details>, que parecia um link e não um botão).
const mostrarFormPeso = ref(false)

// Counts decimal places from the number's canonical string representation.
// Using String(v) is reliable for values in the 20–500 range — no exponential
// notation is produced and floating-point artifacts do not add spurious digits
// (e.g. String(75.99) === "75.99", String(75.999) === "75.999").
function decimalPlaces(v: number): number {
  const str = String(v)
  const dotIndex = str.indexOf('.')
  return dotIndex === -1 ? 0 : str.length - dotIndex - 1
}

const erroNovoPeso = computed<string | null>(() => {
  const v = novopesoNumerico.value
  if (v === null) return null
  if (v < 20) return 'Peso mínimo: 20 kg.'
  if (v > 500) return 'Peso máximo: 500 kg.'
  if (decimalPlaces(v) > 2) return 'O peso deve ter no máximo 2 casas decimais.'
  return null
})

// Garante que o valor do input seja tratado como number ou null
const novopesoNumerico = computed<number | null>(() => {
  if (novopeso.value === null || novopeso.value === undefined) return null
  const n = Number(novopeso.value)
  return isNaN(n) ? null : n
})

async function fetchWeightSummary(): Promise<void> {
  loadingPeso.value = true
  errorPeso.value = null
  try {
    weightSummary.value = await bodyWeightService.getSummary()
  } catch {
    errorPeso.value = 'Não foi possível carregar os dados de peso. Tente novamente.'
  } finally {
    loadingPeso.value = false
  }
}

// Fecha o formulário inline e descarta o valor digitado, para que reabrir não traga
// um peso inválido anterior (e seu erro) já preenchido.
function cancelarRegistro(): void {
  novopeso.value = null
  mostrarFormPeso.value = false
}

async function registrarPeso(): Promise<void> {
  const v = novopesoNumerico.value
  if (v === null || erroNovoPeso.value !== null) return
  registrandoPeso.value = true
  try {
    await bodyWeightService.register(v)
    novopeso.value = null
    mostrarFormPeso.value = false
    await fetchWeightSummary()
  } catch {
    errorPeso.value = 'Não foi possível registrar o peso. Tente novamente.'
  } finally {
    registrandoPeso.value = false
  }
}

// ── Gráfico SVG ───────────────────────────────────────────────────────────────
const SVG_W = 480
const SVG_H = 140
const PADDING = { top: 16, right: 16, bottom: 32, left: 44 }

const chartPoints = computed<{ x: number; y: number; label: string; weight: number }[]>(() => {
  const history = weightSummary.value?.history ?? []
  if (history.length === 0) return []

  const weights = history.map((e) => e.weightKg)
  const minW = Math.min(...weights)
  const maxW = Math.max(...weights)
  const range = maxW - minW
  // Add margin so the line never touches top/bottom edges
  const yMin = minW - (range === 0 ? 2 : range * 0.15)
  const yMax = maxW + (range === 0 ? 2 : range * 0.15)

  const innerW = SVG_W - PADDING.left - PADDING.right
  const innerH = SVG_H - PADDING.top - PADDING.bottom

  return history.map((entry, i) => {
    const x = PADDING.left + (history.length === 1 ? innerW / 2 : (i / (history.length - 1)) * innerW)
    const y = PADDING.top + innerH - ((entry.weightKg - yMin) / (yMax - yMin)) * innerH
    return { x, y, label: entry.measuredDate.slice(5), weight: entry.weightKg }
  })
})

const svgPolyline = computed<string>(() =>
  chartPoints.value.map((p) => `${p.x},${p.y}`).join(' '),
)

const svgAreaPath = computed<string>(() => {
  const pts = chartPoints.value
  if (pts.length === 0) return ''
  const bottomY = SVG_H - PADDING.bottom
  const first = pts[0]!
  const last = pts[pts.length - 1]!
  const linePart = pts.map((p) => `${p.x},${p.y}`).join(' L ')
  return `M ${first.x},${bottomY} L ${linePart} L ${last.x},${bottomY} Z`
})

// ── Resumo derivado ───────────────────────────────────────────────────────────
const trendLabel = computed<string>(() => {
  const t = weightSummary.value?.trend
  if (t === 'LOSS') return 'Perda'
  if (t === 'GAIN') return 'Ganho'
  if (t === 'STABLE') return 'Estável'
  return ''
})

const trendClass = computed<string>(() => {
  const t = weightSummary.value?.trend
  if (t === 'LOSS') return 'trend--loss'
  if (t === 'GAIN') return 'trend--gain'
  if (t === 'STABLE') return 'trend--stable'
  return ''
})

const changeText = computed<string>(() => {
  const ws = weightSummary.value
  if (!ws || ws.totalChangeKg === null) return ''
  const abs = Math.abs(ws.totalChangeKg).toFixed(1)
  if (ws.trend === 'LOSS') return `Você perdeu ${abs} kg`
  if (ws.trend === 'GAIN') return `Você ganhou ${abs} kg`
  return 'Peso estável'
})

const svgAriaLabel = computed<string>(() => {
  const history = weightSummary.value?.history ?? []
  if (history.length === 0) return 'Gráfico de peso corporal sem dados'
  const first = history[0]!
  const last = history[history.length - 1]!
  return `Gráfico de evolução do peso: de ${first.weightKg} kg em ${first.measuredDate} até ${last.weightKg} kg em ${last.measuredDate}`
})

// ── Lifecycle ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchWeightSummary()
  fetchWeeklyData()
})
</script>

<template>
  <div class="evolucao-page">
    <main class="evolucao-content">

      <!-- Cabeçalho -->
      <header class="page-header">
        <span class="category-tag">Analytics & Performance</span>
        <h1>Progresso e acompanhamento</h1>
        <p>Análise detalhada da sua aderência ao plano alimentar.</p>
      </header>

      <!-- ══════════════════════════════════════════════════════════════════════
           SEÇÃO: PESO CORPORAL
      ═══════════════════════════════════════════════════════════════════════ -->
      <section class="weight-section" aria-labelledby="weight-section-title">
        <div class="section-heading">
          <h2 id="weight-section-title" class="section-title">Peso Corporal</h2>
        </div>

        <!-- Estado: carregando -->
        <div v-if="loadingPeso" class="state-feedback" role="status" aria-live="polite">
          <p>Carregando dados de peso...</p>
        </div>

        <!-- Estado: erro -->
        <div v-else-if="errorPeso" class="state-feedback state-feedback--error" role="alert">
          <p>{{ errorPeso }}</p>
          <button class="retry-btn" @click="fetchWeightSummary">Tentar novamente</button>
        </div>

        <!-- Estado: sem dados (entryCount === 0) -->
        <div v-else-if="weightSummary && weightSummary.entryCount === 0" class="weight-empty-card">
          <p class="weight-empty-title">Nenhum peso registrado ainda.</p>
          <p class="weight-empty-hint">Registre seu peso para começar a acompanhar sua evolução.</p>

          <!-- Formulário de registro inline -->
          <form class="weight-form" @submit.prevent="registrarPeso" novalidate>
            <div class="weight-form-field">
              <label for="novo-peso" class="weight-form-label">Peso atual (kg)</label>
              <input
                id="novo-peso"
                v-model.number="novopeso"
                type="number"
                step="0.1"
                min="20"
                max="500"
                placeholder="ex: 75.5"
                class="weight-form-input"
                :class="{ 'weight-form-input--error': erroNovoPeso }"
                :aria-invalid="erroNovoPeso ? 'true' : 'false'"
                :aria-describedby="erroNovoPeso ? 'erro-novo-peso' : undefined"
              />
              <p v-if="erroNovoPeso" id="erro-novo-peso" class="weight-form-error" aria-live="polite">
                {{ erroNovoPeso }}
              </p>
            </div>
            <button
              type="submit"
              class="weight-register-btn"
              :disabled="registrandoPeso || novopesoNumerico === null || erroNovoPeso !== null"
              :aria-busy="registrandoPeso"
            >
              <span v-if="registrandoPeso" class="btn-spinner" aria-hidden="true"></span>
              {{ registrandoPeso ? 'Registrando...' : 'Registrar Peso' }}
            </button>
          </form>
        </div>

        <!-- Estado: com dados -->
        <div v-else-if="weightSummary" class="weight-data-card">

          <!-- Cards de resumo -->
          <div class="weight-summary-grid">
            <div class="weight-stat-card">
              <label>Peso Atual</label>
              <strong>{{ weightSummary.currentWeightKg != null ? weightSummary.currentWeightKg + ' kg' : '—' }}</strong>
            </div>
            <div class="weight-stat-card">
              <label>Peso Inicial</label>
              <strong>{{ weightSummary.initialWeightKg != null ? weightSummary.initialWeightKg + ' kg' : '—' }}</strong>
            </div>
            <div class="weight-stat-card">
              <label>Registros</label>
              <strong>{{ weightSummary.entryCount }}</strong>
            </div>

            <!-- Variação e tendência: só quando entryCount >= 2 -->
            <div v-if="weightSummary.entryCount >= 2" class="weight-stat-card weight-stat-card--trend">
              <label>Variação Total</label>
              <div class="trend-row">
                <strong>
                  {{ weightSummary.totalChangeKg != null
                    ? (weightSummary.totalChangeKg > 0 ? '+' : '') + weightSummary.totalChangeKg.toFixed(1) + ' kg'
                    : '—' }}
                </strong>
                <span v-if="weightSummary.trend" class="trend-badge" :class="trendClass">
                  {{ trendLabel }}
                </span>
              </div>
            </div>
          </div>

          <!-- Mensagem de tendência -->
          <p v-if="weightSummary.entryCount >= 2 && changeText" class="weight-change-msg" :class="trendClass">
            {{ changeText }}
          </p>
          <p v-else-if="weightSummary.entryCount === 1" class="weight-single-hint">
            Registre mais pesos para ver sua tendência.
          </p>

          <!-- Gráfico SVG: só com 2+ registros, para não exibir um ponto isolado.
               Com 1 registro, a mensagem acima ("Registre mais pesos…") já orienta o usuário. -->
          <div v-if="weightSummary.entryCount >= 2" class="weight-chart-wrapper">
            <svg
              :viewBox="`0 0 ${SVG_W} ${SVG_H}`"
              class="weight-chart"
              role="img"
              :aria-label="svgAriaLabel"
              preserveAspectRatio="xMidYMid meet"
            >
              <defs>
                <linearGradient id="weight-area-gradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#10b981" stop-opacity="0.25" />
                  <stop offset="100%" stop-color="#10b981" stop-opacity="0.02" />
                </linearGradient>
              </defs>

              <!-- Área preenchida -->
              <path
                v-if="chartPoints.length >= 2"
                :d="svgAreaPath"
                fill="url(#weight-area-gradient)"
              />

              <!-- Linha do gráfico -->
              <polyline
                v-if="chartPoints.length >= 2"
                :points="svgPolyline"
                fill="none"
                stroke="#10b981"
                stroke-width="2"
                stroke-linejoin="round"
                stroke-linecap="round"
              />

              <!-- Pontos + labels de data -->
              <g v-for="(pt, i) in chartPoints" :key="i">
                <circle
                  :cx="pt.x"
                  :cy="pt.y"
                  r="4"
                  fill="#10b981"
                  stroke="#0f172a"
                  stroke-width="2"
                />
                <text
                  v-if="chartPoints.length <= 12 || i === 0 || i === chartPoints.length - 1"
                  :x="pt.x"
                  :y="SVG_H - PADDING.bottom + 14"
                  text-anchor="middle"
                  class="chart-label"
                >
                  {{ pt.label }}
                </text>
              </g>
            </svg>
          </div>

          <!-- Registro de novo peso: CTA real (botão) que revela o formulário inline -->
          <div class="weight-register-block">
            <button
              v-if="!mostrarFormPeso"
              type="button"
              class="weight-register-cta"
              aria-controls="form-novo-peso-inline"
              aria-expanded="false"
              @click="mostrarFormPeso = true"
            >
              <span class="weight-register-cta__icon" aria-hidden="true">+</span>
              Registrar novo peso
            </button>

            <form
              v-else
              id="form-novo-peso-inline"
              class="weight-form weight-form--inline"
              @submit.prevent="registrarPeso"
              novalidate
            >
              <div class="weight-form-field">
                <label for="novo-peso-inline" class="weight-form-label">Peso atual (kg)</label>
                <input
                  id="novo-peso-inline"
                  v-model.number="novopeso"
                  type="number"
                  step="0.1"
                  min="20"
                  max="500"
                  placeholder="ex: 75.5"
                  class="weight-form-input"
                  :class="{ 'weight-form-input--error': erroNovoPeso }"
                  :aria-invalid="erroNovoPeso ? 'true' : 'false'"
                  :aria-describedby="erroNovoPeso ? 'erro-novo-peso-inline' : undefined"
                />
                <p v-if="erroNovoPeso" id="erro-novo-peso-inline" class="weight-form-error" aria-live="polite">
                  {{ erroNovoPeso }}
                </p>
              </div>
              <div class="weight-form-actions">
                <button type="button" class="weight-form-cancel" @click="cancelarRegistro">
                  Cancelar
                </button>
                <button
                  type="submit"
                  class="weight-register-btn"
                  :disabled="registrandoPeso || novopesoNumerico === null || erroNovoPeso !== null"
                  :aria-busy="registrandoPeso"
                >
                  <span v-if="registrandoPeso" class="btn-spinner" aria-hidden="true"></span>
                  {{ registrandoPeso ? 'Registrando...' : 'Registrar' }}
                </button>
              </div>
            </form>
          </div>

        </div>
      </section>

      <!-- ══════════════════════════════════════════════════════════════════════
           SEÇÃO: ADERÊNCIA SEMANAL
      ═══════════════════════════════════════════════════════════════════════ -->

      <!-- Estado: carregando aderência -->
      <div v-if="loadingAdherencia" class="state-feedback">
        <p>Carregando dados de evolução...</p>
      </div>

      <!-- Estado: erro aderência -->
      <div v-else-if="errorAdherencia" class="state-feedback state-feedback--error">
        <p>{{ errorAdherencia }}</p>
        <button class="retry-btn" @click="fetchWeeklyData">Tentar novamente</button>
      </div>

      <!-- Estado: sem dados de aderência -->
      <div v-else-if="isEmpty" class="state-feedback state-feedback--empty">
        <p>Nenhuma refeição registrada nesta semana.</p>
        <p class="state-feedback__hint">Efetive refeições no plano diário para visualizar sua evolução.</p>
      </div>

      <!-- Estado: sucesso — gráfico de aderência com dados reais -->
      <template v-else-if="summary">
        <div class="analytics-grid">
          <section class="chart-card">
            <div class="card-header">
              <h3>Aderência Semanal</h3>
              <span class="pct-avg">{{ mediaAdherencia }}</span>
            </div>
            <div class="adherence-list">
              <div v-for="dia in aderenciaSemanal" :key="dia.dia" class="adherence-item">
                <span class="day-label">{{ dia.dia }}</span>
                <div class="adherence-bar-bg">
                  <div
                    class="adherence-bar-fill"
                    :style="{ width: (dia.pct ?? 0) + '%' }"
                  ></div>
                </div>
                <span class="pct-label">{{ dia.pct != null ? dia.pct + '%' : '--' }}</span>
              </div>
            </div>
          </section>
        </div>

        <section class="analytics-footer">
          <div class="footer-stat">
            <label>Calorias médias</label>
            <strong>{{ caloriasMedias }}</strong>
          </div>
          <div class="footer-stat">
            <label>Dias ativos</label>
            <strong>{{ diasAtivos }}</strong>
          </div>
          <div class="footer-stat">
            <label>Aderência média</label>
            <strong class="accent-text">
              {{ summary.averageAdherencePercentage != null
                ? summary.averageAdherencePercentage.toFixed(1) + '%'
                : '--' }}
            </strong>
          </div>
        </section>
      </template>

    </main>
  </div>
</template>

<style scoped>
.evolucao-page {
  --bg-deep: #0f172a;
  --bg-card: #1e293b;
  --accent: #10b981;
  --text-muted: #94a3b8;

  min-height: 100vh;
  background-color: var(--bg-deep);
  color: white;
  font-family: 'Inter', sans-serif;
}

.evolucao-content { max-width: 1100px; margin: 0 auto; padding: 4rem 1.5rem; }

.page-header { margin-bottom: 3.5rem; }
.category-tag { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
.page-header h1 { font-size: 2.5rem; font-weight: 900; margin: 0.5rem 0; letter-spacing: -1px; }
.page-header p { color: var(--text-muted); font-size: 1.1rem; }

/* ── SEÇÃO PESO ── */
.weight-section { margin-bottom: 3rem; }

.section-heading { margin-bottom: 1.5rem; }
.section-title { font-size: 1.1rem; font-weight: 800; text-transform: uppercase; letter-spacing: 1.5px; color: var(--text-muted); margin: 0; }

/* Estados de feedback */
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

/* Card vazio */
.weight-empty-card {
  background: var(--bg-card);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 28px;
  padding: 2.5rem;
  text-align: center;
}
.weight-empty-title { font-size: 1.05rem; font-weight: 700; margin: 0 0 0.5rem; }
.weight-empty-hint { font-size: 0.9rem; color: var(--text-muted); margin: 0 0 2rem; }

/* Card com dados */
.weight-data-card {
  background: var(--bg-card);
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 28px;
  padding: 2.5rem;
}

/* Grid de resumo */
.weight-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.weight-stat-card {
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 16px;
  padding: 1rem 1.25rem;
}
.weight-stat-card label {
  display: block;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--text-muted);
  font-weight: 700;
  margin-bottom: 6px;
}
.weight-stat-card strong { font-size: 1.35rem; font-weight: 800; }

.weight-stat-card--trend strong { font-size: 1.2rem; }

.trend-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.trend-badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 8px;
}
.trend--loss { color: var(--accent); }
.trend--loss.trend-badge { background: rgba(16,185,129,0.12); }
.trend--gain { color: #f59e0b; }
.trend--gain.trend-badge { background: rgba(245,158,11,0.12); }
.trend--stable { color: var(--text-muted); }
.trend--stable.trend-badge { background: rgba(148,163,184,0.1); }

/* Mensagem de mudança */
.weight-change-msg {
  font-size: 0.9rem;
  font-weight: 700;
  margin: 0 0 1.5rem;
}
.weight-single-hint {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0 0 1.5rem;
}

/* Gráfico */
.weight-chart-wrapper {
  width: 100%;
  overflow-x: auto;
  margin-bottom: 1.5rem;
}
.weight-chart {
  display: block;
  width: 100%;
  height: auto;
}
.chart-label {
  fill: var(--text-muted);
  font-size: 9px;
  font-family: 'Inter', sans-serif;
}

/* CTA real "Registrar novo peso" (substitui o antigo <summary> tipo link) */
.weight-register-block { margin-top: 0.5rem; }
.weight-register-cta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: rgba(16, 185, 129, 0.1);
  color: var(--accent);
  border: 1px solid rgba(16, 185, 129, 0.35);
  border-radius: 12px;
  padding: 0.8rem 1.4rem;
  font-family: inherit;
  font-weight: 800;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}
.weight-register-cta:hover {
  background: rgba(16, 185, 129, 0.18);
  border-color: var(--accent);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.15);
}
.weight-register-cta:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.35);
}
.weight-register-cta__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent);
  color: #0f172a;
  font-size: 1rem;
  font-weight: 900;
  line-height: 1;
}

/* Ações do formulário inline (cancelar + registrar) */
.weight-form-actions { display: flex; align-items: center; gap: 0.75rem; }
.weight-form-cancel {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--text-muted);
  border-radius: 12px;
  padding: 0.75rem 1.25rem;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s, color 0.2s;
}
.weight-form-cancel:hover { color: white; border-color: rgba(255, 255, 255, 0.25); }
.weight-form-cancel:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.15);
}

/* Formulário de peso (compartilhado) */
.weight-form {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
  flex-wrap: wrap;
  justify-content: center;
}
.weight-form--inline {
  justify-content: flex-start;
  margin-top: 1rem;
}

.weight-form-field { display: flex; flex-direction: column; gap: 6px; }
.weight-form-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.weight-form-input {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  padding: 0.75rem 1rem;
  color: white;
  font-size: 0.95rem;
  font-family: inherit;
  width: 160px;
  transition: border-color 0.2s;
}
.weight-form-input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(16,185,129,0.15);
}
.weight-form-input--error {
  border-color: #f87171;
}
.weight-form-error {
  font-size: 0.76rem;
  color: #f87171;
  font-weight: 600;
  margin: 0;
}

.weight-register-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--accent);
  color: #0f172a;
  border: none;
  border-radius: 12px;
  padding: 0.75rem 1.5rem;
  font-weight: 800;
  font-size: 0.9rem;
  cursor: pointer;
  font-family: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  white-space: nowrap;
}
.weight-register-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(16,185,129,0.2);
}
.weight-register-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.btn-spinner {
  display: inline-block;
  width: 13px;
  height: 13px;
  border: 2px solid rgba(15,23,42,0.3);
  border-top-color: #0f172a;
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── ANALYTICS GRID (aderência) ── */
.analytics-grid { display: grid; grid-template-columns: 1fr; gap: 2rem; margin-bottom: 2rem; }
.chart-card { background: var(--bg-card); padding: 2.5rem; border-radius: 28px; border: 1px solid rgba(255,255,255,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2.5rem; }
.card-header h3 { font-size: 1rem; font-weight: 800; }
.pct-avg { font-size: 0.75rem; font-weight: 700; color: var(--accent); background: rgba(16, 185, 129, 0.1); padding: 4px 10px; border-radius: 8px; }

.adherence-list { display: flex; flex-direction: column; gap: 1rem; }
.adherence-item { display: flex; align-items: center; gap: 12px; }
.day-label { width: 35px; font-size: 0.8rem; font-weight: 700; color: var(--text-muted); }
.adherence-bar-bg { flex: 1; height: 8px; background: rgba(255,255,255,0.05); border-radius: 10px; overflow: hidden; }
.adherence-bar-fill { height: 100%; background: var(--accent); border-radius: 10px; }
.pct-label { width: 40px; font-size: 0.8rem; font-weight: 700; text-align: right; }

.analytics-footer { background: var(--bg-card); padding: 2rem 3rem; border-radius: 28px; display: flex; justify-content: space-between; align-items: center; border: 1px solid rgba(255,255,255,0.05); }
.footer-stat label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
.footer-stat strong { font-size: 1.1rem; font-weight: 800; }
.accent-text { color: var(--accent); }

@media (max-width: 900px) {
  .analytics-footer { flex-direction: column; gap: 2rem; text-align: center; }
  .weight-form { flex-direction: column; align-items: stretch; }
  .weight-form-input { width: 100%; }
  .weight-register-cta { width: 100%; justify-content: center; }
  .weight-form-actions { width: 100%; }
  .weight-form-actions .weight-register-btn,
  .weight-form-cancel { flex: 1; justify-content: center; }
}
</style>
