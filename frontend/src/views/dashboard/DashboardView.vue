<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { meService } from '@/services/modules/me';
import { dashboardService } from '@/services/modules/dashboard'; // Esse já existe na sua branch!

const userName = ref('Usuário');
const dashboardData = ref(null);
const carregando = ref(true);

onMounted(async () => {
  try {
    // 1. Pega o nome real do usuário
    const me = await meService.getMe();
    userName.value = me.userName || 'Usuário';

    // 2. Pega as calorias e refeições do dia (endpoint GET /dashboard)
    const dash = await dashboardService.getDashboard();
    dashboardData.value = dash;
  } catch (error) {
    console.error("Erro ao carregar o dashboard:", error);
  } finally {
    carregando.value = false;
  }
});
</script>

<template>
  <div class="dashboard-container">
    <!-- Header de Boas-vindas -->
    <header class="welcome-header">
      <div class="user-info">
        <h2>Bom dia, {{ userName }}</h2>
        <p>Bem-vindo ao <strong>Tukan</strong>. Vamos manter o foco hoje?</p>
      </div>
      <button class="btn-perfil-top" @click="irPara('/perfil')">
        Meu Perfil
      </button>
      <button class="btn-logout" @click="fazerLogout" title="Sair do sistema">
          Sair 
        </button>
    </header>

    <!-- Card de Progresso Rápido -->
    <section class="quick-stats">
      <div class="progress-card">
        <div class="progress-info">
          <h3>Calorias do Dia</h3>
          <span>{{ usuario.kcalConsumidas }} / {{ usuario.kcalMeta }} kcal</span>
        </div>
        <div class="progress-bar-bg">
          <div 
            class="progress-bar-fill" 
            :style="{ width: (usuario.kcalConsumidas / usuario.kcalMeta * 100) + '%' }"
          ></div>
        </div>
      </div>
    </section>

    <!-- Grade de Navegação Principal -->
    <section class="menu-grid">
      <div 
        v-for="item in navegacao" 
        :key="item.path" 
        class="menu-card" 
        @click="irPara(item.path)"
      >
        <span class="icon">{{ item.icone }}</span>
        <div class="card-text">
          <h3>{{ item.nome }}</h3>
          <p>{{ item.desc }}</p>
        </div>
        <span class="arrow">→</span>
      </div>
    </section>

    <!-- Atalho para Admin (Se for Admin) -->
    <footer v-if="authStore.isAuthenticated" class="admin-shortcut">
      <button @click="irPara('/admin/usuarios')" class="btn-admin">
        ⚙️ Acessar Painel Admin
      </button>
    </footer>
  </div>
</template>

<style scoped>
.dashboard-container {
  max-width: 900px;
  margin: 2rem auto;
  padding: 0 1.5rem;
}

.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2.5rem;
}

.user-info h1 {
  font-size: 1.8rem;
  color: #333;
  margin-bottom: 0.2rem;
}

.user-info p {
  color: #666;
}

.btn-perfil-top {
  background: white;
  border: 1px solid #ddd;
  padding: 0.6rem 1.2rem;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-perfil-top:hover {
  background: #f5f5f5;
  border-color: var(--primary-color, #2D5A27);
}

.quick-stats {
  margin-bottom: 2.5rem;
}

.progress-card {
  background: var(--primary-color, #2D5A27);
  color: white;
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(45, 90, 39, 0.2);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.progress-bar-bg {
  height: 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: #A8D5BA;
  border-radius: 6px;
  transition: width 0.5s ease-out;
}

.menu-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.2rem;
}

.menu-card {
  background: white;
  padding: 1.5rem;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 1rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #f0f0f0;
}

.menu-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 15px rgba(0,0,0,0.08);
  border-color: #A8D5BA;
}

.icon {
  font-size: 2rem;
}

.card-text h3 {
  font-size: 1.1rem;
  margin-bottom: 0.2rem;
}

.card-text p {
  font-size: 0.85rem;
  color: #888;
}

.arrow {
  margin-left: auto;
  color: #ccc;
  font-weight: bold;
}

.admin-shortcut {
  margin-top: 3rem;
  text-align: center;
}

.btn-admin {
  background: #333;
  color: white;
  border: none;
  padding: 0.8rem 1.5rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
}

/* Responsividade para Celular */
@media (max-width: 600px) {
  .menu-grid {
    grid-template-columns: 1fr;
  }
  
  .welcome-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.btn-perfil-top {
  background: white;
  border: 1px solid #ddd;
  padding: 0.6rem 1.2rem;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-logout {
  background: #fff0f0; /* Um tom leve de vermelho para indicar saída */
  border: 1px solid #ffcccc;
  color: #c0392b;
  padding: 0.6rem 1.2rem;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-logout:hover {
  background: #ffe0e0;
  border-color: #e74c3c;
  transform: scale(1.05);
}

/* Ajuste na responsividade */
@media (max-width: 600px) {
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}

</style>