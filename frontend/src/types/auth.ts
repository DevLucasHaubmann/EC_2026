/**
 * Auth Types
 * Mirrors backend DTOs: LoginRequest, RegisterRequest, RefreshRequest, AuthResponse
 */

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface RefreshRequest {
  refreshToken: string
}

export type UserType = 'ADMIN' | 'USER'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  type: string
  expiresIn: number
  message: string
  onboardingRequired: boolean
  nextStep: string
  userType: UserType
}
