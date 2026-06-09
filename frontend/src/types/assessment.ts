import type { Dashboard, DietType } from './api'

export interface CreateAssessmentRequest {
  goal: Dashboard.NutritionalGoal
  dietType?: DietType
  dietaryRestrictions?: string
  allergies?: string
  healthConditions?: string
  mealsPerDay?: number
  targetWeightKg?: number
}

export interface UpdateAssessmentRequest {
  goal?: Dashboard.NutritionalGoal
  dietType?: DietType
  dietaryRestrictions?: string
  allergies?: string
  healthConditions?: string
  mealsPerDay?: number
  targetWeightKg?: number
}

