<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

// RF013: Sugestões da IA vindas do Plano Semanal
const sugestoesIA = ref([
  { id: 101, nome: 'Wrap de frango', kcal: 320, prot: '25g', carb: '30g', tipo: 'Almoço' },
  { id: 102, nome: 'Bowl vegetariano', kcal: 410, prot: '15g', carb: '55g', tipo: 'Jantar' },
  { id: 103, nome: 'Shake proteico', kcal: 220, prot: '30g', carb: '10g', tipo: 'Lanche' },
]);

// RF015: Log de refeições efetivadas (consumidas)
const refeicoesConsumidas = ref([
  { id: 1, nome: 'Café da manhã', descricao: 'Iogurte com aveia', kcal: 420, horario: '08:10' },
]);

const totalKcal = computed(() => {
  return refeicoesConsumidas.value.reduce((acc, item) => acc + item.kcal, 0);
});

// Ações
const efetivarSugestao = (sugestao: any) => {
  const agora = new Date();
  const horario = `${agora.getHours()}:${agora.getMinutes().toString().padStart(2, '0')}`;
  refeicoesConsumidas.value.push({
    id: Date.now(),
    nome: sugestao.tipo,
    descricao: sugestao.nome,
    kcal: sugestao.kcal,
    horario: horario
  });
};

const removerRefeicao = (id: number) => {
  refeicoesConsumidas.value = refeicoesConsumidas.value.filter(r => r.id !== id);
};

const voltar = () => router.back();
const logout = () => authStore.logout();
</script>

<template>
  <div class="registro-wrapper">
    <!-- NAVBAR PADRONIZADA (Igual ao Dashboard/Perfil) -->
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

      <!-- Card de Saldo (Baseado na pág 14 do PDF) -->
      <section class="balance-highlight-card">
        <div class="balance-content">
          <label>Saldo Calórico do Dia</label>
          <h2 class="total-display">{{ totalKcal }} <span>kcal</span></h2>
          <p class="balance-status">Consumo registrado até o momento.</p>
        </div>
        <div class="decoration-circle"></div>
      </section>

      <!-- Layout em Grid Espalhado -->
      <div class="registro-grid">
        
        <!-- Coluna: Sugestões do Plano (RF013) -->
        <section class="column-section">
          <div class="column-header">
            <h3>Sugestões do Plano</h3>
            <p>Rápida efetivação</p>
          </div>

          <div class="sugestoes-stack">
            <article v-for="item in sugestoesIA" :key="item.id" class="sugestao-card">
              <div class="sugestao-info">
                <strong>{{ item.nome }}</strong>
                <div class="macros-badges">
                  <span>{{ item.kcal }} kcal</span>
                  <span>{{ item.prot }}P</span>
                  <span>{{ item.carb }}C</span>
                </div>
              </div>
              <button @click="efetivarSugestao(item)" class="btn-add-plus">Adicionar</button>
            </article>
          </div>
        </section>

        <!-- Coluna: Histórico Diário (RF015 com Remoção) -->
        <section class="column-section">
          <div class="column-header">
            <h3>Consumido Hoje</h3>
            <p>Linha do tempo</p>
          </div>

          <div class="history-timeline">
            <article v-for="log in refeicoesConsumidas" :key="log.id" class="log-entry">
              <div class="log-time-box">{{ log.horario }}</div>
              <div class="log-main-card">
                <div class="log-text">
                  <strong>{{ log.nome }}</strong>
                  <p>{{ log.descricao }} - {{ log.kcal }} kcal</p>
                </div>
                <button @click="removerRefeicao(log.id)" class="btn-remove-log">
                  Remover
                </button>
              </div>
            </article>

            <div v-if="refeicoesConsumidas.length === 0" class="empty-log-msg">
              Nenhum registro encontrado para hoje.
            </div>
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

/* CARD DE DESTAQUE (Página 14 do PDF) */
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

/* GRID DE REGISTRO */
.registro-grid { display: grid; grid-template-columns: 1fr 1.3fr; gap: 4rem; }

.column-header { margin-bottom: 2rem; }
.column-header h3 { font-size: 1.4rem; font-weight: 800; }
.column-header p { color: var(--text-muted); font-size: 0.85rem; }

/* SUGESTÕES */
.sugestoes-stack { display: flex; flex-direction: column; gap: 1rem; }
.sugestao-card {
  background: var(--bg-card); padding: 1.5rem; border-radius: 20px;
  display: flex; justify-content: space-between; align-items: center;
  border: 1px solid rgba(255,255,255,0.03);
}

.sugestao-info strong { display: block; font-size: 1.1rem; margin-bottom: 0.5rem; }
.macros-badges { display: flex; gap: 8px; }
.macros-badges span { background: rgba(255,255,255,0.05); padding: 2px 8px; border-radius: 6px; font-size: 0.75rem; color: var(--text-muted); }

.btn-add-plus {
  background: rgba(16, 185, 129, 0.1); color: var(--accent); border: none;
  padding: 0.6rem 1rem; border-radius: 10px; font-weight: 700; cursor: pointer; transition: 0.3s;
}
.btn-add-plus:hover { background: var(--accent); color: var(--bg-deep); }

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