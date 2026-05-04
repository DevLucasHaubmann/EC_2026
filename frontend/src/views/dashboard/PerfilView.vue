<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from "../../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

// Dados simulados (No futuro virão do seu Backend Spring Boot)
const usuario = {
  nome: 'Adrian Antônio',
  email: 'adrian.souza@pucpr.edu.br',
  bio: 'Focado em ganho de massa magra e alta performance.',
  foto: 'https://ui-avatars.com/api/?name=Adrian+Souza&background=2D5A27&color=fff',
  metricas: {
    peso: '78kg',
    altura: '1.82m',
    objetivo: 'Ganho de Massa',
    plano: 'Hipertrofia Ativa'
  }
};

const editarPerfil = () => {
  router.push('/perfil/editar');
};
</script>

<template>
  <div class="profile-page">
    <!-- Header com fundo decorativo -->
    <div class="profile-header-bg"></div>

    <div class="profile-content">
      <!-- Card Principal -->
      <section class="main-card">
        <div class="profile-main-info">
          <img :src="usuario.foto" alt="Avatar" class="profile-avatar" />
          <div class="text-info">
            <h2>{{ usuario.nome }}</h2>
            <p class="user-email">{{ usuario.email }}</p>
            <span class="badge-status">Plano Premium</span>
          </div>
          <button @click="editarPerfil" class="btn-edit-main">
            ✏️ Editar Perfil
          </button>
        </div>

        <div class="bio-section">
          <p>{{ usuario.bio }}</p>
        </div>
      </section>

      <!-- Grade de Métricas -->
      <section class="metrics-grid">
        <div class="metric-item">
          <span class="metric-label">Peso Atual</span>
          <span class="metric-value">{{ usuario.metricas.peso }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">Altura</span>
          <span class="metric-value">{{ usuario.metricas.altura }}</span>
        </div>
        <div class="metric-item highlight">
          <span class="metric-label">Objetivo</span>
          <span class="metric-value">{{ usuario.metricas.objetivo }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">Frequência</span>
          <span class="metric-value">5x / semana</span>
        </div>
      </section>

      <!-- Seção de Conta/Segurança -->
      <section class="settings-list">
        <h3>Preferências da Conta</h3>
        <div class="settings-item">
          <div class="settings-text">
            <strong>Notificações de Refeição</strong>
            <span>Lembrar 15min antes das refeições</span>
          </div>
          <label class="switch">
            <input type="checkbox" checked>
            <span class="slider"></span>
          </label>
        </div>
        <div class="settings-item danger" @click="authStore.logout()">
          <div class="settings-text">
            <strong>Encerrar Sessão</strong>
            <span>Sair da conta Tukan em todos os dispositivos</span>
          </div>
          <span class="arrow">→</span>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Variáveis de Cor - Use as mesmas do seu projeto */
:root {
  --primary: #2D5A27;
  --secondary: #A8D5BA;
  --bg-light: #f8f9fa;
  --text-dark: #2c3e50;
}

.profile-page {
  min-height: 100vh;
  background-color: #f4f7f6;
  position: relative;
}

.profile-header-bg {
  height: 180px;
  background: linear-gradient(135deg, #2D5A27 0%, #1a3a17 100%);
  width: 100%;
}

.profile-content {
  max-width: 800px;
  margin: -60px auto 0;
  padding: 0 1.5rem 3rem;
}

/* Card Principal */
.main-card {
  background: white;
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 10px 25px rgba(0,0,0,0.05);
  margin-bottom: 1.5rem;
}

.profile-main-info {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  position: relative;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid white;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

.text-info h2 {
  font-size: 1.6rem;
  color: var(--text-dark);
  margin-bottom: 0.2rem;
}

.user-email {
  color: #7f8c8d;
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.badge-status {
  background: #e8f5e9;
  color: #2e7d32;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 20px;
  text-transform: uppercase;
}

.btn-edit-main {
  margin-left: auto;
  background: transparent;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit-main:hover {
  background: #f8f9fa;
  border-color: #2D5A27;
}

.bio-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eee;
  color: #555;
  line-height: 1.6;
}

/* Grid de Métricas */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.metric-item {
  background: white;
  padding: 1.2rem;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 10px rgba(0,0,0,0.02);
  border: 1px solid rgba(0,0,0,0.03);
}

.metric-item.highlight {
  background: #e8f5e9;
  border-color: #A8D5BA;
}

.metric-label {
  font-size: 0.8rem;
  color: #888;
  font-weight: 600;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

.metric-value {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-dark);
}

/* Lista de Configurações */
.settings-list {
  background: white;
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 10px 25px rgba(0,0,0,0.05);
}

.settings-list h3 {
  font-size: 1.1rem;
  margin-bottom: 1.2rem;
  color: var(--text-dark);
}

.settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #f9f9f9;
}

.settings-item:last-child {
  border-bottom: none;
}

.settings-text {
  display: flex;
  flex-direction: column;
}

.settings-text strong {
  font-size: 0.95rem;
  color: #333;
}

.settings-text span {
  font-size: 0.85rem;
  color: #999;
}

.settings-item.danger {
  color: #d32f2f;
  cursor: pointer;
}

.settings-item.danger strong {
  color: #d32f2f;
}

/* Toggle Switch Simples */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 22px;
}

.switch input { display:none; }

.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 34px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px; width: 16px;
  left: 3px; bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider { background-color: #2D5A27; }
input:checked + .slider:before { transform: translateX(22px); }

/* Responsividade */
@media (max-width: 600px) {
  .profile-main-info {
    flex-direction: column;
    text-align: center;
  }
  
  .btn-edit-main {
    margin: 1rem auto 0;
  }
}
</style>