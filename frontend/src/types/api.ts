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

  export type ActivityLevel = 'SEDENTARY' | 'MODERATE' | 'INTENSE'
  export type Gender = 'MALE' | 'FEMALE'
  export type NutritionalGoal = 'WEIGHT_LOSS' | 'MUSCLE_GAIN' | 'MAINTENANCE' | 'DIETARY_REEDUCATION' | 'SPORTS_PERFORMANCE'
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
