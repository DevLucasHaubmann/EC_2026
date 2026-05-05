<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { aiService } from '../../services/modules/aiRecommendation';
import type { Recommendation } from '@/types/api';

const loading = ref(true);
const erro = ref<string | null>(null);
const recomendacao = ref<Recommendation.Response | null>(null);
const summaryExpanded = ref(false);

const MEAL_LABELS: Record<string, string> = {
  BREAKFAST:        'Café da manhã',
  MORNING_SNACK:    'Lanche da manhã',
  LUNCH:            'Almoço',
  AFTERNOON_SNACK:  'Lanche da tarde',
  DINNER:           'Jantar',
  SUPPER:           'Ceia',
};

function mealLabel(type: string): string {
  return MEAL_LABELS[type] ?? type;
}

function mealAlimentos(meal: Recommendation.MealPlanMeal): string {
  const opt = meal.options[0];
  if (!opt || opt.items.length === 0) return '—';
  return opt.items.map(i => i.displayName || i.name).join(', ');
}

function mealMacros(meal: Recommendation.MealPlanMeal) {
  const opt = meal.options[0];
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

onMounted(async () => {
  try {
    recomendacao.value = await aiService.getLatest();
    if (recomendacao.value?.status === 'GENERATED') {
      aiService.markAsViewed(recomendacao.value.id).catch(() => {});
    }
  } catch (e: any) {
    const status = e?.response?.status;
    if (status === 404) {
      recomendacao.value = null;
    } else {
      erro.value = 'Não foi possível carregar sua dieta. Tente novamente.';
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
      <p class="state-msg">Carregando sua dieta...</p>
    </div>

    <!-- ERRO -->
    <div v-else-if="erro" class="state-center">
      <p class="state-msg state-error">{{ erro }}</p>
    </div>

    <!-- VAZIO -->
    <div v-else-if="!recomendacao" class="state-center">
      <p class="state-msg">Nenhuma recomendação encontrada.</p>
      <p class="state-sub">Complete a triagem para gerar seu plano alimentar personalizado.</p>
    </div>

    <!-- CONTEÚDO REAL -->
    <main v-else class="dieta-content">

      <!-- Cabeçalho -->
      <header class="dieta-intro">
        <span class="gen-date">Plano gerado em: {{ formatDate(recomendacao.createdAt) }}</span>
        <h1>Seu plano alimentar personalizado</h1>
        <div class="summary-block">
          <p class="intro-desc" :class="{ clamped: !summaryExpanded }">
            {{ summaryText(recomendacao) }}
          </p>
          <button
            v-if="summaryText(recomendacao).length > 120"
            class="summary-toggle"
            @click="summaryExpanded = !summaryExpanded"
          >
            {{ summaryExpanded ? 'Ver menos' : 'Ver mais' }}
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
            v-for="meal in recomendacao.plan.meals"
            :key="meal.mealType"
            class="meal-card-item"
          >
            <div class="meal-card-header">
              <div class="meal-title-group">
                <span class="meal-type">{{ mealLabel(meal.mealType) }}</span>
                <span class="meal-kcal">{{ meal.calorieTarget }} kcal</span>
              </div>
            </div>

            <p class="meal-description">{{ mealAlimentos(meal) }}</p>

            <div class="meal-footer">
              <div class="macros-display">
                <span>{{ mealMacros(meal).prot }} prot</span>
                <span>{{ mealMacros(meal).carb }} carb</span>
                <span>{{ mealMacros(meal).gord }} gord</span>
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
                <li v-for="(tip, i) in recomendacao.tips" :key="i" class="tip-item">{{ tip }}</li>
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
                <p><strong>{{ mealLabel(String(type)) }}:</strong> {{ explanation }}</p>
              </div>
            </div>
          </section>

        </aside>
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
.state-sub { color: var(--text-muted); font-size: 0.9rem; margin: 0; }
.state-error { color: #f87171; }

/* CONTEÚDO */
.dieta-content { max-width: 1200px; margin: 0 auto; padding: 4rem 1.5rem; }

.dieta-intro { margin-bottom: 2.5rem; }
.gen-date { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
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
.meal-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.meal-type { font-size: 1.1rem; font-weight: 800; display: block; }
.meal-kcal { font-size: 0.8rem; color: var(--text-muted); font-weight: 600; background: rgba(255,255,255,0.05); padding: 3px 9px; border-radius: 8px; margin-top: 3px; display: inline-block; }
.meal-description { font-size: 0.95rem; line-height: 1.5; color: #cbd5e1; margin-bottom: 1.25rem; }
.meal-footer { display: flex; align-items: center; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 1rem; }
.macros-display { display: flex; gap: 1.2rem; }
.macros-display span { font-size: 0.85rem; color: var(--text-muted); font-weight: 600; }

/* SIDEBAR IA */
.ai-justification-column { display: flex; flex-direction: column; gap: 1.5rem; }

.ai-strategy-card {
  background: linear-gradient(145deg, #1e293b 0%, #161e2b 100%);
  padding: 1.5rem; border-radius: 20px; border: 1px solid rgba(16, 185, 129, 0.2);
}
.ai-strategy-card .card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 1.25rem; }
.ai-strategy-card h3 { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 1.5px; color: var(--accent); font-weight: 800; margin: 0; }

.strategy-content ul { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.9rem; }
.tip-item {
  font-size: 0.875rem;
  color: #cbd5e1;
  line-height: 1.5;
  padding-left: 1.2rem;
  position: relative;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tip-item::before { content: "•"; position: absolute; left: 0; color: var(--accent); font-weight: bold; }

.profile-notes-card { background: rgba(255,255,255,0.02); padding: 1.5rem; border-radius: 20px; border: 1px solid rgba(255,255,255,0.05); }
.profile-notes-card h3 { font-size: 0.8rem; font-weight: 800; margin: 0 0 1.25rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }

.notes-list { display: flex; flex-direction: column; gap: 1rem; }
.note-item { display: flex; gap: 10px; align-items: flex-start; }
.note-dot { width: 6px; height: 6px; background: var(--text-muted); border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.note-item p {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.note-item strong { color: #cbd5e1; }

@media (max-width: 1024px) {
  .dieta-grid-layout { grid-template-columns: 1fr; }
  .ai-justification-column { order: -1; }
}
</style>
