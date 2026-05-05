import axios from 'axios'
import router from '@/router';
import { useAuthStore } from '@/stores/auth';

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
// ... (Seu código existente do api.interceptors.request fica aqui em cima) ...

// Interceptor de Resposta: Vigia a volta do backend
api.interceptors.response.use(
  (response) => {
    // Se deu tudo certo (Status 2xx), só repassa a resposta
    return response;
  },
  (error) => {
    // Se deu erro, verifica se foi um problema de autenticação (Token expirado ou inválido)
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.warn("Sessão expirada ou acesso negado. Deslogando...");

      const authStore = useAuthStore();
      authStore.logout(); // Limpa o token do localStorage e o estado do Pinia

      // Redireciona o usuário para a página de Auth para logar de novo
      router.push('/auth');
    }

    // Repassa o erro para frente caso alguma tela precise tratar (ex: senha errada no login)
    return Promise.reject(error);
  }
);