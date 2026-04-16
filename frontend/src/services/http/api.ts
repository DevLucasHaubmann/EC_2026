import axios from 'axios'

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
    const is401 = error.response?.status === 401

    if (!is401 || original._retry || isAuthEndpoint) {
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
        })
        .catch(() => {
          localStorage.removeItem('tukan_access_token')
          localStorage.removeItem('tukan_refresh_token')
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
  // Evita redirect duplicado se já estiver na página de auth
  if (!window.location.pathname.startsWith('/auth')) {
    window.location.href = '/auth'
  }
}
