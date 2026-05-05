<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

// RF014: Histórico de peso para o gráfico (Tela 07 do PDF)
const historicoPeso = ref([
  { data: 'Jan', peso: 88.0 },
  { data: 'Fev', peso: 86.5 },
  { data: 'Mar', peso: 85.8 },
  { data: 'Abr', peso: 84.2 },
  { data: 'Mai', peso: 83.5 },
]);

// Analytics de Aderência Semanal (Página 15 do PDF)
const aderenciaSemanal = ref([
  { dia: 'Seg', pct: 72 },
  { dia: 'Ter', pct: 84 },
  { dia: 'Qua', pct: 79 },
  { dia: 'Qui', pct: 90 },
  { dia: 'Sex', pct: 86 },
  { dia: 'Sáb', pct: 76 },
  { dia: 'Dom', pct: 88 },
]);

const pesoInicial = historicoPeso.value[0]!.peso;
const pesoAtual = historicoPeso.value[historicoPeso.value.length - 1]!.peso;
const diferencaPeso = (pesoAtual - pesoInicial).toFixed(1);
const novoPeso = ref(null);

const registrarPeso = () => {
  if (novoPeso.value) {
    const dataAtual = new Date().toLocaleDateString('pt-BR', { month: 'short' });
    historicoPeso.value.push({ data: dataAtual, peso: novoPeso.value });
    novoPeso.value = null;
  }
};

const voltar = () => router.back();
const logout = () => authStore.logout();
</script>

<template>
  <div class="evolucao-page">
    <!-- NAVBAR PADRONIZADA -->
    <nav class="standard-nav">
      <button class="btn-back" @click="voltar">
        <span class="chevron-left"></span>
        Voltar
      </button>
      <span class="brand-logo">Tukan <span></span></span>
      <button class="btn-logout" @click="logout">Sair</button>
    </nav>

    <main class="evolucao-content">
      <!-- Cabeçalho (Tela 07) -->
      <header class="page-header">
        <span class="category-tag">Analytics & Performance</span>
        <h1>Progresso e acompanhamento</h1>
        <p>Análise detalhada da sua evolução corporal e aderência ao plano alimentar.</p>
      </header>

      <!-- Cards de Resumo Rápido -->
      <section class="summary-grid">
        <div class="summary-card">
          <label>Peso Inicial</label>
          <p>{{ pesoInicial }} <span>kg</span></p>
        </div>
        <div class="summary-card highlight">
          <label>Variação Total</label>
          <p>{{ diferencaPeso }} <span>kg</span></p>
        </div>
        <div class="summary-card">
          <label>Peso Atual</label>
          <p>{{ pesoAtual }} <span>kg</span></p>
        </div>
      </section>

      <!-- Grade de Analytics (Gráficos) -->
      <div class="analytics-grid">
        
        <!-- Gráfico de Peso (RF017 - Evolução) -->
        <section class="chart-card">
          <div class="card-header">
            <h3>Peso ao longo dos meses</h3>
            <span class="trend-up">Tendência de queda</span>
          </div>
          <div class="weight-chart-visual">
            <div 
              v-for="(item, i) in historicoPeso" :key="i" 
              class="chart-bar-container"
            >
              <div class="bar-fill" :style="{ height: (item.peso / 100 * 150) + 'px' }">
                <span class="bar-tooltip">{{ item.peso }}kg</span>
              </div>
              <span class="bar-date">{{ item.data }}</span>
            </div>
          </div>
        </section>

        <!-- Gráfico de Aderência (Página 15 do PDF) -->
        <section class="chart-card">
          <div class="card-header">
            <h3>Aderência Semanal</h3>
            <span class="pct-avg">82% Média</span>
          </div>
          <div class="adherence-list">
            <div v-for="dia in aderenciaSemanal" :key="dia.dia" class="adherence-item">
              <span class="day-label">{{ dia.dia }}</span>
              <div class="adherence-bar-bg">
                <div class="adherence-bar-fill" :style="{ width: dia.pct + '%' }"></div>
              </div>
              <span class="pct-label">{{ dia.pct }}%</span>
            </div>
          </div>
        </section>

      </div>

      <!-- Footer Analytics (RF014 e RF018) -->
      <section class="analytics-footer">
        <div class="footer-stat">
          <label>Calorias médias</label>
          <strong>1.860 kcal</strong>
        </div>
        <div class="footer-stat">
          <label>Meta atingida</label>
          <strong>5 dias / semana</strong>
        </div>
        <div class="footer-stat">
          <label>Ritmo atual</label>
          <strong class="accent-text">Consistente</strong>
        </div>
        
        <!-- Registro de Novo Peso (RF014) -->
        <div class="weight-input-box">
          <input v-model="novoPeso" type="number" step="0.1" placeholder="Novo peso (kg)" />
          <button @click="registrarPeso">Registrar</button>
        </div>
      </section>

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

/* NAVBAR PADRONIZADA */
.standard-nav {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.2rem 5%; background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(12px); border-bottom: 1px solid rgba(255,255,255,0.05);
  position: sticky; top: 0; z-index: 100;
}
.btn-back { background: transparent; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 8px; font-weight: 600; }
.btn-back:hover { color: var(--accent); }
.chevron-left { width: 8px; height: 8px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
.brand-logo { font-weight: 800; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem; }
.brand-logo span { color: var(--accent); font-weight: 400; }
.btn-logout { background: rgba(239, 68, 68, 0.1); color: #f87171; border: none; padding: 0.5rem 1.2rem; border-radius: 10px; font-weight: 700; cursor: pointer; }

/* CONTEÚDO */
.evolucao-content { max-width: 1100px; margin: 0 auto; padding: 4rem 1.5rem; }

.page-header { margin-bottom: 3.5rem; }
.category-tag { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
.page-header h1 { font-size: 2.5rem; font-weight: 900; margin: 0.5rem 0; letter-spacing: -1px; }
.page-header p { color: var(--text-muted); font-size: 1.1rem; }

/* SUMMARY CARDS */
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.5rem; margin-bottom: 2.5rem; }
.summary-card { background: var(--bg-card); padding: 2rem; border-radius: 24px; border: 1px solid rgba(255,255,255,0.03); }
.summary-card.highlight { border-color: var(--accent); background: rgba(16, 185, 129, 0.05); }
.summary-card label { display: block; font-size: 0.75rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-bottom: 0.5rem; }
.summary-card p { font-size: 1.8rem; font-weight: 800; }
.summary-card p span { font-size: 1rem; color: var(--accent); font-weight: 400; }

/* ANALYTICS GRID */
.analytics-grid { display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 2rem; margin-bottom: 2rem; }
.chart-card { background: var(--bg-card); padding: 2.5rem; border-radius: 28px; border: 1px solid rgba(255,255,255,0.05); }

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2.5rem; }
.card-header h3 { font-size: 1rem; font-weight: 800; }
.trend-up, .pct-avg { font-size: 0.75rem; font-weight: 700; color: var(--accent); background: rgba(16, 185, 129, 0.1); padding: 4px 10px; border-radius: 8px; }

/* WEIGHT CHART (Visual) */
.weight-chart-visual { height: 200px; display: flex; align-items: flex-end; justify-content: space-around; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 10px; }
.chart-bar-container { display: flex; flex-direction: column; align-items: center; flex: 1; }
.bar-fill { width: 30px; background: rgba(16, 185, 129, 0.2); border-radius: 6px 6px 0 0; position: relative; transition: height 0.5s ease; border-bottom: 2px solid var(--accent); }
.bar-fill:hover { background: var(--accent); }
.bar-tooltip { position: absolute; top: -25px; left: 50%; transform: translateX(-50%); font-size: 0.75rem; font-weight: 800; color: white; }
.bar-date { margin-top: 10px; font-size: 0.75rem; color: var(--text-muted); }

/* ADHERENCE LIST */
.adherence-list { display: flex; flex-direction: column; gap: 1rem; }
.adherence-item { display: flex; align-items: center; gap: 12px; }
.day-label { width: 35px; font-size: 0.8rem; font-weight: 700; color: var(--text-muted); }
.adherence-bar-bg { flex: 1; height: 8px; background: rgba(255,255,255,0.05); border-radius: 10px; overflow: hidden; }
.adherence-bar-fill { height: 100%; background: var(--accent); border-radius: 10px; }
.pct-label { width: 40px; font-size: 0.8rem; font-weight: 700; text-align: right; }

/* FOOTER ANALYTICS */
.analytics-footer { background: var(--bg-card); padding: 2rem 3rem; border-radius: 28px; display: flex; justify-content: space-between; align-items: center; border: 1px solid rgba(255,255,255,0.05); }
.footer-stat label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
.footer-stat strong { font-size: 1.1rem; font-weight: 800; }
.accent-text { color: var(--accent); }

.weight-input-box { display: flex; gap: 10px; }
.weight-input-box input { background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(255,255,255,0.1); padding: 0.7rem; border-radius: 10px; color: white; width: 120px; }
.weight-input-box button { background: var(--accent); border: none; padding: 0.7rem 1.2rem; border-radius: 10px; font-weight: 800; color: var(--bg-deep); cursor: pointer; }

@media (max-width: 900px) {
  .analytics-grid { grid-template-columns: 1fr; }
  .summary-grid { grid-template-columns: 1fr; }
  .analytics-footer { flex-direction: column; gap: 2rem; text-align: center; }
}
</style>