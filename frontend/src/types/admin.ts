import type { Dashboard, DietType } from '@/types/api'

export interface AdminUpdateUserRequest {
  name?: string
  email?: string
  type?: 'ADMIN' | 'USER'
  status?: 'ACTIVE' | 'BLOCKED' | 'BANNED'
}

export interface AdminProfileRequest {
  dateOfBirth: string
  gender: Dashboard.Gender
  weightKg: number | string
  heightCm: number | string
  activityLevel: Dashboard.ActivityLevel
}

export interface AdminAssessmentRequest {
  goal: Dashboard.NutritionalGoal
  // Required by the backend contract (CreateAssessmentRequest/UpdateAssessmentRequest): the admin
  // flow must never send null for dietType or mealsPerDay.
  dietType: DietType
  dietaryRestrictions?: string | null
  allergies?: string | null
  healthConditions?: string | null
  mealsPerDay: number
  targetWeightKg?: number | null
}

export interface AdminUser {
  id: number
  name: string
  email: string
  type: 'ADMIN' | 'USER'
  status: 'ACTIVE' | 'BLOCKED' | 'BANNED'
  avatarUrl: string | null
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ProfileData {
  id: number
  userId: number
  userName: string
  dateOfBirth: string
  gender: Dashboard.Gender
  weightKg: number
  heightCm: number
  activityLevel: Dashboard.ActivityLevel
  createdAt: string
  updatedAt: string | null
}

export interface AssessmentData {
  id: number
  userId: number
  userName: string
  goal: Dashboard.NutritionalGoal
  dietType: DietType | null
  dietaryRestrictions: string | null
  allergies: string | null
  healthConditions: string | null
  mealsPerDay: number | null
  targetWeightKg: number | null
  createdAt: string
  updatedAt: string | null
}

export type TabState<T> = T | 'loading' | 'not-found' | 'error'
