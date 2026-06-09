import type { Dashboard, DietType } from '@/types/api'
import { DIET_LABELS } from './labels'

export type NutritionalGoalOption = {
  value: Dashboard.NutritionalGoal
  label: string
  desc?: string
}

export const HEALTH_CONDITION_OPTIONS: string[] = [
  'Diabetes Tipo 1', 'Diabetes Tipo 2', 'Hipertensão', 'Colesterol Alto',
  'Gastrite', 'Anemia', 'Hipotireoidismo', 'Hipertireoidismo', 'Síndrome do Intestino Irritável',
]

export const ALLERGY_OPTIONS: string[] = [
  'Glúten', 'Lactose', 'Frutos do Mar', 'Amendoim', 'Ovo',
  'Soja', 'Proteína do Leite (APLV)', 'Trigo', 'Nozes',
]

export const NUTRITIONAL_GOAL_OPTIONS: NutritionalGoalOption[] = [
  { value: 'WEIGHT_LOSS',         label: 'Emagrecimento',        desc: 'Redução de gordura com déficit calórico.' },
  { value: 'MUSCLE_GAIN',         label: 'Ganho de Massa',        desc: 'Aumento de volume muscular com superávit.' },
  { value: 'MAINTENANCE',         label: 'Manutenção',            desc: 'Manter o peso e a composição atual.' },
  { value: 'DIETARY_REEDUCATION', label: 'Reeducação Alimentar',  desc: 'Melhora dos hábitos alimentares.' },
  { value: 'SPORTS_PERFORMANCE',  label: 'Performance Esportiva', desc: 'Otimização para rendimento atlético.' },
]

export type ActivityLevelOption = { value: Dashboard.ActivityLevel; label: string }

export const ACTIVITY_LEVEL_OPTIONS: ActivityLevelOption[] = [
  { value: 'SEDENTARY',    label: 'Sedentário (pouco ou nenhum exercício)' },
  { value: 'LIGHT',        label: 'Leve (1–3x por semana)' },
  { value: 'MODERATE',     label: 'Moderado (3–5x por semana)' },
  { value: 'INTENSE',      label: 'Intenso (treino diário)' },
  { value: 'VERY_INTENSE', label: 'Muito intenso (2x por dia ou trabalho físico)' },
]

export type DietTypeOption = { value: DietType; label: string }

const DIET_TYPE_ORDER: DietType[] = [
  'ONIVORA',
  'VEGETARIANA',
  'VEGANA',
  'PESCATARIANA',
  'FLEXITARIANA',
  'LOW_CARB',
  'CETOGENICA',
  'CARNIVORA',
]

export const DIET_TYPE_OPTIONS: DietTypeOption[] = DIET_TYPE_ORDER.map((value) => ({
  value,
  label: DIET_LABELS[value],
}))

export const RESTRICTIVE_DIET_TYPES: ReadonlySet<DietType> = new Set<DietType>([
  'LOW_CARB',
  'CETOGENICA',
  'CARNIVORA',
])

export function isRestrictiveDiet(dietType: DietType | null | undefined): boolean {
  return dietType != null && RESTRICTIVE_DIET_TYPES.has(dietType)
}
