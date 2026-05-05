<template>
  <div class="auth-layout">
    <!-- Círculos de luz no fundo -->
    <div class="bg-glow-1"></div>
    <div class="bg-glow-2"></div>

    <AuthCard class="main-card">
      <div class="auth-header">
        <div class="logo-area">
          <div class="logo-icon-box">
            <!-- SVG do Tucano direto aqui para não dar erro de importação -->
            <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg" class="tukan-svg">
              <path d="M20 40C20 28.9543 28.9543 20 40 20H80V35C80 43.2843 73.2843 50 65 50H50V65C50 73.2843 43.2843 80 35 80H20V40Z" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="55" cy="35" r="4" fill="currentColor"/>
              <path d="M20 55H35" stroke="currentColor" stroke-width="6" stroke-linecap="round"/>
            </svg>
          </div>
          <span class="logo-name">tukan<span></span></span>
        </div>
        <p class="auth-desc">Acesse sua plataforma de nutrição inteligente</p>
      </div>

      <!-- Tabs de navegação -->
      <div class="tabs-container">
        <button 
          class="tab-item" 
          :class="{ active: mode === 'login' }" 
          @click="switchMode('login')"
        >
          Entrar
        </button>
        <button 
          class="tab-item" 
          :class="{ active: mode === 'register' }" 
          @click="switchMode('register')"
        >
          Criar conta
        </button>
        <div class="tab-highlight" :style="{ left: mode === 'login' ? '4px' : '50%' }"></div>
      </div>

      <!-- Erro -->
      <div v-if="authStore.error" class="error-msg">
        {{ authStore.error }}
      </div>

      <!-- Login -->
      <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="form-wrapper">
        <AuthInput
          v-model="loginForm.email"
          label="E-mail"
          type="email"
          placeholder="seu@email.com"
          required
        />
        <AuthInput
          v-model="loginForm.password"
          label="Senha"
          type="password"
          placeholder="••••••••"
          required
        />
        <AuthButton type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? 'Entrando...' : 'Acessar Conta' }}
        </AuthButton>
      </form>

      <!-- Cadastro -->
      <form v-else @submit.prevent="handleRegister" class="form-wrapper">
        <AuthInput
          v-model="registerForm.name"
          label="Nome completo"
          required
        />
        <AuthInput
          v-model="registerForm.email"
          label="E-mail"
          type="email"
          required
        />
        <AuthInput
          v-model="registerForm.password"
          label="Crie uma senha"
          type="password"
          required
        />
        <AuthButton type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? 'Criando...' : 'Finalizar Cadastro' }}
        </AuthButton>
      </form>

      <div class="auth-footer">
        <router-link to="/" class="back-link">← Voltar para o início</router-link>
      </div>
    </AuthCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import AuthCard from '../../components/auth/AuthCard.vue'
import AuthInput from '../../components/auth/AuthInput.vue'
import AuthButton from '../../components/auth/AuthButton.vue'
import { useAuthStore } from '../../stores'

type Mode = 'login' | 'register'

const router = useRouter()
const authStore = useAuthStore()
const mode = ref<Mode>('login')

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({ name: '', email: '', password: '' })

function switchMode(next: Mode) {
  mode.value = next
  authStore.clearError()
}

async function handleLogin() {
  try {
    const nextStep = await authStore.login({ email: loginForm.email, password: loginForm.password })
    router.push(resolveNextStep(nextStep))
  } catch (e) {}
}

async function handleRegister() {
  try {
    const nextStep = await authStore.register({ name: registerForm.name, email: registerForm.email, password: registerForm.password })
    router.push(resolveNextStep(nextStep))
  } catch (e) {}
}

function resolveNextStep(nextStep: string) {
  if (nextStep === '/profiles/first-access') return { name: 'perfil' }
  if (nextStep === '/assessments') return { name: 'triagem' }
  if (nextStep === '/dashboard') return { name: 'dashboard' }
  return { name: 'dashboard' }
}
</script>

<style scoped>
.auth-layout {
  --green: #10b981;
  --dark: #0f172a;
  --card: #1e293b;
  
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--dark);
  position: relative;
  overflow: hidden;
  font-family: 'Inter', sans-serif;
}

/* Efeito de luz no fundo */
.bg-glow-1, .bg-glow-2 {
  position: absolute;
  width: 400px; height: 400px;
  border-radius: 50%;
  filter: blur(100px);
  z-index: 0;
  opacity: 0.15;
}
.bg-glow-1 { background: var(--green); top: -100px; right: -100px; }
.bg-glow-2 { background: #06b6d4; bottom: -150px; left: -150px; }

/* Estilizando o AuthCard de fora */
:deep(.main-card) {
  background: rgba(30, 41, 59, 0.8) !important;
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 2.5rem;
  width: 100%;
  max-width: 420px;
  z-index: 10;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5);
}

.auth-header { text-align: center; margin-bottom: 2rem; }

.logo-area { display: flex; align-items: center; justify-content: center; gap: 12px; margin-bottom: 0.5rem; }
.logo-icon-box { width: 40px; height: 40px; color: var(--green); }
.logo-name { font-size: 1.8rem; font-weight: 800; color: white; letter-spacing: -1px; }
.logo-name span { color: var(--green); font-weight: 400; }

.auth-desc { color: #94a3b8; font-size: 0.9rem; }

/* Tabs modernas */
.tabs-container {
  display: flex;
  background: rgba(15, 23, 42, 0.6);
  padding: 4px;
  border-radius: 12px;
  margin-bottom: 2rem;
  position: relative;
}

.tab-item {
  flex: 1; background: none; border: none; padding: 0.8rem;
  color: #94a3b8; font-weight: 600; cursor: pointer; z-index: 2;
  transition: color 0.3s;
}
.tab-item.active { color: white; }

.tab-highlight {
  position: absolute; height: calc(100% - 8px); width: calc(50% - 6px);
  background: var(--green); top: 4px; border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.error-msg {
  background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2);
  color: #fca5a5; padding: 0.8rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 1rem;
}

.form-wrapper { display: flex; flex-direction: column; gap: 1.5rem; }

/* ESTILIZAÇÃO PROFUNDA DOS SEUS COMPONENTES */
:deep(label) { color: #cbd5e1 !important; margin-bottom: 0.5rem !important; display: block; }

:deep(input) {
  background: rgba(15, 23, 42, 0.6) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: white !important;
  padding: 0.8rem 1rem !important;
  border-radius: 10px !important;
}

:deep(button[type="submit"]) {
  background: var(--green) !important;
  color: var(--dark) !important;
  font-weight: 800 !important;
  padding: 1rem !important;
  border-radius: 10px !important;
  border: none !important;
  margin-top: 1rem;
  cursor: pointer;
}

.auth-footer { margin-top: 2rem; text-align: center; }
.back-link { color: #94a3b8; text-decoration: none; font-size: 0.85rem; }
.back-link:hover { color: var(--green); }
</style>