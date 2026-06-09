import type { Dashboard, DietType, MealType } from '@/types/api'

export const GOAL_LABELS: Record<Dashboard.NutritionalGoal, string> = {
  WEIGHT_LOSS:         'Perda de peso',
  MUSCLE_GAIN:         'Ganho de massa',
  MAINTENANCE:         'Manutenção',
  DIETARY_REEDUCATION: 'Reeducação alimentar',
  SPORTS_PERFORMANCE:  'Performance esportiva',
}

export const ACTIVITY_LABELS: Record<Dashboard.ActivityLevel, string> = {
  SEDENTARY:    'Sedentário',
  LIGHT:        'Leve',
  MODERATE:     'Moderado',
  INTENSE:      'Intenso',
  VERY_INTENSE: 'Muito intenso',
}

export const DIET_LABELS: Record<DietType, string> = {
  ONIVORA:      'Onívora',
  VEGETARIANA:  'Vegetariana',
  VEGANA:       'Vegana',
  PESCATARIANA: 'Pescatariana',
  FLEXITARIANA: 'Flexitariana',
  LOW_CARB:     'Low carb',
  CETOGENICA:   'Cetogênica',
  CARNIVORA:    'Carnívora',
}

export const GENDER_LABELS: Record<Dashboard.Gender, string> = {
  MALE:   'Masculino',
  FEMALE: 'Feminino',
}

export const MEAL_LABELS: Record<MealType, string> = {
  BREAKFAST:            'Café da manhã',
  MORNING_SNACK:        'Lanche da manhã',
  LUNCH:                'Almoço',
  AFTERNOON_SNACK:      'Lanche da tarde',
  DINNER:               'Jantar',
  SUPPER:               'Ceia',
  LUNCH_LOW_CARB:       'Almoço low carb',
  DINNER_LOW_CARB:      'Jantar low carb',
  LUNCH_CETOGENICA:     'Almoço cetogênico',
  DINNER_CETOGENICA:    'Jantar cetogênico',
  BREAKFAST_CETOGENICA: 'Café cetogênico',
  LUNCH_CARNIVORA:      'Almoço carnívoro',
  DINNER_CARNIVORA:     'Jantar carnívoro',
  BREAKFAST_CARNIVORA:  'Café carnívoro',
}
