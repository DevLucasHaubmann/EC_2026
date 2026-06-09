import type { Dashboard } from './api'

export interface CreateProfileRequest {
  dateOfBirth: string
  gender: Dashboard.Gender
  weightKg: number
  heightCm: number
  activityLevel: Dashboard.ActivityLevel
}

export interface UpdateProfileRequest {
  weightKg?: number
  heightCm?: number
  activityLevel?: Dashboard.ActivityLevel
}

