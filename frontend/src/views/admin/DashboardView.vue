<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

// RF004 - Monitoramento de Uso (Dados simulados conforme o PDF)
const stats = ref({
  totalDietasGeradas: 1458,
  usuariosAtivos: 342,
  usoServidorIA: '42%',
  limiteGlobalIA: 5000
});

// RF001 - Definição de Limites de Uso
const limiteMensalIA = ref(10); // Limite de prompts por usuário

// RF002 e RF003 - Lista de Usuários e Gestão de Credenciais
const usuarios = ref([
  { id: 1, nome: 'Adrian Antônio', email: 'adrian@pucpr.edu.br', status: 'Ativo', dietas: 4 },
  { id: 2, nome: 'Lucas Azzolin', email: 'lucas@pucpr.edu.br', status: 'Ativo', dietas: 7 },
  { id: 3, nome: 'Usuário Exemplo', email: 'user@teste.com', status: 'Bloqueado', dietas: 2 },
  { id: 4, nome: 'Vinicius Teider', email: 'vinicius@pucpr.edu.br', status: 'Ativo', dietas: 5 },
]);

const alternarStatus = (id: number) => {
  const user = usuarios.value.find(u => u.id === id);
  if (user) {
    user.status = user.status === 'Ativo' ? 'Bloqueado' : 'Ativo';
  }
};

const resetarSenha = (email: string) => {
  alert(`RF003: Um e-mail de redefinição foi enviado para ${email}`);
};

const voltar = () => router.push('/dashboard');
</script>

<template>
  <div class="admin-wrapper">
    <!-- Topbar Padronizada -->
    <nav class="admin-nav">
      <div class="nav-content">
        <button class="btn-back" @click="voltar">
          <span class="chevron-left"></span> Dashboard Principal
        </button>
        <span class="admin-tag">Painel Administrativo</span>
      </div>
    </nav>

    <main class="admin-container">
      
      <!-- RF004 - Seção de Estatísticas -->
      <section class="stats-grid">
        <article class="stat-card">
          <label>Total de Dietas Geradas</label>
          <p>{{ stats.totalDietasGeradas }}</p>
        </article>
        <article class="stat-card">
          <label>Usuários Ativos</label>
          <p>{{ stats.usuariosAtivos }}</p>
        </article>
        <article class="stat-card">
          <label>Carga da IA</label>
          <p>{{ stats.usoServidorIA }}</p>
        </article>
      </section>

      <div class="admin-main-grid">
        
        <!-- RF002 & RF003 - Gestão de Usuários -->
        <section class="users-section">
          <div class="card-header">
            <h3>Controle de Acessos</h3>
            <p>Gerencie permissões e credenciais da plataforma</p>
          </div>

          <div class="table-container">
            <table class="user-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Status</th>
                  <th>Planos Gerados</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in usuarios" :key="user.id">
                  <td>
                    <div class="user-cell">
                      <strong>{{ user.nome }}</strong>
                      <span>{{ user.email }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="status-pill" :class="user.status.toLowerCase()">
                      {{ user.status }}
                    </span>
                  </td>
                  <td>{{ user.dietas }}</td>
                  <td>
                    <div class="action-btns">
                      <button @click="resetarSenha(user.email)" class="btn-table">Resetar Senha</button>
                      <button @click="alternarStatus(user.id)" class="btn-table danger">
                        {{ user.status === 'Ativo' ? 'Bloquear' : 'Desbloquear' }}
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- RF001 - Configurações do Sistema -->
        <aside class="config-sidebar">
          <div class="config-card">
            <h3>Parâmetros da IA</h3>
            <div class="field mt-6">
              <label>Limite de Gerações (Semanal)</label>
              <div class="range-control">
                <input type="range" v-model="limiteMensalIA" min="1" max="50" />
                <span class="range-val">{{ limiteMensalIA }} prompts</span>
              </div>
              <p class="field-desc">Define o máximo de planos que cada usuário pode gerar por ciclo.</p>
            </div>
            
            <button class="btn-save-config">Aplicar Configurações</button>
          </div>

    
        </aside>

      </div>
    </main>
  </div>
</template>

<style scoped>
.admin-wrapper {
  --bg: #0f172a; --card: #1e293b; --emerald: #10b981; --text-dim: #94a3b8;
  min-height: 100vh; background-color: var(--bg); color: white; font-family: 'Inter', sans-serif;
}

/* NAVBAR */
.admin-nav {
  background: rgba(15, 23, 42, 0.9); backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255,255,255,0.05); position: sticky; top: 0; z-index: 100;
}
.nav-content { max-width: 1200px; margin: 0 auto; padding: 1.2rem 1.5rem; display: flex; justify-content: space-between; align-items: center; }
.btn-back { background: transparent; border: none; color: var(--text-dim); cursor: pointer; font-weight: 600; display: flex; align-items: center; gap: 10px; }
.btn-back:hover { color: var(--emerald); }
.chevron-left { width: 7px; height: 7px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }
.admin-tag { font-weight: 800; text-transform: uppercase; letter-spacing: 2px; font-size: 0.75rem; color: var(--emerald); }

/* CONTENT */
.admin-container { max-width: 1200px; margin: 0 auto; padding: 3rem 1.5rem; }

/* STATS */
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.5rem; margin-bottom: 2.5rem; }
.stat-card { background: var(--card); padding: 2rem; border-radius: 20px; border: 1px solid rgba(255,255,255,0.03); }
.stat-card label { display: block; font-size: 0.75rem; font-weight: 800; text-transform: uppercase; color: var(--text-dim); margin-bottom: 0.5rem; }
.stat-card p { font-size: 2.2rem; font-weight: 900; letter-spacing: -1px; }

/* MAIN GRID */
.admin-main-grid { display: grid; grid-template-columns: 1fr 320px; gap: 2rem; }

.users-section { background: var(--card); border-radius: 24px; padding: 2.5rem; border: 1px solid rgba(255,255,255,0.05); }
.card-header { margin-bottom: 2.5rem; }
.card-header h3 { font-size: 1.4rem; font-weight: 800; }
.card-header p { color: var(--text-dim); font-size: 0.9rem; }

/* TABLE */
.table-container { overflow-x: auto; }
.user-table { width: 100%; border-collapse: collapse; text-align: left; }
.user-table th { padding: 1rem; color: var(--text-dim); font-size: 0.75rem; text-transform: uppercase; font-weight: 800; border-bottom: 1px solid rgba(255,255,255,0.05); }
.user-table td { padding: 1.5rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.03); }

.user-cell { display: flex; flex-direction: column; }
.user-cell span { font-size: 0.8rem; color: var(--text-dim); }

.status-pill { padding: 4px 12px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
.status-pill.ativo { background: rgba(16, 185, 129, 0.1); color: var(--emerald); }
.status-pill.bloqueado { background: rgba(239, 68, 68, 0.1); color: #f87171; }

.action-btns { display: flex; gap: 10px; }
.btn-table { background: rgba(255,255,255,0.05); border: none; color: white; padding: 6px 12px; border-radius: 8px; font-size: 0.75rem; font-weight: 700; cursor: pointer; }
.btn-table:hover { background: rgba(255,255,255,0.1); }
.btn-table.danger { color: #f87171; }
.btn-table.danger:hover { background: rgba(239, 68, 68, 0.1); }

/* SIDEBAR CONFIG */
.config-card { background: var(--card); padding: 2rem; border-radius: 24px; border: 1px solid rgba(255,255,255,0.05); }
.config-card h3 { font-size: 1rem; font-weight: 800; }
.config-card.info { background: linear-gradient(145deg, #1e293b 0%, #161e2b 100%); border-color: rgba(16, 185, 129, 0.2); }
.config-card.info p { font-size: 0.85rem; color: var(--text-dim); margin-top: 1rem; line-height: 1.5; }

.field label { font-size: 0.8rem; font-weight: 700; color: #cbd5e1; display: block; margin-bottom: 1rem; }
.range-control { display: flex; align-items: center; gap: 15px; }
input[type="range"] { flex: 1; accent-color: var(--emerald); }
.range-val { font-weight: 800; font-size: 0.9rem; color: var(--emerald); min-width: 80px; }
.field-desc { font-size: 0.75rem; color: var(--text-dim); margin-top: 10px; }

.btn-save-config { width: 100%; background: var(--emerald); color: var(--bg); border: none; padding: 1rem; border-radius: 12px; font-weight: 800; margin-top: 2rem; cursor: pointer; transition: 0.3s; }
.btn-save-config:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2); }

@media (max-width: 1000px) { .admin-main-grid { grid-template-columns: 1fr; } .stats-grid { grid-template-columns: 1fr; } }
</style>