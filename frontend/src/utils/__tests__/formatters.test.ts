import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { splitField, calcularIdade } from '../formatters'

describe('splitField', () => {
  it('returns empty array for empty string', () => {
    expect(splitField('')).toEqual([])
  })

  it('returns empty array for null', () => {
    expect(splitField(null)).toEqual([])
  })

  it('returns empty array for undefined', () => {
    expect(splitField(undefined)).toEqual([])
  })

  it('returns single-item array for string without comma', () => {
    expect(splitField('diabetes')).toEqual(['diabetes'])
  })

  it('splits comma-separated values', () => {
    expect(splitField('gluten,lactose,nozes')).toEqual(['gluten', 'lactose', 'nozes'])
  })

  it('trims whitespace around each item', () => {
    expect(splitField('gluten, lactose , nozes')).toEqual(['gluten', 'lactose', 'nozes'])
  })

  it('filters out empty item from trailing comma', () => {
    expect(splitField('gluten,lactose,')).toEqual(['gluten', 'lactose'])
  })

  it('filters out empty item from consecutive commas', () => {
    expect(splitField('gluten,,nozes')).toEqual(['gluten', 'nozes'])
  })

  it('filters out item that is only whitespace', () => {
    expect(splitField('gluten,   ,nozes')).toEqual(['gluten', 'nozes'])
  })
})

describe('calcularIdade', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns correct age when birthday already occurred this year', () => {
    vi.setSystemTime(new Date('2024-06-15'))
    expect(calcularIdade('1990-06-14')).toBe(34)
  })

  it('returns age minus one when birthday not yet occurred this year', () => {
    vi.setSystemTime(new Date('2024-06-15'))
    expect(calcularIdade('1990-06-16')).toBe(33)
  })

  it('returns correct age on the exact birthday', () => {
    vi.setSystemTime(new Date('2024-06-15'))
    expect(calcularIdade('1990-06-15')).toBe(34)
  })

  it('returns reduced age when birthday is in a future month', () => {
    vi.setSystemTime(new Date('2024-01-15'))
    expect(calcularIdade('1990-02-01')).toBe(33)
  })

  it('returns full age when birthday was in a past month', () => {
    vi.setSystemTime(new Date('2024-03-01'))
    expect(calcularIdade('1990-02-01')).toBe(34)
  })

  it('handles year-end boundary — birthday in December, today in January', () => {
    vi.setSystemTime(new Date('2024-01-01'))
    expect(calcularIdade('1990-12-31')).toBe(33)
  })

  it('returns negative number for future birth date', () => {
    vi.setSystemTime(new Date('2024-01-01'))
    expect(calcularIdade('2099-01-01')).toBe(-75)
  })

  it('returns NaN for invalid date string', () => {
    expect(calcularIdade('not-a-date')).toBeNaN()
  })
})
