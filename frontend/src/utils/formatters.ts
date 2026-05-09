/**
 * Utility Functions - Formatters
 * Date, number, currency formatting helpers
 */

export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleDateString('pt-BR')
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)
}

export function formatWeight(kg: number): string {
  return `${kg.toFixed(1)} kg`
}

export function formatBMI(bmi: number): string {
  return `${bmi.toFixed(1)}`
}

export function formatActivityLevel(level: string): string {
  const map: Record<string, string> = {
    SEDENTARY: 'Sedentário',
    LIGHT: 'Leve',
    MODERATE: 'Moderado',
    INTENSE: 'Intenso',
    VERY_INTENSE: 'Muito intenso',
  }
  return map[level] ?? level
}

export function formatNutritionalGoal(goal: string): string {
  const map: Record<string, string> = {
    WEIGHT_LOSS: 'Perda de peso',
    MUSCLE_GAIN: 'Ganho de massa',
    MAINTENANCE: 'Manutenção',
    DIETARY_REEDUCATION: 'Reeducação alimentar',
    SPORTS_PERFORMANCE: 'Performance esportiva',
  }
  return map[goal] ?? goal
}

export function formatGender(gender: string): string {
  const map: Record<string, string> = {
    MALE: 'Masculino',
    FEMALE: 'Feminino',
  }
  return map[gender] ?? gender
}
