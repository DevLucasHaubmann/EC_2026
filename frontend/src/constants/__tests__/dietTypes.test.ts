import { describe, it, expect } from 'vitest'
import type { DietType } from '@/types/api'
import { DIET_LABELS } from '@/constants/labels'
import {
  DIET_TYPE_OPTIONS,
  RESTRICTIVE_DIET_TYPES,
  isRestrictiveDiet,
} from '@/constants/assessmentOptions'

const ALL_DIET_TYPES: DietType[] = [
  'ONIVORA',
  'VEGETARIANA',
  'VEGANA',
  'PESCATARIANA',
  'FLEXITARIANA',
  'LOW_CARB',
  'CETOGENICA',
  'CARNIVORA',
]

describe('DIET_TYPE_OPTIONS', () => {
  it('has exactly 8 items', () => {
    // given the canonical set of 8 diet types
    // when counting DIET_TYPE_OPTIONS
    // then it must have exactly 8 entries
    expect(DIET_TYPE_OPTIONS).toHaveLength(8)
  })

  it('has ONIVORA as the first option', () => {
    // given DIET_TYPE_OPTIONS
    // when reading the first entry
    // then its value must be ONIVORA (default diet type)
    expect(DIET_TYPE_OPTIONS[0].value).toBe('ONIVORA')
  })

  it('contains all 8 canonical diet type values', () => {
    // given the canonical list of all 8 DietType values
    // when checking membership in DIET_TYPE_OPTIONS
    // then every value must be present
    const values = DIET_TYPE_OPTIONS.map((opt) => opt.value)
    for (const dietType of ALL_DIET_TYPES) {
      expect(values).toContain(dietType)
    }
  })

  it('derives every label from DIET_LABELS (single source of truth)', () => {
    // given DIET_TYPE_OPTIONS derived from DIET_LABELS
    // when comparing each opt.label with DIET_LABELS[opt.value]
    // then they must be identical — no duplicated string literals
    for (const opt of DIET_TYPE_OPTIONS) {
      expect(opt.label).toBe(DIET_LABELS[opt.value])
    }
  })

  it('has a defined non-empty label for every option', () => {
    // given all options
    // when inspecting each label
    // then none is empty or undefined
    for (const opt of DIET_TYPE_OPTIONS) {
      expect(opt.label).toBeTruthy()
    }
  })
})

describe('RESTRICTIVE_DIET_TYPES', () => {
  it('contains exactly LOW_CARB, CETOGENICA and CARNIVORA', () => {
    // given the set of restrictive diet types
    // when comparing its contents
    // then it must be exactly those 3 values
    expect(RESTRICTIVE_DIET_TYPES.size).toBe(3)
    expect(RESTRICTIVE_DIET_TYPES.has('LOW_CARB')).toBe(true)
    expect(RESTRICTIVE_DIET_TYPES.has('CETOGENICA')).toBe(true)
    expect(RESTRICTIVE_DIET_TYPES.has('CARNIVORA')).toBe(true)
  })

  it('does not include non-restrictive types', () => {
    // given the non-restrictive diet types
    // when checking membership in RESTRICTIVE_DIET_TYPES
    // then none of them is present
    const nonRestrictive: DietType[] = ['ONIVORA', 'VEGETARIANA', 'VEGANA', 'PESCATARIANA', 'FLEXITARIANA']
    for (const dietType of nonRestrictive) {
      expect(RESTRICTIVE_DIET_TYPES.has(dietType)).toBe(false)
    }
  })
})

describe('isRestrictiveDiet', () => {
  it('returns true for LOW_CARB', () => {
    // given a restrictive diet type
    // when calling isRestrictiveDiet
    // then it returns true
    expect(isRestrictiveDiet('LOW_CARB')).toBe(true)
  })

  it('returns true for CETOGENICA', () => {
    // given a restrictive diet type
    // when calling isRestrictiveDiet
    // then it returns true
    expect(isRestrictiveDiet('CETOGENICA')).toBe(true)
  })

  it('returns true for CARNIVORA', () => {
    // given a restrictive diet type
    // when calling isRestrictiveDiet
    // then it returns true
    expect(isRestrictiveDiet('CARNIVORA')).toBe(true)
  })

  it('returns false for non-restrictive types', () => {
    // given non-restrictive diet types
    // when calling isRestrictiveDiet for each
    // then all return false
    const nonRestrictive: DietType[] = ['ONIVORA', 'VEGETARIANA', 'VEGANA', 'PESCATARIANA', 'FLEXITARIANA']
    for (const dietType of nonRestrictive) {
      expect(isRestrictiveDiet(dietType)).toBe(false)
    }
  })

  it('returns false for null', () => {
    // given a null value (diet not set)
    // when calling isRestrictiveDiet
    // then it returns false without throwing
    expect(isRestrictiveDiet(null)).toBe(false)
  })

  it('returns false for undefined', () => {
    // given an undefined value
    // when calling isRestrictiveDiet
    // then it returns false without throwing
    expect(isRestrictiveDiet(undefined)).toBe(false)
  })
})
