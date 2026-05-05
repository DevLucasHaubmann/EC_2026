/**
 * Auth Store
 * Manages authentication state: tokens, loading, error and session persistence
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService } from '../services/modules/auth'
import type { LoginRequest, RegisterRequest } from '@/types/auth'

const ACCESS_TOKEN_KEY = 'tukan_access_token'
const REFRESH_TOKEN_KEY = 'tukan_refresh_token'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  //const isAuthenticated = true
  async function restoreSession(): Promise<void> {

    const storedAccess = localStorage.getItem(ACCESS_TOKEN_KEY)
    const storedRefresh = localStorage.getItem(REFRESH_TOKEN_KEY)

    if (!storedAccess || !storedRefresh) return

    accessToken.value = storedAccess
    refreshToken.value = storedRefresh

  }

  function persistTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem(ACCESS_TOKEN_KEY, access)
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  }

  function clearSession() {
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  function clearError() {
    error.value = null
  }

  async function login(payload: LoginRequest): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const data = await authService.login(payload)
      persistTokens(data.accessToken, data.refreshToken)
      return data.nextStep
    } catch (err: any) {
      error.value = resolveErrorMessage(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterRequest): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const data = await authService.register(payload)
      persistTokens(data.accessToken, data.refreshToken)
      return data.nextStep
    } catch (err: any) {
      error.value = resolveErrorMessage(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    const token = refreshToken.value
    clearSession()
    if (token) {
      // Fire-and-forget: sessão já está limpa localmente
      authService.logout({ refreshToken: token }).catch(() => {})
    }
  }

  async function refresh(): Promise<boolean> {
    const token = refreshToken.value
    if (!token) return false
    try {
      const data = await authService.refresh({ refreshToken: token })
      persistTokens(data.accessToken, data.refreshToken)
      return true
    } catch {
      clearSession()
      return false
    }
  }

  return {
    accessToken,
    loading,
    error,
    isAuthenticated,
    restoreSession,
    clearError,
    login,
    register,
    logout,
    refresh,
  }
})

function resolveErrorMessage(err: any): string {
  const status = err.response?.status
  const serverMessage = err.response?.data?.message

  if (serverMessage) return serverMessage

  if (status === 401) return 'E-mail ou senha incorretos.'
  if (status === 409) return 'Este e-mail já está cadastrado.'
  if (status === 400 || status === 422) return 'Dados inválidos. Verifique as informações e tente novamente.'
  if (status === 500 || status === 503) return 'Serviço temporariamente indisponível. Tente novamente em breve.'

  return 'Ocorreu um erro inesperado. Tente novamente.'
}
