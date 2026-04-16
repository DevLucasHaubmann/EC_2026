/**
 * Auth Service
 * Handles all authentication-related API calls
 */

import { api } from '../http/api'
import type { LoginRequest, RegisterRequest, RefreshRequest, AuthResponse } from '@/types/auth'

class AuthService {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', payload)
    return response.data
  }

  async register(payload: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', payload)
    return response.data
  }

  async refresh(payload: RefreshRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/refresh', payload)
    return response.data
  }

  async logout(payload: RefreshRequest): Promise<void> {
    await api.post('/auth/logout', payload)
  }
}

export const authService = new AuthService()
