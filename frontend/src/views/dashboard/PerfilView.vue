<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { meService } from '../../services/modules/me';

const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const erro = ref<string | null>(null);

const nome = ref('—')
const email = ref('—')
const pesoAtual = ref<string>('—')
const altura = ref<string>('—')
const idade = ref<string>('—')
const sexo = ref<string>('—')
const objetivo = ref<string>('—')
const tipoDieta = ref<string>('—')
const condicoesSaude = ref<string>('—')
const alergias = ref<string>('—')

const GOAL_LABELS: Record<string, string> = {
  WEIGHT_LOSS: 'Perda de peso',
  MUSCLE_GAIN: 'Ganho de massa',
  MAINTENANCE: 'Manutenção',
  DIETARY_REEDUCATION: 'Reeducação alimentar',
  SPORTS_PERFORMANCE: 'Performance esportiva',
}

const GENDER_LABELS: Record<string, string> = {
  MALE: 'Masculino',
  FEMALE: 'Feminino',
}

function calcularIdade(dateOfBirth: string): number {
  const nasc = new Date(dateOfBirth)
  const hoje = new Date()
  let anos = hoje.getFullYear() - nasc.getFullYear()
  const m = hoje.getMonth() - nasc.getMonth()
  if (m < 0 || (m === 0 && hoje.getDate() < nasc.getDate())) anos--
  return anos
}

onMounted(async () => {
  try {
    const me = await meService.getMe()

    nome.value = me.name ?? '—'
    email.value = me.email ?? '—'

    if (me.profile) {
      pesoAtual.value = me.profile.weightKg != null ? `${me.profile.weightKg}` : '—'
      altura.value = me.profile.heightCm != null ? `${me.profile.heightCm}` : '—'
      sexo.value = GENDER_LABELS[me.profile.gender] ?? me.profile.gender ?? '—'
      idade.value = me.profile.dateOfBirth ? `${calcularIdade(me.profile.dateOfBirth)}` : '—'
    }

    if (me.assessment) {
      objetivo.value = GOAL_LABELS[me.assessment.goal] ?? me.assessment.goal ?? '—'
      tipoDieta.value = me.assessment.dietaryRestrictions || 'Sem restrições'
      condicoesSaude.value = me.assessment.healthConditions || 'Nenhuma'
      alergias.value = me.assessment.allergies || 'Nenhuma'
    }
  } catch {
    erro.value = 'Não foi possível carregar seu perfil.'
  } finally {
    loading.value = false
  }
})

const voltar = () => router.back();
const logout = () => { authStore.logout(); router.push({ name: 'auth' }); };
const editarPerfil = () => router.push({ name: 'editar-perfil' });
</script>

<template>
  <div class="profile-wrapper">
    <nav class="top-nav">
      <button class="btn-back" @click="voltar">
        <span class="arrow-icon"></span>
        Voltar
      </button>
      <span class="nav-brand">Tukan <span></span></span>
      <button class="btn-logout" @click="logout">Sair</button>
    </nav>

    <!-- Estado: carregando -->
    <div v-if="loading" class="state-center">
      <p style="color: #94a3b8;">Carregando perfil...</p>
    </div>

    <!-- Estado: erro -->
    <div v-else-if="erro" class="state-center">
      <p style="color: #f87171;">{{ erro }}</p>
    </div>

    <!-- Conteúdo real -->
    <div v-else class="content-container">
      <header class="profile-hero">
        <div class="hero-info">
          <h1>{{ nome }}</h1>
          <p>{{ email }}</p>
        </div>
        <button class="btn-primary-edit" @click="editarPerfil">
          Editar Perfil
        </button>
      </header>

      <main class="info-grid">

        <!-- Biometria (RF006) -->
        <section class="info-card">
          <div class="card-header">
            <div class="dot-indicator"></div>
            <h3>Composição Corporal</h3>
          </div>
          <div class="metrics-row">
            <div class="metric-item">
              <p class="m-val">{{ pesoAtual }}<span v-if="pesoAtual !== '—'">kg</span></p>
              <label>Peso Atual</label>
            </div>
            <div class="metric-item">
              <p class="m-val">{{ altura }}<span v-if="altura !== '—'">cm</span></p>
              <label>Altura</label>
            </div>
            <div class="metric-item">
              <p class="m-val">{{ idade }}<span v-if="idade !== '—'"> anos</span></p>
              <label>Idade</label>
            </div>
            <div class="metric-item">
              <p class="m-val" style="font-size: 1.4rem;">{{ sexo }}</p>
              <label>Sexo</label>
            </div>
          </div>
        </section>

        <!-- Objetivo (RF007) -->
        <section class="info-card highlight-card">
          <div class="card-header">
            <div class="dot-indicator green"></div>
            <h3>Foco e Objetivo</h3>
          </div>
          <div class="goal-content">
            <div class="goal-main">
              <label>Meta Principal</label>
              <p>{{ objetivo }}</p>
            </div>
          </div>
        </section>

        <!-- Rotina (RF008) -->
        <section class="info-card">
          <div class="card-header">
            <div class="dot-indicator blue"></div>
            <h3>Rotina Alimentar</h3>
          </div>
          <div class="routine-list">
            <div class="routine-item">
              <span class="r-label">Preferência de Dieta</span>
              <span class="r-val">{{ tipoDieta }}</span>
            </div>
          </div>
        </section>

        <!-- Saúde (RF009) -->
        <section class="info-card">
          <div class="card-header">
            <div class="dot-indicator red"></div>
            <h3>Restrições e Saúde</h3>
          </div>
          <div class="health-summary">
            <div class="health-box">
              <label>Condições Clínicas</label>
              <p>{{ condicoesSaude }}</p>
            </div>
            <div class="health-box warning">
              <label>Alergias</label>
              <p>{{ alergias }}</p>
            </div>
          </div>
        </section>

      </main>
    </div>
  </div>
</template>

<style scoped>
.profile-wrapper {
  --bg-deep: #0f172a;
  --bg-card: #1e293b;
  --accent: #10b981;
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  
  min-height: 100vh;
  background-color: var(--bg-deep);
  color: var(--text-main);
  font-family: 'Inter', sans-serif;
}

/* TOP NAV */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.2rem 5%;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255,255,255,0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.btn-back {
  background: transparent; border: none; color: var(--text-muted);
  display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 600;
}
.btn-back:hover { color: var(--accent); }
.arrow-icon { width: 8px; height: 8px; border-left: 2px solid currentColor; border-bottom: 2px solid currentColor; transform: rotate(45deg); }

.nav-brand { font-weight: 800; text-transform: uppercase; letter-spacing: 1px; font-size: 0.9rem; }
.nav-brand span { color: var(--accent); font-weight: 400; }

.btn-logout { background: rgba(239, 68, 68, 0.1); color: #f87171; border: none; padding: 0.5rem 1.2rem; border-radius: 10px; font-weight: 700; cursor: pointer; }

/* HERO SECTION */
.content-container { max-width: 1100px; margin: 0 auto; padding: 4rem 5%; }

.profile-hero {
  display: flex; align-items: center; gap: 3rem; margin-bottom: 5rem;
}

.avatar-glow {
  position: relative; padding: 5px; background: linear-gradient(45deg, var(--accent), #34d399); border-radius: 40px;
}
.avatar-glow img { width: 130px; height: 130px; border-radius: 35px; border: 4px solid var(--bg-deep); object-fit: cover; }

.status-badge { background: rgba(16, 185, 129, 0.1); color: var(--accent); padding: 4px 12px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
.hero-info h1 { font-size: 3rem; font-weight: 900; letter-spacing: -1.5px; margin: 0.5rem 0; }
.hero-info p { color: var(--text-muted); font-size: 1.1rem; }

.btn-primary-edit {
  margin-left: auto; background: var(--accent); color: var(--bg-deep); border: none; padding: 1.2rem 2.2rem; border-radius: 16px; font-weight: 800; cursor: pointer; transition: all 0.3s;
}
.btn-primary-edit:hover { transform: translateY(-3px); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2); }

/* GRID CARDS */
.info-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 2rem;
}

.info-card {
  background: var(--bg-card); padding: 2.5rem; border-radius: 30px; border: 1px solid rgba(255,255,255,0.03); position: relative; overflow: hidden;
}

.highlight-card { border-color: rgba(16, 185, 129, 0.3); background: linear-gradient(145deg, #1e293b 0%, #161e2b 100%); }

.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 2.5rem; }
.dot-indicator { width: 10px; height: 10px; border-radius: 50%; background: #64748b; }
.dot-indicator.green { background: var(--accent); box-shadow: 0 0 10px var(--accent); }
.dot-indicator.blue { background: #3b82f6; }
.dot-indicator.red { background: #ef4444; }

.card-header h3 { font-size: 0.85rem; text-transform: uppercase; letter-spacing: 2px; color: var(--text-muted); font-weight: 800; }

/* Metrics Row */
.metrics-row { display: flex; gap: 4rem; }
.metric-item .m-val { font-size: 2.5rem; font-weight: 900; letter-spacing: -1px; }
.metric-item .m-val span { font-size: 1rem; color: var(--accent); margin-left: 4px; }
.metric-item label { color: var(--text-muted); font-size: 0.9rem; font-weight: 600; }

/* Goal Style */
.goal-main label { font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase; font-weight: 700; }
.goal-main p { font-size: 2rem; font-weight: 800; color: var(--accent); margin-bottom: 1.5rem; }
.goal-sub { display: flex; gap: 3rem; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 1.5rem; }
.sub-item label { display: block; font-size: 0.75rem; color: var(--text-muted); }
.sub-item span { font-weight: 700; font-size: 1.1rem; }

/* Routine & Health */
.routine-list { display: flex; flex-direction: column; gap: 1rem; }
.routine-item { display: flex; justify-content: space-between; padding: 1rem; background: rgba(0,0,0,0.15); border-radius: 15px; }
.r-label { color: var(--text-muted); font-size: 0.9rem; }
.r-val { font-weight: 700; }

.health-summary { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.health-box { padding: 1.2rem; background: rgba(255,255,255,0.02); border-radius: 18px; border: 1px solid rgba(255,255,255,0.05); }
.health-box label { font-size: 0.7rem; text-transform: uppercase; color: var(--text-muted); font-weight: 800; }
.health-box p { font-weight: 600; margin-top: 5px; }
.health-box.warning { border-color: rgba(239, 68, 68, 0.2); color: #f87171; }

.state-center {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Inter', sans-serif;
}

/* RESPONSIVE */
@media (max-width: 1000px) {
  .info-grid { grid-template-columns: 1fr; }
  .profile-hero { flex-direction: column; text-align: center; gap: 1.5rem; }
  .btn-primary-edit { margin: 0 auto; width: 100%; }
}
</style>