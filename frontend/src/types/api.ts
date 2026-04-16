/**
 * Dashboard & Auth Types
 */

export namespace Dashboard {
  export interface ProfileSummary {
    weightKg: number
    heightCm: number
    bmi: number
    bmiClassification: string
    activityLevel: ActivityLevel
    gender: Gender
    age: number
  }

  export interface AssessmentSummary {
    goal: NutritionalGoal
    hasRestrictions: boolean
    hasAllergies: boolean
    hasHealthConditions: boolean
  }

  export interface OnboardingStatus {
    onboardingRequired: boolean
    nextStep: string
  }

  export interface Response {
    name: string
    profile: ProfileSummary | null
    assessment: AssessmentSummary | null
    onboarding: OnboardingStatus
  }

  export type ActivityLevel = 'SEDENTARIO' | 'LEVE' | 'MODERADO' | 'INTENSO' | 'MUITO_INTENSO'
  export type Gender = 'MASCULINO' | 'FEMININO' | 'OUTRO'
  export type NutritionalGoal = 'PERDER_PESO' | 'MANTER_PESO' | 'GANHAR_PESO'
}

export namespace Recommendation {
  export interface DailyMealPlan {
    dailyCalorieTarget: number
    goal: string
    meals: MealPlanMeal[]
  }

  export interface MealPlanMeal {
    mealType: string
    calorieTarget: number
    options: MealOption[]
  }

  export interface MealOption {
    optionNumber: number
    items: MealPlanFoodItem[]
    totalCalories: number
    totalProtein: number
    totalCarbs: number
    totalFat: number
  }

  export interface MealPlanFoodItem {
    foodId: number
    name: string
    displayName: string
    category: string
    portionGrams: number
    calories: number
    protein: number
    carbs: number
    fat: number
    fiber: number
  }

  export interface Response {
    id: number
    userId: number
    summary: string
    plan: DailyMealPlan
    mealExplanations: Record<string, string>
    tips: string[]
    alerts: string[]
    provider: string
    model: string
    status: 'GENERATED' | 'VIEWED' | 'ARCHIVED'
    createdAt: string
  }
}
