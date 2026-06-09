import { describe, it, expect } from 'vitest'
import { toggleArrayItem } from '../array'

describe('toggleArrayItem', () => {
  it('adds item when not present in array', () => {
    const items = ['a', 'b']
    toggleArrayItem(items, 'c')
    expect(items).toEqual(['a', 'b', 'c'])
  })

  it('removes item when already present in array', () => {
    const items = ['a', 'b', 'c']
    toggleArrayItem(items, 'b')
    expect(items).toEqual(['a', 'c'])
  })

  it('preserves other items when adding', () => {
    const items = ['gluten', 'lactose']
    toggleArrayItem(items, 'nozes')
    expect(items).toEqual(['gluten', 'lactose', 'nozes'])
  })

  it('preserves other items when removing', () => {
    const items = ['gluten', 'lactose', 'nozes']
    toggleArrayItem(items, 'lactose')
    expect(items).toEqual(['gluten', 'nozes'])
  })

  it('adds item to empty array', () => {
    const items: string[] = []
    toggleArrayItem(items, 'diabetes')
    expect(items).toEqual(['diabetes'])
  })

  it('mutates the original array in place and returns undefined', () => {
    const items = ['a']
    const result = toggleArrayItem(items, 'b')
    expect(result).toBeUndefined()
    expect(items).toEqual(['a', 'b'])
  })

  it('removes item when array has only that item', () => {
    const items = ['hipertensao']
    toggleArrayItem(items, 'hipertensao')
    expect(items).toEqual([])
  })
})
