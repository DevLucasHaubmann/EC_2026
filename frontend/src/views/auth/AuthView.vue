<template>
  <div class="auth-layout">
    <AuthCard>
      <div class="auth-header">
        <div class="logo-wrapper">
          <div class="logo-mark">T</div>
          <span class="logo-text">Tukan</span>
        </div>
        <p class="auth-subtitle">Acesse sua plataforma de educação e saúde</p>
      </div>

      <!-- Tabs login / cadastro -->
      <div class="auth-tabs">
        <button
          class="tab-btn"
          :class="{ active: mode === 'login' }"
          type="button"
          @click="switchMode('login')"
        >
          Entrar
        </button>
        <button
          class="tab-btn"
          :class="{ active: mode === 'register' }"
          type="button"
          @click="switchMode('register')"
        >
          Criar conta
        </button>
      </div>

      <!-- Mensagem de erro -->
      <div v-if="authStore.error" class="alert alert-error" role="alert">
        {{ authStore.error }}
      </div>

      <!-- Formulário de Login -->
      <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="auth-form">
        <AuthInput
          v-model="loginForm.email"
          label="E-mail"
          type="email"
          placeholder="exemplo@email.com"
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
          {{ authStore.loading ? 'Entrando...' : 'Entrar na Conta' }}
        </AuthButton>
      </form>

      <!-- Formulário de Cadastro -->
      <form v-else @submit.prevent="handleRegister" class="auth-form">
        <AuthInput
          v-model="registerForm.name"
          label="Nome completo"
          type="text"
          placeholder="Seu nome"
          required
        />
        <AuthInput
          v-model="registerForm.email"
          label="E-mail"
          type="email"
          placeholder="exemplo@email.com"
          required
        />
        <AuthInput
          v-model="registerForm.password"
          label="Senha"
          type="password"
          placeholder="Mínimo 6 caracteres"
          required
        />
        <AuthButton type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? 'Criando conta...' : 'Criar Conta' }}
        </AuthButton>
      </form>

      <div class="auth-footer">
        <router-link to="/" class="footer-link">← Voltar para Início</router-link>
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
// Importando o novo serviço para checar o status do usuário
import { meService } from '../../services/modules/me'

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
    await authStore.login({
      email: loginForm.email,
      password: loginForm.password,
    })
    loginForm.email = ''
    loginForm.password = ''
    await redirectAfterAuth()
  } catch {
    // erro já está em authStore.error
  }
}

async function handleRegister() {
  try {
    await authStore.register({
      name: registerForm.name,
      email: registerForm.email,
      password: registerForm.password,
    })
    registerForm.name = ''
    registerForm.email = ''
    registerForm.password = ''
    await redirectAfterAuth()
  } catch {
    // erro já está em authStore.error
  }
}

// Nova lógica inteligente de redirecionamento
async function redirectAfterAuth() {
  try {
    // Puxa os dados reais do usuário logado no backend
    const meData = await meService.getMe()

    // Verifica se a conta é nova (pendente de perfil/triagem) ou completa
    if (meData.onboardingStatus === 'COMPLETED') {
      router.push({ name: 'dashboard' })
    } else {
      router.push({ name: 'triagem' })
    }
  } catch (error) {
    console.error('Erro ao verificar status do usuário:', error)
    // Fallback de segurança: se a API do /me falhar, joga pro dashboard
    router.push({ name: 'dashboard' })
  }
}
</script>

<style scoped>
.auth-layout {
  --color-bg: #F4F9F4;
  --color-primary: #2E7D32;
  --color-text-main: #2D3748;
  --color-text-muted: #718096;

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-bg);
  padding: 1.5rem;
  font-family: 'Inter', sans-serif;
}

.auth-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.logo-mark {
  background-color: var(--color-primary);
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.25rem;
}

.logo-text {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-primary);
}

.auth-subtitle {
  color: var(--color-text-muted);
  font-size: 0.95rem;
  margin: 0;
}

.auth-tabs {
  display: flex;
  border-bottom: 2px solid #E2E8F0;
  margin-bottom: 1.5rem;
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  padding: 0.625rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.2s, border-color 0.2s;
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.alert {
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.alert-error {
  background-color: #FFF5F5;
  color: #C53030;
  border: 1px solid #FEB2B2;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.auth-footer {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
  font-size: 0.875rem;
}

.footer-link {
  color: var(--color-text-muted);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-link:hover {
  color: var(--color-primary);
}
</style>
