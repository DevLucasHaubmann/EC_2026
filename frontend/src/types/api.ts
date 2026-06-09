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

  export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'INTENSE' | 'VERY_INTENSE'
  export type Gender = 'MALE' | 'FEMALE'
  export type NutritionalGoal = 'WEIGHT_LOSS' | 'MUSCLE_GAIN' | 'MAINTENANCE' | 'DIETARY_REEDUCATION' | 'SPORTS_PERFORMANCE'
}

export type DietType =
  | 'ONIVORA'
  | 'VEGETARIANA'
  | 'VEGANA'
  | 'PESCATARIANA'
  | 'FLEXITARIANA'
  | 'LOW_CARB'
  | 'CETOGENICA'
  | 'CARNIVORA'
export type MealType =
  | 'BREAKFAST'
  | 'MORNING_SNACK'
  | 'LUNCH'
  | 'AFTERNOON_SNACK'
  | 'DINNER'
  | 'SUPPER'
  | 'LUNCH_LOW_CARB'
  | 'DINNER_LOW_CARB'
  | 'LUNCH_CETOGENICA'
  | 'DINNER_CETOGENICA'
  | 'BREAKFAST_CETOGENICA'
  | 'LUNCH_CARNIVORA'
  | 'DINNER_CARNIVORA'
  | 'BREAKFAST_CARNIVORA'

export namespace Assessment {
  export interface Response {
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
}

export interface MeProfileResponse {
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

export interface MeResponse {
  id: number
  name: string
  email: string
  type: 'ADMIN' | 'USER'
  status: 'ACTIVE' | 'BLOCKED' | 'BANNED'
  avatarUrl: string | null
  profile: MeProfileResponse | null
  assessment: Assessment.Response | null
}

export interface ProfileCreatedResponse {
  profile: MeProfileResponse
  onboardingRequired: boolean
  nextStep: string
}

export interface AssessmentCreatedResponse {
  assessment: Assessment.Response
  onboardingRequired: boolean
  nextStep: string
}

export namespace MealLog {
  export interface LogRequest {
    recommendationId: number
    mealDate: string        // ISO date: YYYY-MM-DD
    mealType: string        // MealType canônico
    optionNumber: number
    notes?: string
  }

  export interface Response {
    id: number
    recommendationId: number
    mealDate: string
    mealType: string
    optionNumber: number
    calories: number
    protein: number
    carbs: number
    fat: number
    notes: string | null
    createdAt: string
  }
}

export namespace Evolution {
  export interface DailyMetrics {
    date: string
    consumedCalories: number
    consumedProtein: number
    consumedCarbs: number
    consumedFat: number
    completedMeals: number
    plannedMeals: number
    adherencePercentage: number | null
  }

  export interface WeeklySummary {
    weekStart: string
    weekEnd: string
    days: DailyMetrics[]
    averageAdherencePercentage: number | null
    totalCalories: number
    activeDays: number
  }
}

export namespace GenerationJob {
  // Real, observable phases of the async diet-generation job. No fake percentage,
  // no time estimate — mirrors the backend GenerationJobStatus enum.
  export type Status =
    | 'PENDING'
    | 'PREPARING_CONTEXT'
    | 'GENERATING_WITH_AI'
    | 'SAVING_RECOMMENDATION'
    | 'COMPLETED'
    | 'FAILED'

  // Shared shape across start-job (202/409), status-fallback and SSE `job-update` payload.
  // recommendationId is only present on COMPLETED; errorMessage only on FAILED.
  export interface Response {
    jobId: string
    status: Status
    recommendationId: number | null
    errorMessage: string | null
  }

  // Result of starting a job: created=true on HTTP 202, false when reattaching to an
  // already-active job (HTTP 409).
  export interface StartResult {
    job: Response
    created: boolean
  }
}

export namespace BodyWeight {
  export interface LogResponse {
    id: number
    weightKg: number
    measuredDate: string // YYYY-MM-DD
    createdAt: string
  }

  export interface SummaryResponse {
    currentWeightKg: number | null
    initialWeightKg: number | null
    totalChangeKg: number | null
    trend: 'LOSS' | 'GAIN' | 'STABLE' | null
    entryCount: number
    history: LogResponse[]
  }
}

export namespace Activity {
  export interface StreakResponse {
    currentStreak: number
    longestStreak: number
    totalActiveDays: number
    lastActiveDate: string | null
  }
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

  export type FeedbackTag =
    | 'PRACTICAL'
    | 'VARIED'
    | 'AFFORDABLE'
    | 'BALANCED'
    | 'TASTY'
    | 'TOO_RESTRICTIVE'
    | 'REPETITIVE'
    | 'EXPENSIVE'
    | 'LACKING_PROTEIN'
    | 'EASY_TO_PREPARE'
    | 'OTHER'

  export interface FeedbackRequest {
    rating: number
    likedTags?: FeedbackTag[]
    dislikedTags?: FeedbackTag[]
    comment?: string
  }

  export interface FeedbackResponse {
    id: number
    recommendationId: number
    rating: number
    likedTags: FeedbackTag[]
    dislikedTags: FeedbackTag[]
    comment: string | null
    createdAt: string
    updatedAt: string
  }
}
