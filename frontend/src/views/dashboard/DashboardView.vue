<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { dashboardService } from '../../services/modules/dashboard';
import type { Dashboard } from '@/types/api';

const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const erro = ref<string | null>(null);
const dados = ref<Dashboard.Response | null>(null);

const navegacao = [
  { nome: 'Efetivar Refeição', path: '/efetivarRefeicao', desc: 'Marque aqui suas refeições' },
  { nome: 'Minha Dieta', path: '/dieta', desc: 'Veja seu plano alimentar' },
  { nome: 'Triagem', path: '/triagem', desc: 'Atualize seus dados' },
  { nome: 'Evolução', path: '/evolucao', desc: 'Gráficos de progresso' },
  { nome: 'Perfil', path: '/perfil', desc: 'Configurações da conta' },
];

const irPara = (path: string) => router.push(path);

// Converte o nextStep do backend para a rota correta do frontend
function resolveOnboardingRoute(nextStep: string) {
  if (nextStep === '/profiles/first-access') return { name: 'triagem' }
  if (nextStep === '/assessments') return { name: 'triagem' }
  return null
}

const GOAL_LABELS: Record<Dashboard.NutritionalGoal, string> = {
  WEIGHT_LOSS: 'Perda de peso',
  MUSCLE_GAIN: 'Ganho de massa',
  MAINTENANCE: 'Manutenção',
  DIETARY_REEDUCATION: 'Reeducação alimentar',
  SPORTS_PERFORMANCE: 'Performance esportiva',
}

const ACTIVITY_LABELS: Record<Dashboard.ActivityLevel, string> = {
  SEDENTARY: 'Sedentário',
  MODERATE: 'Moderado',
  INTENSE: 'Intenso',
}

// Converte IMC em porcentagem visual para a barra de progresso (faixa saudável = 100%)
const bmiProgressWidth = computed(() => {
  const bmi = dados.value?.profile?.bmi
  if (!bmi) return '0%'
  // Normaliza: 40+ = 100%, escala linear
  return Math.min(Math.round((bmi / 40) * 100), 100) + '%'
})

onMounted(async () => {
  try {
    const res = await dashboardService.getDashboard()

    // Guard de onboarding: se ainda não completou, redireciona
    if (res.onboarding.onboardingRequired) {
      const destino = resolveOnboardingRoute(res.onboarding.nextStep)
      if (destino) {
        router.replace(destino)
        return
      }
    }

    dados.value = res
  } catch {
    erro.value = 'Não foi possível carregar seus dados. Tente novamente.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="dashboard-wrapper">
    <div class="dashboard-container">

      <!-- Estado: carregando -->
      <div v-if="loading" class="state-center">
        <p class="state-msg">Carregando seus dados...</p>
      </div>

      <!-- Estado: erro -->
      <div v-else-if="erro" class="state-center">
        <p class="state-msg state-error">{{ erro }}</p>
        <button class="btn-action btn-outline" style="margin-top: 1rem" @click="irPara('/auth')">
          Voltar ao início
        </button>
      </div>

      <!-- Conteúdo real -->
      <template v-else-if="dados">

        <!-- Cabeçalho Principal -->
        <header class="dashboard-header">
          <div class="header-left">
            <span class="context-tag">Visão Geral</span>
            <h1>Olá, {{ dados.name }}</h1>
            <p class="subtitle">Bem-vindo ao <strong>Tukan</strong>. Vamos manter o foco hoje?</p>
          </div>

        </header>

        <!-- Seção de Perfil Nutricional (substitui card de kcal — dado não disponível na API) -->
        <section class="performance-section">
          <article class="stats-card">
            <div class="stats-header">
              <div class="stats-title">
                <h3>Índice de Massa Corporal</h3>
                <p>
                  Objetivo: {{ dados.assessment ? GOAL_LABELS[dados.assessment.goal] : '—' }}
                </p>
              </div>
              <div class="stats-numbers">
                <span class="current">{{ dados.profile?.bmi ?? '—' }}</span>
                <span class="separator">/</span>
                <span class="total">{{ dados.profile?.bmiClassification ?? '—' }}</span>
              </div>
            </div>

            <div class="progress-container">
              <div class="progress-fill" :style="{ width: bmiProgressWidth }"></div>
            </div>
          </article>
        </section>

        <!-- Grade de Navegação (Menu Grid) -->
        <nav class="menu-grid">
          <article
            v-for="item in navegacao"
            :key="item.path"
            class="menu-card"
            @click="irPara(item.path)"
          >
            <div class="card-icon-box">
              <span class="icon-placeholder"></span>
            </div>
            <div class="card-content">
              <h3>{{ item.nome }}</h3>
              <p>{{ item.desc }}</p>
            </div>
            <div class="card-arrow">
              <span class="chevron"></span>
            </div>
          </article>
        </nav>

        <!-- Rodapé Administrativo -->
        <footer v-if="authStore.isAuthenticated" class="admin-footer">
          <button class="btn-admin-link" @click="irPara('/admin/dashboard')">
            Acessar Painel de Controle Admin
          </button>
        </footer>

      </template>

    </div>
  </main>
</template>

<style scoped>
/* Configurações de Cores e Estilo Tukan */
.dashboard-wrapper {
  --bg-deep: #0f172a;
  --bg-card: #1e293b;
  --accent: #10b981;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --danger: #f87171;

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

/* Header */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 4rem;
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
  border: 1px solid rgba(255,255,255,0.1);
  color: white;
}

.btn-outline:hover {
  background: var(--bg-card);
  border-color: var(--accent);
}

.btn-danger-soft {
  background: rgba(248, 113, 113, 0.1);
  color: var(--danger);
  border: 1px solid rgba(248, 113, 113, 0.2);
}

.btn-danger-soft:hover {
  background: var(--danger);
  color: white;
}

/* Stats Card */
.performance-section {
  margin-bottom: 2.5rem;
}

.stats-card {
  background: var(--bg-card);
  padding: 2.5rem;
  border-radius: 28px;
  border: 1px solid rgba(255,255,255,0.03);
  box-shadow: 0 20px 40px rgba(0,0,0,0.25);
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.stats-title h3 {
  font-size: 1.2rem;
  font-weight: 800;
}

.stats-title p {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.stats-numbers {
  font-size: 2rem;
  font-weight: 900;
}

.stats-numbers .separator {
  margin: 0 0.4rem;
  color: var(--text-muted);
  font-weight: 300;
}

.stats-numbers .total {
  color: var(--text-muted);
}

.stats-numbers .total span {
  font-size: 1rem;
  color: var(--accent);
  font-weight: 500;
}

.progress-container {
  height: 10px;
  background: rgba(255,255,255,0.05);
  border-radius: 20px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), #34d399);
  border-radius: 20px;
  transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 15px rgba(16, 185, 129, 0.3);
}

/* Menu Grid */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.menu-card {
  background: var(--bg-card);
  padding: 2rem;
  border-radius: 24px;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255,255,255,0.02);
}

.menu-card:hover {
  transform: translateY(-5px);
  background: #243347;
  border-color: var(--accent);
}

.card-icon-box {
  width: 52px;
  height: 52px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}

.icon-placeholder {
  width: 12px;
  height: 12px;
  background: currentColor;
  border-radius: 3px;
}

.card-content h3 {
  font-size: 1.1rem;
  font-weight: 800;
  margin-bottom: 0.2rem;
}

.card-content p {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.4;
}

.chevron {
  width: 8px;
  height: 8px;
  border-top: 2px solid var(--text-muted);
  border-right: 2px solid var(--text-muted);
  transform: rotate(45deg);
  display: block;
  margin-left: auto;
}

.menu-card:hover .chevron {
  border-color: var(--accent);
}

/* Admin Footer */
.admin-footer {
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(255,255,255,0.05);
  text-align: center;
}

.btn-admin-link {
  background: transparent;
  color: var(--text-muted);
  border: 1px solid rgba(255,255,255,0.1);
  padding: 0.6rem 2rem;
  border-radius: 10px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-admin-link:hover {
  color: white;
  border-color: white;
}

/* Loading / Erro */
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

/* Mobile */
@media (max-width: 768px) {
  .menu-grid {
    grid-template-columns: 1fr;
  }
  
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }
}
</style>