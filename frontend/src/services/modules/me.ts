import { api } from '../http/api'

export const meService = {
  // Busca as informações do usuário logado e seu status de onboarding
  getMe: async () => {
    const res = await api.get('/me')
    return res.data
  }
}