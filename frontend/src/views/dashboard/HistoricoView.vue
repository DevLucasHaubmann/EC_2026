<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { aiService } from '@/services/modules/aiRecommendation'
import type { Recommendation, MealType } from '@/types/api'
import { MEAL_LABELS } from '@/constants/labels'

const loading = ref(true)
const error = ref<string | null>(null)
const recommendations = ref<Recommendation.Response[]>([])
const selected = ref<Recommendation.Response | null>(null)

// ID da única dieta "atual": o primeiro non-ARCHIVED na lista ordenada por data desc.
// Garante que, mesmo com dados inconsistentes no backend, apenas uma dieta exibe "Ativa".
// Assumes recommendations.value is sorted by createdAt desc (enforced by load()).
const currentDietId = computed((): number | null => {
  return recommendations.value.find(r => r.status !== 'ARCHIVED')?.id ?? null
})

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    const data = await aiService.getHistory()
    recommendations.value = [...data].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    )
  } catch {
    error.value = 'Não foi possível carregar o histórico. Tente novamente.'
  } finally {
    loading.value = false
  }
}

function selectItem(rec: Recommendation.Response): void {
  selected.value = selected.value?.id === rec.id ? null : rec
}

function isCurrent(rec: Recommendation.Response): boolean {
  return rec.id === currentDietId.value
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit', month: 'long', year: 'numeric',
  })
}

function mealLabel(type: string): string {
  return MEAL_LABELS[type as MealType] ?? type
}

// Returns formatted macros for a given option, or dash-strings when the option is absent.
// O(1) per call — no I/O, no iteration over the option list.
function optionMacros(opt: Recommendation.MealOption): { prot: string; carb: string; gord: string } {
  return {
    prot: `${Math.round(opt.totalProtein)}g`,
    carb: `${Math.round(opt.totalCarbs)}g`,
    gord: `${Math.round(opt.totalFat)}g`,
  }
}

onMounted(load)
</script>

<template>
  <div class="historico-page">
    <main class="historico-content">

      <header class="page-header">
        <span class="category-tag">Dietas Geradas</span>
        <h1>Histórico de dietas</h1>
        <p>Consulte todas as dietas geradas pela IA para o seu perfil.</p>
      </header>

      <!-- LOADING -->
      <div v-if="loading" class="state-center">
        <div class="spinner"></div>
        <p class="state-msg">Carregando histórico...</p>
      </div>

      <!-- ERRO -->
      <div v-else-if="error" class="state-center">
        <div class="error-card">
          <div class="error-icon" aria-hidden="true"></div>
          <p class="error-text">{{ error }}</p>
          <button type="button" class="retry-btn" @click="load">Tentar novamente</button>
        </div>
      </div>

      <!-- VAZIO -->
      <div v-else-if="recommendations.length === 0" class="state-center">
        <div class="empty-card">
          <div class="empty-icon" aria-hidden="true"></div>
          <p class="empty-title">Nenhuma dieta gerada ainda.</p>
          <p class="empty-sub">
            Gere sua primeira dieta em <strong>Minha Dieta</strong> para vê-la aqui.
          </p>
        </div>
      </div>

      <!-- LISTA + DETALHE -->
      <div v-else class="historico-body">

        <!-- Lista de dietas -->
        <section class="diet-list" aria-label="Lista de dietas geradas">
          <button
            v-for="rec in recommendations"
            :key="rec.id"
            type="button"
            class="diet-card"
            :class="{ 'diet-card--selected': selected?.id === rec.id }"
            :aria-pressed="selected?.id === rec.id"
            @click="selectItem(rec)"
          >
            <div class="diet-card-top">
              <span class="diet-date">{{ formatDate(rec.createdAt) }}</span>
              <span
                class="status-badge"
                :class="isCurrent(rec) ? 'badge-active' : 'badge-archived'"
              >
                {{ isCurrent(rec) ? 'Ativa' : 'Arquivada' }}
              </span>
            </div>
            <p class="diet-summary-preview">
              {{ rec.summary || 'Plano alimentar personalizado.' }}
            </p>
            <span class="diet-card-cta" aria-hidden="true">
              {{ selected?.id === rec.id ? 'Fechar ↑' : 'Ver detalhes →' }}
            </span>
          </button>
        </section>

        <!-- Painel de detalhe -->
        <section
          v-if="selected"
          class="detail-panel"
          aria-label="Detalhes da dieta selecionada"
        >
          <div
            class="detail-status-banner"
            :class="isCurrent(selected) ? 'banner-active' : 'banner-archived'"
          >
            <span v-if="isCurrent(selected)">
              Esta é sua dieta atual — visível também em Minha Dieta.
            </span>
            <span v-else>
              Dieta arquivada — não é mais seu plano atual.
            </span>
          </div>

          <header class="detail-header">
            <span class="detail-gen-date">Gerada em: {{ formatDate(selected.createdAt) }}</span>
            <h2>Plano alimentar</h2>
            <p class="detail-summary">
              {{ selected.summary || 'Plano alimentar personalizado.' }}
            </p>
          </header>

          <!-- Alertas -->
          <div v-if="(selected.alerts ?? []).length > 0" class="detail-alerts">
            <div v-for="(alert, i) in selected.alerts" :key="i" class="alert-row">
              <span class="alert-dot" aria-hidden="true"></span>
              <p>{{ alert }}</p>
            </div>
          </div>

          <!-- Meta calórica -->
          <div class="detail-calorie-target">
            <span class="detail-calorie-label">Meta calórica diária</span>
            <span class="detail-calorie-val">{{ selected.plan.dailyCalorieTarget }} kcal</span>
          </div>

          <!-- Refeições -->
          <section class="detail-meals" aria-label="Refeições do plano">
            <article
              v-for="(meal, mealIdx) in selected.plan.meals"
              :key="mealIdx"
              class="detail-meal-card"
            >
              <div class="detail-meal-header">
                <span class="detail-meal-type">{{ mealLabel(meal.mealType) }}</span>
                <span class="detail-meal-kcal">{{ meal.calorieTarget }} kcal</span>
              </div>

              <div
                v-for="option in meal.options"
                :key="option.optionNumber"
                class="detail-option"
              >
                <span v-if="meal.options.length > 1" class="detail-option-label">
                  Opção {{ option.optionNumber }}
                </span>

                <ul class="detail-items-list">
                  <li
                    v-for="item in option.items"
                    :key="item.foodId"
                    class="detail-item-row"
                  >
                    <span class="detail-item-name">{{ item.displayName || item.name }}</span>
                    <span class="detail-item-portion">{{ item.portionGrams }}g</span>
                  </li>
                </ul>

                <div class="detail-meal-macros">
                  <span class="detail-macro detail-macro-prot">
                    {{ optionMacros(option).prot }} prot
                  </span>
                  <span class="detail-macro detail-macro-carb">
                    {{ optionMacros(option).carb }} carb
                  </span>
                  <span class="detail-macro detail-macro-gord">
                    {{ optionMacros(option).gord }} gord
                  </span>
                </div>
              </div>
            </article>
          </section>

          <!-- Dicas -->
          <section v-if="(selected.tips ?? []).length > 0" class="detail-tips">
            <h3>Dicas personalizadas</h3>
            <ul class="detail-tips-list">
              <li
                v-for="(tip, i) in selected.tips"
                :key="i"
                class="detail-tip-item"
              >
                {{ tip }}
              </li>
            </ul>
          </section>
        </section>

        <!-- Instrução inicial (nada selecionado) -->
        <div v-else class="detail-placeholder">
          <p>Selecione uma dieta na lista para visualizar os detalhes.</p>
        </div>

      </div>

    </main>
  </div>
</template>

<style scoped>
/* --bg-deep, --bg-card, --accent, --text-muted herdados de :root (base.css) */

.historico-page {
  min-height: 100vh;
  background-color: var(--bg-deep);
  color: white;
  font-family: 'Inter', sans-serif;
}

.historico-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 4rem 1.5rem;
}

/* CABEÇALHO DA PÁGINA */
.page-header {
  margin-bottom: 3rem;
}

.category-tag {
  color: var(--accent);
  font-weight: 800;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.page-header h1 {
  font-size: 2.5rem;
  font-weight: 900;
  margin: 0.5rem 0;
  letter-spacing: -1px;
}

.page-header p {
  color: var(--text-muted);
  font-size: 1.1rem;
}

/* ESTADOS CENTRALIZADOS */
.state-center {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem;
}

.state-msg {
  color: var(--text-muted);
  font-size: 1.1rem;
  font-weight: 600;
}

/* SPINNER */
.spinner {
  width: 44px;
  height: 44px;
  border: 4px solid rgba(16, 185, 129, 0.15);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* CARD ERRO */
.error-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  background: rgba(248, 113, 113, 0.06);
  border: 1px solid rgba(248, 113, 113, 0.2);
  border-radius: 20px;
  padding: 2.5rem 3rem;
  max-width: 480px;
  text-align: center;
}

.error-icon {
  width: 40px;
  height: 40px;
  background: #fca5a5;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2L2 22h20L12 2zm0 3.75L19.5 20H4.5L12 5.75zM11 10h2v5h-2v-5zm0 6h2v2h-2v-2z'/%3E%3C/svg%3E") no-repeat center;
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2L2 22h20L12 2zm0 3.75L19.5 20H4.5L12 5.75zM11 10h2v5h-2v-5zm0 6h2v2h-2v-2z'/%3E%3C/svg%3E") no-repeat center;
}

.error-text {
  color: #fca5a5;
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
  line-height: 1.5;
}

.retry-btn {
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.3);
  color: #fca5a5;
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.retry-btn:hover {
  background: rgba(248, 113, 113, 0.22);
}

/* CARD VAZIO */
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  background: var(--bg-card);
  border-radius: 20px;
  padding: 3rem 3.5rem;
  max-width: 480px;
  text-align: center;
}

.empty-icon {
  width: 48px;
  height: 48px;
  background: var(--accent);
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M20 3h-2.19c-.45-1.18-1.58-2-2.91-2h-5.8c-1.33 0-2.46.82-2.91 2H4c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3.5 10H7.5c-1.38 0-2.5 1.12-2.5 2.5v5.5c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2v-5.5c0-1.38-1.12-2.5-2.5-2.5z'/%3E%3C/svg%3E") no-repeat center;
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M20 3h-2.19c-.45-1.18-1.58-2-2.91-2h-5.8c-1.33 0-2.46.82-2.91 2H4c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3.5 10H7.5c-1.38 0-2.5 1.12-2.5 2.5v5.5c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2v-5.5c0-1.38-1.12-2.5-2.5-2.5z'/%3E%3C/svg%3E") no-repeat center;
  margin-bottom: 0.25rem;
}

.empty-title {
  color: white;
  font-size: 1.15rem;
  font-weight: 800;
  margin: 0;
}

.empty-sub {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin: 0;
  line-height: 1.5;
}

.empty-sub strong {
  color: var(--accent);
}

/* CORPO PRINCIPAL: LISTA + DETALHE */
.historico-body {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 2rem;
  align-items: start;
}

/* LISTA */
.diet-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  padding-right: 4px;
}

.diet-list::-webkit-scrollbar {
  width: 4px;
}

.diet-list::-webkit-scrollbar-track {
  background: transparent;
}

.diet-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.diet-card {
  width: 100%;
  text-align: left;
  background: var(--bg-card);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.diet-card:hover {
  background: rgba(30, 41, 59, 0.9);
  border-color: rgba(255, 255, 255, 0.1);
}

.diet-card--selected {
  border-color: var(--accent);
  background: rgba(16, 185, 129, 0.06);
}

.diet-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.diet-date {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-muted);
}

.status-badge {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 2px 7px;
  border-radius: 6px;
  flex-shrink: 0;
}

.badge-active {
  background: rgba(16, 185, 129, 0.12);
  color: var(--accent);
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.badge-archived {
  background: rgba(148, 163, 184, 0.08);
  color: var(--text-muted);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.diet-summary-preview {
  font-size: 0.82rem;
  color: #cbd5e1;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.diet-card-cta {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--accent);
  margin-top: 0.25rem;
}

/* PAINEL DE DETALHE */
.detail-panel {
  background: var(--bg-card);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 2rem 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  position: sticky;
  top: 1.5rem;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.detail-panel::-webkit-scrollbar {
  width: 4px;
}

.detail-panel::-webkit-scrollbar-track {
  background: transparent;
}

.detail-panel::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

/* Banner status */
.detail-status-banner {
  border-radius: 10px;
  padding: 0.6rem 1rem;
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.4;
}

.banner-active {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: #6ee7b7;
}

.banner-archived {
  background: rgba(148, 163, 184, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.12);
  color: var(--text-muted);
}

/* Cabeçalho do detalhe */
.detail-header {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.detail-gen-date {
  color: var(--accent);
  font-size: 0.72rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.detail-header h2 {
  font-size: 1.5rem;
  font-weight: 900;
  letter-spacing: -0.5px;
}

.detail-summary {
  color: var(--text-muted);
  font-size: 0.9rem;
  line-height: 1.6;
}

/* Alertas */
.detail-alerts {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  background: rgba(248, 113, 113, 0.05);
  border: 1px solid rgba(248, 113, 113, 0.18);
  border-radius: 12px;
  padding: 1rem 1.25rem;
}

.alert-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.alert-dot {
  width: 5px;
  height: 5px;
  background: #f87171;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.alert-row p {
  font-size: 0.85rem;
  color: #fca5a5;
  line-height: 1.5;
}

/* Meta calórica */
.detail-calorie-target {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.18);
  border-radius: 12px;
  padding: 0.9rem 1.25rem;
}

.detail-calorie-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.detail-calorie-val {
  font-size: 1.2rem;
  font-weight: 900;
  color: var(--accent);
}

/* Refeições */
.detail-meals {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.detail-meal-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 14px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.detail-meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-meal-type {
  font-size: 0.95rem;
  font-weight: 800;
}

.detail-meal-kcal {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.05);
  padding: 2px 8px;
  border-radius: 7px;
}

.detail-items-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.detail-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.35rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  gap: 0.5rem;
}

.detail-item-row:last-child {
  border-bottom: none;
}

.detail-item-name {
  font-size: 0.85rem;
  color: #cbd5e1;
  line-height: 1.35;
}

.detail-item-portion {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Option block within a meal card */
.detail-option {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
}

.detail-option:first-of-type {
  border-top: none;
  padding-top: 0;
}

.detail-option-label {
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--text-muted);
}

.detail-meal-macros {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding-top: 0.4rem;
}

.detail-macro {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 7px;
}

.detail-macro-prot {
  background: rgba(96, 165, 250, 0.1);
  color: #60a5fa;
}

.detail-macro-carb {
  background: rgba(251, 191, 36, 0.1);
  color: #fbbf24;
}

.detail-macro-gord {
  background: rgba(251, 146, 60, 0.1);
  color: #fb923c;
}

/* Dicas */
.detail-tips h3 {
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--accent);
  margin-bottom: 0.75rem;
}

.detail-tips-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.detail-tip-item {
  font-size: 0.85rem;
  color: #cbd5e1;
  line-height: 1.5;
  padding-left: 1.1rem;
  position: relative;
}

.detail-tip-item::before {
  content: '•';
  position: absolute;
  left: 0;
  color: var(--accent);
  font-weight: bold;
}

/* Placeholder (nada selecionado) */
.detail-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  background: var(--bg-card);
  border: 1px dashed rgba(255, 255, 255, 0.07);
  border-radius: 24px;
  padding: 2rem;
  text-align: center;
}

.detail-placeholder p {
  color: var(--text-muted);
  font-size: 0.95rem;
  font-weight: 500;
}

/* RESPONSIVO */
@media (max-width: 900px) {
  .historico-body {
    grid-template-columns: 1fr;
  }

  .diet-list {
    max-height: none;
    overflow-y: visible;
  }

  .detail-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 640px) {
  .page-header h1 {
    font-size: 1.8rem;
  }

  .detail-panel {
    padding: 1.5rem;
  }
}
</style>
