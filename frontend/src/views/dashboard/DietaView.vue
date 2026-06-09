<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { aiService } from '@/services/modules/aiRecommendation';
import { dashboardService } from '@/services/modules/dashboard';
import type { Recommendation } from '@/types/api';
import { MEAL_LABELS } from '@/constants/labels';
import { DIET_MESSAGES } from '@/constants/messages';
import type { MealType } from '@/types/api';
import { useDietGeneration } from '@/composables/useDietGeneration';
import DietGenerationProgress from '@/components/ui/DietGenerationProgress.vue';
import RecommendationFeedbackForm from '@/components/feedback/RecommendationFeedbackForm.vue';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const erro = ref<string | null>(null);
const recomendacao = ref<Recommendation.Response | null>(null);

// true  = usuário ainda não completou triagem/perfil (deve ir para /triagem)
// false = onboarding completo, mas sem dieta ainda (pode gerar a primeira)
// null  = ainda não determinado (loading) ou falhou ao carregar o status
const precisaTriagem = ref<boolean | null>(null);
const erroOnboarding = ref<string | null>(null);

const {
  isGenerating,
  status: generationStatus,
  error: generationError,
  currentMessage: generationMessage,
  recommendationId: generationRecommendationId,
  start: startGeneration,
} = useDietGeneration();

const generationFailed = computed(() => generationStatus.value === 'FAILED');
// True when generation succeeded but fetching the new recommendation failed.
// Kept separate from `erro` so the old diet stays visible instead of showing a fatal error screen.
const postGenFetchError = ref(false);

async function regenerate(): Promise<void> {
  if (isGenerating.value) return;
  postGenFetchError.value = false;
  // start() resets status to PENDING, clearing generationFailed implicitly.
  await startGeneration();
}

// When generation completes: fetch the new recommendation and update the view.
// On fetch failure, keep the old recommendation visible — do not enter the fatal error state.
watch(generationStatus, async (status) => {
  if (status !== 'COMPLETED') return;
  const rid = generationRecommendationId.value;
  try {
    recomendacao.value = rid
      ? await aiService.getById(rid)
      : await aiService.getLatest();
    // Clear ?rid so the "Dieta atual" badge reflects the newly generated diet.
    if (route.query.rid) router.replace({ query: {} });
  } catch {
    postGenFetchError.value = true;
  }
});

// Acima deste comprimento, um bloco textual exibe o botão "Ver mais/menos".
const LONG_TEXT_THRESHOLD = 160;

// Estado de expansão por bloco textual. Chave estável: ex. 'summary', 'tip-0', 'expl-BREAKFAST'.
const expandedBlocks = ref<Record<string, boolean>>({});

// Índice da opção selecionada por refeição. Chave: `${mealType}-${indexNoPlan}`
const selectedOptionIndex = ref<Record<string, number>>({});

function isLongText(text: string): boolean {
  return text.trim().length > LONG_TEXT_THRESHOLD;
}

function isExpanded(key: string): boolean {
  return expandedBlocks.value[key] ?? false;
}

// Aplica o recorte visual apenas quando há texto longo recolhido (com botão correspondente).
function isClamped(key: string, text: string): boolean {
  return isLongText(text) && !isExpanded(key);
}

function toggleBlock(key: string): void {
  expandedBlocks.value = { ...expandedBlocks.value, [key]: !isExpanded(key) };
}

function mealKey(meal: Recommendation.MealPlanMeal, index: number): string {
  return `${meal.mealType}-${index}`;
}

function mealLabel(type: string): string {
  return MEAL_LABELS[type as MealType] ?? type;
}

function selectedOption(meal: Recommendation.MealPlanMeal, index: number): Recommendation.MealOption | undefined {
  const key = mealKey(meal, index);
  const idx = selectedOptionIndex.value[key] ?? 0;
  return meal.options[idx] ?? meal.options[0];
}

function selectOption(meal: Recommendation.MealPlanMeal, mealIndex: number, optionIdx: number): void {
  const key = mealKey(meal, mealIndex);
  selectedOptionIndex.value = { ...selectedOptionIndex.value, [key]: optionIdx };
}

function activeOptionIndex(meal: Recommendation.MealPlanMeal, mealIndex: number): number {
  return selectedOptionIndex.value[mealKey(meal, mealIndex)] ?? 0;
}

function mealMacros(meal: Recommendation.MealPlanMeal, mealIndex: number) {
  const opt = selectedOption(meal, mealIndex);
  if (!opt) return { prot: '—', carb: '—', gord: '—' };
  return {
    prot: `${Math.round(opt.totalProtein)}g`,
    carb: `${Math.round(opt.totalCarbs)}g`,
    gord: `${Math.round(opt.totalFat)}g`,
  };
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit', month: 'long', year: 'numeric',
  });
}

function summaryText(rec: Recommendation.Response): string {
  return rec.summary?.trim() || 'Plano alimentar personalizado gerado com base no seu perfil.';
}

// Resolves the recommendation to show: by id when arriving from the generation flow
// (?rid), with getLatest as a safe fallback if the id is missing or no longer fetchable.
async function loadRecommendation(): Promise<Recommendation.Response> {
  const ridParam = route.query.rid;
  const rid = Number(ridParam);
  if (typeof ridParam === 'string' && Number.isInteger(rid) && rid > 0) {
    try {
      return await aiService.getById(rid);
    } catch {
      return await aiService.getLatest();
    }
  }
  return await aiService.getLatest();
}

onMounted(async () => {
  postGenFetchError.value = false;
  try {
    recomendacao.value = await loadRecommendation();
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status;
    if (status === 404) {
      recomendacao.value = null;
      // Determine whether the user still needs to complete onboarding or is
      // ready to generate their first diet. This call is only made when there
      // is no recommendation yet, so it is not a redundant request.
      try {
        const dashboard = await dashboardService.getDashboard();
        precisaTriagem.value = dashboard.onboarding.onboardingRequired;
      } catch {
        erroOnboarding.value = DIET_MESSAGES.ONBOARDING_LOAD_ERROR;
      }
    } else {
      erro.value = DIET_MESSAGES.LOAD_ERROR;
    }
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="dieta-page-wrapper">

    <!-- LOADING -->
    <div v-if="loading" class="state-center">
      <div class="spinner"></div>
      <p class="state-msg">{{ DIET_MESSAGES.LOADING }}</p>
    </div>

    <!-- ERRO -->
    <div v-else-if="erro" class="state-center">
      <div class="error-card">
        <div class="error-icon"></div>
        <p class="error-text">{{ erro }}</p>
      </div>
    </div>

    <!-- GERANDO NOVA DIETA — only mounted while isGenerating (non-terminal); failed:false is always
         correct here because FAILED status sets isTerminal=true, which makes isGenerating=false. -->
    <DietGenerationProgress
      v-else-if="isGenerating"
      :message="generationMessage"
      :failed="false"
    />

    <!-- VAZIO: sem recomendação — três sub-estados -->
    <div v-else-if="!recomendacao" class="state-center">

      <!-- Erro ao carregar status de onboarding: estado seguro, sem CTA enganoso -->
      <div v-if="erroOnboarding" class="error-card">
        <div class="error-icon"></div>
        <p class="error-text">{{ erroOnboarding }}</p>
      </div>

      <!-- (A) Precisa completar triagem -->
      <div v-else-if="precisaTriagem === true" class="empty-card">
        <div class="empty-icon"></div>
        <p class="empty-title">{{ DIET_MESSAGES.EMPTY }}</p>
        <p class="empty-sub">{{ DIET_MESSAGES.COMPLETE_ASSESSMENT }}</p>
        <button
          type="button"
          class="regen-btn regen-btn--first"
          @click="router.push({ name: 'triagem' })"
        >
          Ir para a triagem
        </button>
      </div>

      <!-- (B)/(C) Onboarding completo, pronto para gerar (ou falhou na tentativa anterior) -->
      <div v-else-if="precisaTriagem === false" class="empty-card">
        <div class="empty-icon"></div>
        <p class="empty-title">{{ generationFailed ? DIET_MESSAGES.GENERATION_ERROR : DIET_MESSAGES.READY_TITLE }}</p>
        <p class="empty-sub">{{ generationFailed ? DIET_MESSAGES.GENERATION_FIRST_ERROR_RETRY : DIET_MESSAGES.READY_SUBTITLE }}</p>
        <button
          type="button"
          class="regen-btn regen-btn--first"
          :disabled="isGenerating"
          @click="startGeneration"
        >
          {{ generationFailed ? 'Tentar novamente' : DIET_MESSAGES.GENERATE_FIRST }}
        </button>
      </div>

      <!-- precisaTriagem ainda é null: loading do dashboard (não deve aparecer pois loading=true nesse ponto,
           mas garante que não renderizamos nada enganoso caso o estado seja inesperado) -->
      <div v-else class="empty-card">
        <div class="empty-icon"></div>
        <p class="empty-title">{{ DIET_MESSAGES.EMPTY }}</p>
      </div>

    </div>

    <!-- CONTEÚDO REAL -->
    <main v-else class="dieta-content">

      <!-- Error notice when generation failed or when the new recommendation could not be fetched
           after a successful job. In both cases the previous diet remains visible below. -->
      <div v-if="generationFailed || postGenFetchError" class="regen-error-notice">
        <span class="regen-error-text">
          {{ postGenFetchError ? DIET_MESSAGES.LOAD_ERROR : (generationError ?? DIET_MESSAGES.GENERATION_ERROR_RETRY) }}
        </span>
      </div>

      <!-- Cabeçalho -->
      <header class="dieta-intro">
        <div class="dieta-intro-top">
          <span class="gen-date">Plano gerado em: {{ formatDate(recomendacao.createdAt) }}</span>
          <span v-if="!route.query.rid" class="current-diet-badge">Dieta atual</span>
        </div>
        <h1>Seu plano alimentar personalizado</h1>
        <div class="summary-block">
          <p class="intro-desc" :class="{ clamped: isClamped('summary', summaryText(recomendacao)) }">
            {{ summaryText(recomendacao) }}
          </p>
          <button
            v-if="isLongText(summaryText(recomendacao))"
            type="button"
            class="summary-toggle"
            :aria-expanded="isExpanded('summary')"
            @click="toggleBlock('summary')"
          >
            {{ isExpanded('summary') ? 'Ver menos' : 'Ver mais' }}
          </button>
        </div>
      </header>

      <!-- Alertas -->
      <section v-if="(recomendacao.alerts ?? []).length" class="alerts-section">
        <div v-for="(alert, i) in recomendacao.alerts" :key="i" class="alert-item">
          <span class="alert-dot"></span>
          <p>{{ alert }}</p>
        </div>
      </section>

      <!-- Meta calórica diária -->
      <div class="daily-target">
        <span class="daily-label">Meta calórica diária</span>
        <span class="daily-value">{{ recomendacao.plan.dailyCalorieTarget }} kcal</span>
      </div>

      <div class="dieta-grid-layout">

        <!-- Coluna Esquerda: Refeições -->
        <section class="meals-column">
          <article
            v-for="(meal, mealIndex) in recomendacao.plan.meals"
            :key="mealKey(meal, mealIndex)"
            class="meal-card-item"
          >
            <div class="meal-card-header">
              <div class="meal-title-group">
                <span class="meal-type">{{ mealLabel(meal.mealType) }}</span>
                <div class="meal-kcal-group">
                  <span class="meal-kcal">Meta: {{ meal.calorieTarget }} kcal</span>
                  <span
                    v-if="selectedOption(meal, mealIndex)?.totalCalories != null"
                    class="meal-kcal meal-kcal-option"
                  >
                    Opção: {{ selectedOption(meal, mealIndex)?.totalCalories }} kcal
                  </span>
                </div>
              </div>

              <!-- Alternância de opções: só exibe se houver mais de uma opção real -->
              <div v-if="meal.options.length > 1" class="option-switcher">
                <button
                  v-for="(opt, optIdx) in meal.options"
                  :key="optIdx"
                  class="option-btn"
                  :class="{ active: activeOptionIndex(meal, mealIndex) === optIdx }"
                  @click="selectOption(meal, mealIndex, optIdx)"
                >
                  Opção {{ optIdx + 1 }}
                </button>
              </div>
            </div>

            <ul class="meal-items-list">
              <li
                v-for="item in selectedOption(meal, mealIndex)?.items"
                :key="item.foodId"
                class="meal-item-row"
              >
                <span class="meal-item-name">{{ item.displayName || item.name }}</span>
                <span class="meal-item-portion">{{ item.portionGrams }}g</span>
              </li>
            </ul>

            <div class="meal-footer">
              <div class="macros-display">
                <span class="macro-badge macro-prot">
                  <span class="macro-label">PROT</span>
                  <span class="macro-val">{{ mealMacros(meal, mealIndex).prot }}</span>
                </span>
                <span class="macro-badge macro-carb">
                  <span class="macro-label">CARB</span>
                  <span class="macro-val">{{ mealMacros(meal, mealIndex).carb }}</span>
                </span>
                <span class="macro-badge macro-gord">
                  <span class="macro-label">GORD</span>
                  <span class="macro-val">{{ mealMacros(meal, mealIndex).gord }}</span>
                </span>
              </div>
            </div>
          </article>
        </section>

        <!-- Coluna Direita: Dicas e Justificativas -->
        <aside class="ai-justification-column">

          <!-- Dicas da IA -->
          <section v-if="(recomendacao.tips ?? []).length" class="ai-strategy-card">
            <div class="card-header">
              <h3>Dicas personalizadas</h3>
            </div>
            <div class="strategy-content">
              <ul>
                <li v-for="(tip, i) in recomendacao.tips" :key="i" class="tip-item">
                  <span class="tip-text" :class="{ clamped: isClamped('tip-' + i, tip) }">{{ tip }}</span>
                  <button
                    v-if="isLongText(tip)"
                    type="button"
                    class="summary-toggle"
                    :aria-expanded="isExpanded('tip-' + i)"
                    @click="toggleBlock('tip-' + i)"
                  >
                    {{ isExpanded('tip-' + i) ? 'Ver menos' : 'Ver mais' }}
                  </button>
                </li>
              </ul>
            </div>
          </section>

          <!-- Explicações por refeição -->
          <section
            v-if="Object.keys(recomendacao.mealExplanations ?? {}).length"
            class="profile-notes-card"
          >
            <h3>Justificativa por refeição</h3>
            <div class="notes-list">
              <div
                v-for="(explanation, type) in recomendacao.mealExplanations"
                :key="type"
                class="note-item"
              >
                <span class="note-dot"></span>
                <div class="note-body">
                  <p :class="{ clamped: isClamped('expl-' + type, explanation) }">
                    <strong>{{ mealLabel(String(type)) }}:</strong> {{ explanation }}
                  </p>
                  <button
                    v-if="isLongText(explanation)"
                    type="button"
                    class="summary-toggle"
                    :aria-expanded="isExpanded('expl-' + type)"
                    @click="toggleBlock('expl-' + type)"
                  >
                    {{ isExpanded('expl-' + type) ? 'Ver menos' : 'Ver mais' }}
                  </button>
                </div>
              </div>
            </div>
          </section>

          <!-- Feedback -->
          <RecommendationFeedbackForm
            :recommendation-id="recomendacao.id"
            :recommendation-status="recomendacao.status"
          />

        </aside>
      </div>

      <!-- Ação de gerar nova dieta: posicionada após o feedback, seguindo o fluxo
           ver dieta → avaliar → decidir gerar nova. CTA com hierarquia destacada. -->
      <div class="regen-footer">
        <div class="regen-cta">
          <div class="regen-cta-copy">
            <span class="regen-cta-eyebrow">Não combinou com você?</span>
            <p class="regen-cta-text">
              Gere um novo plano alimentar com base na sua triagem atual.
            </p>
          </div>
          <button
            type="button"
            class="regen-cta-btn"
            :disabled="isGenerating"
            @click="regenerate"
          >
            <span class="regen-cta-btn-icon" aria-hidden="true"></span>
            {{ isGenerating ? 'Gerando…' : 'Gerar nova dieta' }}
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.dieta-page-wrapper {
  --bg-deep: #0f172a;
  --bg-card: #1e293b;
  --accent: #10b981;
  --text-muted: #94a3b8;

  min-height: 100vh;
  background-color: var(--bg-deep);
  color: white;
  font-family: 'Inter', sans-serif;
}

/* ESTADOS */
.state-center {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem;
}
.state-msg { color: var(--text-muted); font-size: 1.1rem; font-weight: 600; margin: 0; }

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
.error-icon { width: 40px; height: 40px; background: #fca5a5; mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2L2 22h20L12 2zm0 3.75L19.5 20H4.5L12 5.75zM11 10h2v5h-2v-5zm0 6h2v2h-2v-2z'/%3E%3C/svg%3E") no-repeat center; -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2L2 22h20L12 2zm0 3.75L19.5 20H4.5L12 5.75zM11 10h2v5h-2v-5zm0 6h2v2h-2v-2z'/%3E%3C/svg%3E") no-repeat center; }
.error-text { color: #fca5a5; font-size: 1rem; font-weight: 600; margin: 0; line-height: 1.5; }

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
.empty-icon { width: 48px; height: 48px; background: var(--accent); mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M20 3h-2.19c-.45-1.18-1.58-2-2.91-2h-5.8c-1.33 0-2.46.82-2.91 2H4c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3.5 10H7.5c-1.38 0-2.5 1.12-2.5 2.5v5.5c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2v-5.5c0-1.38-1.12-2.5-2.5-2.5z'/%3E%3C/svg%3E") no-repeat center; -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M20 3h-2.19c-.45-1.18-1.58-2-2.91-2h-5.8c-1.33 0-2.46.82-2.91 2H4c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3.5 10H7.5c-1.38 0-2.5 1.12-2.5 2.5v5.5c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2v-5.5c0-1.38-1.12-2.5-2.5-2.5z'/%3E%3C/svg%3E") no-repeat center; margin-bottom: 0.25rem; }
.empty-title { color: white; font-size: 1.15rem; font-weight: 800; margin: 0; }
.empty-sub { color: var(--text-muted); font-size: 0.9rem; margin: 0; line-height: 1.5; }

/* CONTEÚDO */
.dieta-content { max-width: 1200px; margin: 0 auto; padding: 4rem 1.5rem; }

.dieta-intro { margin-bottom: 2.5rem; }
.dieta-intro-top { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.gen-date { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
.current-diet-badge {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(16, 185, 129, 0.12);
  color: var(--accent);
  border: 1px solid rgba(16, 185, 129, 0.25);
}
.dieta-intro h1 { font-size: 2.5rem; font-weight: 900; margin: 0.8rem 0 0.5rem; letter-spacing: -1px; line-height: 1.1; }

/* SUMMARY */
.summary-block { display: flex; flex-direction: column; align-items: flex-start; gap: 0.4rem; }
.intro-desc {
  color: var(--text-muted);
  font-size: 1rem;
  max-width: 720px;
  line-height: 1.6;
  margin: 0;
}
.intro-desc.clamped {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.summary-toggle {
  background: none;
  border: none;
  color: var(--accent);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.summary-toggle:hover { opacity: 0.8; }

/* ALERTAS */
.alerts-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 2rem;
  background: rgba(248, 113, 113, 0.05);
  border: 1px solid rgba(248, 113, 113, 0.2);
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
}
.alert-item { display: flex; gap: 10px; align-items: flex-start; }
.alert-dot { width: 6px; height: 6px; background: #f87171; border-radius: 50%; margin-top: 7px; flex-shrink: 0; }
.alert-item p { font-size: 0.9rem; color: #fca5a5; margin: 0; line-height: 1.5; }

/* META CALÓRICA */
.daily-target {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2.5rem;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 14px;
  padding: 1rem 1.5rem;
}
.daily-label { font-size: 0.8rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }
.daily-value { font-size: 1.3rem; font-weight: 900; color: var(--accent); }

/* GRID LAYOUT */
.dieta-grid-layout { display: grid; grid-template-columns: 1fr 360px; gap: 3rem; }

/* CARDS DE REFEIÇÃO */
.meals-column { display: flex; flex-direction: column; gap: 1.25rem; }
.meal-card-item {
  background: var(--bg-card);
  padding: 1.5rem 2rem;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.03);
}
.meal-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 0.75rem; }
.meal-type { font-size: 1.1rem; font-weight: 800; display: block; }

/* Badges de kcal no cabeçalho */
.meal-kcal-group { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 4px; }
.meal-kcal {
  font-size: 0.78rem;
  color: var(--text-muted);
  font-weight: 600;
  background: rgba(255,255,255,0.05);
  padding: 3px 9px;
  border-radius: 8px;
  display: inline-block;
}
.meal-kcal-option {
  color: var(--accent);
  background: rgba(16, 185, 129, 0.1);
}

/* Alternância de opções */
.option-switcher { display: flex; gap: 0.4rem; flex-shrink: 0; }
.option-btn {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  letter-spacing: 0.3px;
}
.option-btn:hover { background: rgba(16,185,129,0.1); color: var(--accent); border-color: rgba(16,185,129,0.3); }
.option-btn.active { background: rgba(16,185,129,0.15); color: var(--accent); border-color: rgba(16,185,129,0.5); }

/* Lista de itens de refeição */
.meal-items-list {
  list-style: none;
  padding: 0;
  margin: 0 0 1.25rem;
  display: flex;
  flex-direction: column;
}
.meal-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.45rem 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  gap: 0.5rem;
}
.meal-item-row:last-child { border-bottom: none; }
.meal-item-name { font-size: 0.9rem; color: #cbd5e1; line-height: 1.4; }
.meal-item-portion { font-size: 0.8rem; color: var(--text-muted); font-weight: 600; white-space: nowrap; flex-shrink: 0; }

/* Rodapé do card */
.meal-footer { display: flex; align-items: center; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 1rem; }

/* Badges de macros */
.macros-display { display: flex; gap: 0.6rem; flex-wrap: wrap; }
.macro-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 0.78rem;
  font-weight: 700;
}
.macro-label { font-size: 0.65rem; font-weight: 800; letter-spacing: 0.5px; opacity: 0.75; }
.macro-val { font-size: 0.85rem; }

.macro-prot {
  background: rgba(96, 165, 250, 0.12);
  color: #60a5fa;
}
.macro-carb {
  background: rgba(251, 191, 36, 0.12);
  color: #fbbf24;
}
.macro-gord {
  background: rgba(251, 146, 60, 0.12);
  color: #fb923c;
}

/* SIDEBAR IA */
.ai-justification-column { display: flex; flex-direction: column; gap: 1.5rem; }

.ai-strategy-card {
  background: linear-gradient(145deg, #1e293b 0%, #161e2b 100%);
  padding: 1.5rem; border-radius: 20px; border: 1px solid rgba(16, 185, 129, 0.2);
}
.ai-strategy-card .card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 1.25rem; }
.ai-strategy-card h3 {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--accent);
  font-weight: 800;
  margin: 0;
}

.strategy-content ul { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.9rem; }
.tip-item {
  font-size: 0.875rem;
  color: #cbd5e1;
  line-height: 1.5;
  padding-left: 1.2rem;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.3rem;
}
.tip-item::before { content: "•"; position: absolute; left: 0; color: var(--accent); font-weight: bold; }
.tip-text.clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.profile-notes-card { background: rgba(255,255,255,0.02); padding: 1.5rem; border-radius: 20px; border: 1px solid rgba(255,255,255,0.05); }
.profile-notes-card h3 { font-size: 0.8rem; font-weight: 800; margin: 0 0 1.25rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }

.notes-list { display: flex; flex-direction: column; gap: 1rem; }
.note-item { display: flex; gap: 10px; align-items: flex-start; }
.note-dot { width: 6px; height: 6px; background: var(--text-muted); border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.note-body { display: flex; flex-direction: column; align-items: flex-start; gap: 0.3rem; flex: 1; min-width: 0; }
.note-item p {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.5;
  margin: 0;
}
.note-item p.clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.note-item strong { color: #cbd5e1; }

/* REGENERAR DIETA */
.regen-footer {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* CTA destacado de "gerar nova dieta": copy à esquerda, ação primária à direita. */
.regen-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.09), rgba(16, 185, 129, 0.02));
  border: 1px solid rgba(16, 185, 129, 0.18);
  border-radius: 18px;
  padding: 1.4rem 1.6rem;
}

.regen-cta-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.regen-cta-eyebrow {
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  color: var(--accent);
}

.regen-cta-text {
  margin: 0;
  font-size: 0.92rem;
  color: #cbd5e1;
  line-height: 1.45;
}

.regen-cta-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  background: var(--accent);
  color: var(--bg-deep);
  border: none;
  border-radius: 12px;
  padding: 0.85rem 1.6rem;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.2px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  box-shadow: 0 8px 20px -6px rgba(16, 185, 129, 0.5);
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.regen-cta-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  background: #14c98e;
  box-shadow: 0 12px 26px -6px rgba(16, 185, 129, 0.6);
}

.regen-cta-btn:active:not(:disabled) {
  transform: translateY(0);
}

.regen-cta-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* Ícone "recriar": arco circular aberto com seta, gira no hover. */
.regen-cta-btn-icon {
  position: relative;
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
}

.regen-cta-btn-icon::after {
  content: '';
  position: absolute;
  top: -2px;
  right: -1px;
  width: 5px;
  height: 5px;
  border-top: 2px solid currentColor;
  border-right: 2px solid currentColor;
  transform: rotate(45deg);
}

.regen-cta-btn:hover:not(:disabled) .regen-cta-btn-icon {
  animation: regen-spin 0.6s ease;
}

@keyframes regen-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 560px) {
  .regen-cta {
    flex-direction: column;
    align-items: stretch;
  }
  .regen-cta-btn {
    justify-content: center;
  }
}

.regen-btn {
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.4);
  color: #10b981;
  font-size: 0.82rem;
  font-weight: 700;
  padding: 0.5rem 1.25rem;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  letter-spacing: 0.3px;
}

.regen-btn:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.7);
}

.regen-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Variante usada no estado vazio (sem dieta ainda) */
.regen-btn--first {
  margin-top: 1rem;
}

.regen-error-notice {
  background: rgba(248, 113, 113, 0.06);
  border: 1px solid rgba(248, 113, 113, 0.2);
  border-radius: 12px;
  padding: 0.9rem 1.25rem;
  margin-bottom: 1.5rem;
}

.regen-error-text {
  color: #fca5a5;
  font-size: 0.88rem;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .dieta-grid-layout { grid-template-columns: 1fr; }
}
</style>
