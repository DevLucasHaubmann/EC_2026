import { describe, it, expect } from 'vitest'
import type { MealType } from '@/types/api'
import { MEAL_LABELS } from '@/constants/labels'

const CANONICAL_MEAL_TYPES: MealType[] = [
  'BREAKFAST', 'MORNING_SNACK', 'LUNCH', 'AFTERNOON_SNACK', 'DINNER', 'SUPPER',
]

const RESTRICTIVE_MEAL_TYPES: MealType[] = [
  'LUNCH_LOW_CARB', 'DINNER_LOW_CARB',
  'LUNCH_CETOGENICA', 'DINNER_CETOGENICA', 'BREAKFAST_CETOGENICA',
  'LUNCH_CARNIVORA', 'DINNER_CARNIVORA', 'BREAKFAST_CARNIVORA',
]

const ALL_MEAL_TYPES: MealType[] = [...CANONICAL_MEAL_TYPES, ...RESTRICTIVE_MEAL_TYPES]

describe('MEAL_LABELS', () => {
  it('has a defined non-empty label for every canonical meal type', () => {
    // given canonical meal types
    // when checking MEAL_LABELS
    // then each one has a non-empty Portuguese label
    for (const type of CANONICAL_MEAL_TYPES) {
      expect(MEAL_LABELS[type], `Missing label for ${type}`).toBeTruthy()
    }
  })

  it('has a defined non-empty label for every restrictive/diet-specific meal type', () => {
    // given restrictive diet meal types that the backend can emit
    // when checking MEAL_LABELS
    // then each one has a non-empty Portuguese label (not the raw key)
    for (const type of RESTRICTIVE_MEAL_TYPES) {
      expect(MEAL_LABELS[type], `Missing label for ${type}`).toBeTruthy()
    }
  })

  it('LUNCH_LOW_CARB has human-readable label, not raw key', () => {
    expect(MEAL_LABELS['LUNCH_LOW_CARB']).toBe('Almoço low carb')
  })

  it('BREAKFAST_CETOGENICA has human-readable label', () => {
    expect(MEAL_LABELS['BREAKFAST_CETOGENICA']).toBe('Café cetogênico')
  })

  it('DINNER_CARNIVORA has human-readable label', () => {
    expect(MEAL_LABELS['DINNER_CARNIVORA']).toBe('Jantar carnívoro')
  })

  it('all 14 meal types have labels', () => {
    // given all 14 meal types (6 canonical + 8 diet-specific)
    // when counting MEAL_LABELS entries
    // then 14 entries exist
    expect(Object.keys(MEAL_LABELS)).toHaveLength(14)
  })

  it('no label is equal to its raw key (all are humanized)', () => {
    // given all meal type labels
    // when comparing label with its key
    // then no label equals the raw key string (confirms humanization)
    for (const type of ALL_MEAL_TYPES) {
      expect(MEAL_LABELS[type]).not.toBe(type)
    }
  })
})
