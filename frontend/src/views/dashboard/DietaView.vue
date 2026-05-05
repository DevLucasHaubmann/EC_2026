<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

// Dados baseados na Tela 05 e RF011/RF012/RF013
const planoAlimentar = ref({
  dataGeracao: '03 de Maio de 2026',
  justificativaIA: {
    titulo: 'Por que este plano combina com você',
    pontos: [
      'Estrutura alimentar com refeições práticas para encaixar em rotina acadêmica e trabalho.',
      'Priorização de saciedade e controle glicêmico com carboidratos de melhor qualidade.',
      'Ajustes pensados para prevenção de abandono por baixa praticidade.',
      'Opções de substituição para preferências e restrições alimentares.'
    ]
  },
  observacoes: [
    'Objetivo: reduzir gordura corporal com manutenção de energia diária',
    'Condição observada: tendência a longos períodos sem comer',
    'Preferência: refeições rápidas e acessíveis'
  ],
  refeicoes: [
    {
      id: 1,
      tipo: 'Café da manhã',
      alimentos: 'Iogurte natural, banana, aveia e chia com opção sem lactose.',
      macros: { prot: '28g', carb: '44g', gord: '12g' },
      kcal: 420,
      tag: 'Adaptado para rotina corrida'
    },
    {
      id: 2,
      tipo: 'Almoço',
      alimentos: 'Arroz integral, feijão, frango grelhado e salada fresca.',
      macros: { prot: '38g', carb: '58g', gord: '18g' },
      kcal: 610,
      tag: 'Controle glicêmico'
    },
    {
      id: 3,
      tipo: 'Lanche',
      alimentos: 'Sanduíche natural com pasta de ricota e tomate.',
      macros: { prot: '16g', carb: '22g', gord: '9g' },
      kcal: 240,
      tag: 'Prático para faculdade'
    },
    {
      id: 4,
      tipo: 'Jantar',
      alimentos: 'Omelete com legumes, batata-doce assada e folhas verdes.',
      macros: { prot: '34g', carb: '41g', gord: '20g' },
      kcal: 530,
      tag: 'Leve e saciante'
    }
  ]
});

// Ações
const trocarRefeicao = (id: number) => {
  // RF012 e RF020: Lógica para solicitar nova sugestão à IA
  console.log('Solicitando substituição para a refeição:', id);
};

const voltar = () => router.back();
const logout = () => authStore.logout();
</script>

<template>
  <div class="dieta-page-wrapper">
    <!-- NAVBAR PADRONIZADA -->
    <nav class="standard-nav">
      <button class="btn-back" @click="voltar">
        <span class="chevron-left"></span>
        Voltar
      </button>
      <span class="brand-logo">Tukan <span></span></span>
      <button class="btn-logout" @click="logout">Sair</button>
    </nav>

    <main class="dieta-content">
      <!-- Cabeçalho da Dieta (Tela 05) -->
      <header class="dieta-intro">
        <span class="gen-date">Plano gerado em: {{ planoAlimentar.dataGeracao }}</span>
        <h1>Plano alimentar completo com justificativa da IA</h1>
        <p class="intro-desc">
          Realize a personalização da sua dieta e confira o porquê de cada bloco alimentar, 
          mostrando calorias, macros e adaptações possíveis.
        </p>
      </header>

      <div class="dieta-grid-layout">
        
        <!-- Coluna da Esquerda: Lista de Refeições -->
        <section class="meals-column">
          <article v-for="refeicao in planoAlimentar.refeicoes" :key="refeicao.id" class="meal-card-item">
            <div class="meal-card-header">
              <div class="meal-title-group">
                <span class="meal-type">{{ refeicao.tipo }}</span>
                <span class="meal-kcal">{{ refeicao.kcal }} kcal</span>
              </div>
              <button class="btn-swap-meal" @click="trocarRefeicao(refeicao.id)">
                Trocar refeição
              </button>
            </div>

            <p class="meal-description">{{ refeicao.alimentos }}</p>
            
            <div class="meal-footer">
              <div class="macros-display">
                <span>{{ refeicao.macros.prot }} prot</span>
                <span>{{ refeicao.macros.carb }} carb</span>
                <span>{{ refeicao.macros.gord }} gord</span>
              </div>
              <span class="meal-context-tag">{{ refeicao.tag }}</span>
            </div>
          </article>
        </section>

        <!-- Coluna da Direita: Justificativa e Observações (Sidebar da Tela 05) -->
        <aside class="ai-justification-column">
          
          <!-- Card de Recomendação Inteligente -->
          <section class="ai-strategy-card">
            <div class="card-header">
              <h3>Recomendação inteligente</h3>
            </div>
            <div class="strategy-content">
              <h4>{{ planoAlimentar.justificativaIA.titulo }}</h4>
              <ul>
                <li v-for="(ponto, i) in planoAlimentar.justificativaIA.pontos" :key="i">
                  {{ ponto }}
                </li>
              </ul>
            </div>
          </section>

          <!-- Card de Observações do Perfil -->
          <section class="profile-notes-card">
            <h3>Observações do perfil</h3>
            <div class="notes-list">
              <div v-for="(obs, i) in planoAlimentar.observacoes" :key="i" class="note-item">
                <span class="note-dot"></span>
                <p>{{ obs }}</p>
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

/* NAVBAR */
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
.dieta-content { max-width: 1200px; margin: 0 auto; padding: 4rem 1.5rem; }

.dieta-intro { margin-bottom: 4rem; }
.gen-date { color: var(--accent); font-weight: 800; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1.5px; }
.dieta-intro h1 { font-size: 2.5rem; font-weight: 900; margin: 0.8rem 0; letter-spacing: -1px; line-height: 1.1; }
.intro-desc { color: var(--text-muted); font-size: 1.1rem; max-width: 800px; }

/* GRID LAYOUT */
.dieta-grid-layout { display: grid; grid-template-columns: 1fr 380px; gap: 3rem; }

/* CARDS DE REFEIÇÃO */
.meals-column { display: flex; flex-direction: column; gap: 1.5rem; }
.meal-card-item {
  background: var(--bg-card); padding: 2rem; border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.03);
}

.meal-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.meal-type { font-size: 1.25rem; font-weight: 800; display: block; }
.meal-kcal { font-size: 0.85rem; color: var(--text-muted); font-weight: 600; background: rgba(255,255,255,0.05); padding: 4px 10px; border-radius: 8px; margin-top: 4px; display: inline-block; }

.btn-swap-meal {
  background: transparent; border: 1px solid rgba(255,255,255,0.1); color: white;
  padding: 0.6rem 1rem; border-radius: 10px; font-size: 0.8rem; font-weight: 700; cursor: pointer; transition: 0.3s;
}
.btn-swap-meal:hover { border-color: var(--accent); color: var(--accent); }

.meal-description { font-size: 1.1rem; line-height: 1.6; color: #cbd5e1; margin-bottom: 2rem; }

.meal-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 1.5rem; }
.macros-display { display: flex; gap: 1.2rem; }
.macros-display span { font-size: 0.85rem; color: var(--text-muted); font-weight: 600; }
.meal-context-tag { font-size: 0.75rem; font-weight: 700; color: var(--accent); background: rgba(16, 185, 129, 0.1); padding: 5px 12px; border-radius: 20px; }

/* SIDEBAR IA (Tela 05) */
.ai-justification-column { display: flex; flex-direction: column; gap: 2rem; }

.ai-strategy-card {
  background: linear-gradient(145deg, #1e293b 0%, #161e2b 100%);
  padding: 2rem; border-radius: 24px; border: 1px solid rgba(16, 185, 129, 0.2);
}
.ai-strategy-card .card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 1.5rem; }
.ia-icon { font-size: 1.5rem; }
.ai-strategy-card h3 { font-size: 0.85rem; text-transform: uppercase; letter-spacing: 1.5px; color: var(--accent); font-weight: 800; }

.strategy-content h4 { font-size: 1.1rem; font-weight: 800; margin-bottom: 1.5rem; line-height: 1.4; }
.strategy-content ul { list-style: none; display: flex; flex-direction: column; gap: 1.2rem; }
.strategy-content li { font-size: 0.9rem; color: #cbd5e1; line-height: 1.5; padding-left: 1.2rem; position: relative; }
.strategy-content li::before { content: "•"; position: absolute; left: 0; color: var(--accent); font-weight: bold; }

.profile-notes-card { background: rgba(255,255,255,0.02); padding: 2rem; border-radius: 24px; border: 1px solid rgba(255,255,255,0.05); }
.profile-notes-card h3 { font-size: 0.9rem; font-weight: 800; margin-bottom: 1.5rem; color: var(--text-muted); text-transform: uppercase; }

.notes-list { display: flex; flex-direction: column; gap: 1.2rem; }
.note-item { display: flex; gap: 10px; align-items: flex-start; }
.note-dot { width: 6px; height: 6px; background: var(--text-muted); border-radius: 50%; margin-top: 6px; }
.note-item p { font-size: 0.85rem; color: var(--text-muted); line-height: 1.4; }

@media (max-width: 1024px) {
  .dieta-grid-layout { grid-template-columns: 1fr; }
  .ai-justification-column { order: -1; }
}
</style>