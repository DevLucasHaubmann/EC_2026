import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

export const api = axios.create({
  baseURL: '/api',
})

// Injeta o access token em todas as requisições
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('tukan_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Serializa múltiplos refreshes simultâneos numa única requisição
let refreshPromise: Promise<void> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config

    const isAuthEndpoint = /\/auth\//.test(original?.url ?? '')
    const isUnauthorized = error.response?.status === 401

    if (!isUnauthorized || original._retry || isAuthEndpoint) {
      return Promise.reject(error)
    }

    original._retry = true

    const refreshToken = localStorage.getItem('tukan_refresh_token')
    if (!refreshToken) {
      redirectToAuth()
      return Promise.reject(error)
    }

    // Se já há um refresh em andamento, aguarda ele terminar antes de repetir
    if (!refreshPromise) {
      refreshPromise = axios
        .post('/api/auth/refresh', { refreshToken })
        .then(({ data }) => {
          localStorage.setItem('tukan_access_token', data.accessToken)
          localStorage.setItem('tukan_refresh_token', data.refreshToken)
          if (data.userType) {
            localStorage.setItem('tukan_user_type', data.userType)
            try {
              const authStore = useAuthStore()
              authStore.restoreSession()
            } catch {}
          }
        })
        .catch(() => {
          // Se falhar o refresh (ex: token de refresh venceu no back), tomba a sessão inteira
          redirectToAuth()
        })
        .finally(() => {
          refreshPromise = null
        })
    }

    try {
      await refreshPromise
      const newToken = localStorage.getItem('tukan_access_token')
      if (!newToken) return Promise.reject(error)
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch {
      return Promise.reject(error)
    }
  }
)

function redirectToAuth() {
  try {
    const authStore = useAuthStore()
    authStore.clearSession()
  } catch {
    localStorage.removeItem('tukan_access_token')
    localStorage.removeItem('tukan_refresh_token')
  }

  if (!window.location.pathname.startsWith('/auth')) {
    router.push({ name: 'auth' })
  }
}