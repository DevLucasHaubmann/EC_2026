<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const diferenciais = ref([
  {
    titulo: 'Confiável',
    desc: 'Centraliza recomendações baseadas em dados e reduz a confusão causada por informações genéricas na internet.',
    icone: ''
  },
  {
    titulo: 'Adaptável',
    desc: 'Considera diabetes, vegetarianismo, rotina diária, preferências e evolução do usuário ao longo do tempo.',
    icone: ''
  }
]);

</script>

<template>
  <div class="tukan-landing">
    <!-- Navbar -->
    <nav class="nav-header">
      <div class="brand">
        <!-- ÍCONE SVG DO TUKAN (Baseado na pág 1 do PDF) -->
        <div class="logo-wrapper">
          <svg class="tukan-icon" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20 40C20 28.9543 28.9543 20 40 20H80V35C80 43.2843 73.2843 50 65 50H50V65C50 73.2843 43.2843 80 35 80H20V40Z" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="55" cy="35" r="4" fill="currentColor"/>
            <path d="M20 55H35" stroke="currentColor" stroke-width="6" stroke-linecap="round"/>
          </svg>
        </div>
        <span class="logo-text">tukan<span>nutrition</span></span>
      </div>
      <div class="auth-btns">
        <button class="btn-login" @click="router.push({ name: 'auth' })">Entrar</button>
      </div>
    </nav>

    <!-- Hero Section -->
    <header class="hero">
      <div class="hero-content">
        <p class="ia-badge">Inteligência Artificial aplicada à nutrição</p>
        
        <h1>Seu plano alimentar se <span>adapta à sua vida.</span></h1>
        
        <p class="hero-desc">
          Plataforma web inteligente que transforma perfil biométrico, rotina e 
          restrições em orientações nutricionais dinâmicas e acessíveis.
        </p>

        <div class="cta-group">
          <button class="btn-primary" @click="router.push({ name: 'auth' })">Começar agora</button>
        </div>
      </div>

      <!-- Preview do Dashboard -->
      <div class="dashboard-preview">
        <div class="preview-card">
          <div class="preview-header">
            <span>Dashboard inteligente</span>
            <div class="status-dot"></div>
          </div>
          <div class="preview-stats">
            <div class="stat-item">
              <small>Meta calórica</small>
              <p>2.050 <strong>kcal</strong></p>
            </div>
            <div class="stat-progress">
              <div class="progress-circle">75%</div>
            </div>
          </div>
          <div class="preview-alert">
            "Sugerimos aumentar a ingestão de água hoje."
          </div>
        </div>
      </div>
    </header>

    <!-- Diferenciais -->
    <section class="features-row">
      <div v-for="item in diferenciais" :key="item.titulo" class="feature-card">
        <span class="icon">{{ item.icone }}</span>
        <div class="text">
          <h3>{{ item.titulo }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Funcionalidades -->
    <section class="functionalities">
      <h2>Recursos do Sistema</h2>
      <div class="func-grid">
        <div class="func-item">
          <strong>IA Gerativa</strong>
          <p>Planos alimentares estruturados semanalmente.</p>
        </div>
        <div class="func-item">
          <strong>Evolução de IMC</strong>
          <p>Gráficos automáticos de acompanhamento.</p>
        </div>
        <div class="func-item">
          <strong>Triagem Completa</strong>
          <p>Consideramos alergias e restrições clínicas.</p>
        </div>
        <div class="func-item">
          <strong>Feedback Adaptativo</strong>
          <p>Ajustes automáticos baseados na sua rotina.</p>
        </div>
      </div>
    </section>

    <footer class="simple-footer">
      <p>Curitiba, 2026 • Projeto Desenvolvido para Disciplina de Expêriencia Criativa • PUCPR</p>
    </footer>
  </div>
</template>

<style scoped>
/* CORES E ESTILO */
.tukan-landing {
  --bg-dark: #0f172a;
  --card-dark: #1e293b;
  --accent-green: #10b981;
  --text-white: #f8fafc;
  --text-gray: #94a3b8;
  
  background-color: var(--bg-dark);
  color: var(--text-white);
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
  padding: 0 8%;
}

/* NAVBAR E LOGO */
.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2.5rem 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo-wrapper {
  width: 50px;
  height: 50px;
  color: var(--accent-green);
}

.tukan-icon {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 2.2rem; /* Tamanho da Logo Maior */
  font-weight: 800;
  letter-spacing: -1.5px;
  text-transform: lowercase;
  color: var(--text-white);
}

.logo-text span {
  color: var(--accent-green);
  font-weight: 400;
  margin-left: 2px;
}

.btn-login {
  background: transparent;
  border: 1px solid var(--text-gray);
  color: var(--text-white);
  padding: 0.6rem 1.8rem;
  border-radius: 25px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-login:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

/* HERO SECTION */
.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 5rem;
  padding: 5rem 0;
  align-items: center;
}

.ia-badge {
  color: var(--accent-green);
  font-weight: 700;
  font-size: 0.85rem;
  margin-bottom: 1.5rem;
  letter-spacing: 1px;
}

.hero h1 {
  font-size: 4rem;
  line-height: 1.1;
  font-weight: 800;
  margin-bottom: 2rem;
}

.hero h1 span {
  color: var(--accent-green);
}

.hero-desc {
  font-size: 1.25rem;
  color: var(--text-gray);
  line-height: 1.6;
  margin-bottom: 3.5rem;
}

.btn-primary {
  background: var(--accent-green);
  color: var(--bg-dark);
  border: none;
  padding: 1.2rem 3rem;
  border-radius: 14px;
  font-weight: 800;
  font-size: 1.1rem;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.btn-primary:hover {
  transform: translateY(-3px);
  filter: brightness(1.1);
}

/* PREVIEW CARD */
.preview-card {
  background: var(--card-dark);
  padding: 2.5rem;
  border-radius: 30px;
  border: 1px solid rgba(255,255,255,0.05);
  box-shadow: 0 30px 60px rgba(0,0,0,0.4);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2.5rem;
  color: var(--text-gray);
  font-size: 0.85rem;
}

.status-dot { width: 10px; height: 10px; background: var(--accent-green); border-radius: 50%; box-shadow: 0 0 10px var(--accent-green); }

.stat-item p { font-size: 2.5rem; font-weight: 700; margin-top: 0.5rem; }
.stat-item strong { color: var(--accent-green); font-size: 1rem; }

.preview-alert {
  margin-top: 2.5rem;
  padding: 1.2rem;
  background: rgba(16, 185, 129, 0.08);
  border-radius: 15px;
  font-size: 0.9rem;
  color: var(--accent-green);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

/* FEATURES & GRID */
.features-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3rem;
  margin: 6rem 0;
  padding: 4rem 0;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.feature-card h3 { font-size: 1.5rem; margin-bottom: 0.8rem; color: var(--accent-green); }

.functionalities h2 { text-align: center; margin-bottom: 4rem; font-size: 2.5rem; }

.func-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 2rem;
}

.func-item {
  background: rgba(255,255,255,0.02);
  padding: 2.5rem;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.05);
  transition: all 0.3s ease;
}

.func-item:hover { background: rgba(255,255,255,0.05); border-color: var(--accent-green); }

.func-item strong { color: var(--accent-green); font-size: 1.1rem; display: block; margin-bottom: 1rem; }

.simple-footer {
  text-align: center;
  padding: 5rem 0 3rem;
  color: var(--text-gray);
  font-size: 0.9rem;
}

@media (max-width: 1100px) {
  .hero { grid-template-columns: 1fr; text-align: center; }
  .logo-text { font-size: 1.8rem; }
  .hero h1 { font-size: 3rem; }
}
</style>