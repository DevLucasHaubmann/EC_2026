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
    SEDENTARIO: 'Sedentário',
    LEVE: 'Leve',
    MODERADO: 'Moderado',
    INTENSO: 'Intenso',
    MUITO_INTENSO: 'Muito Intenso',
  }
  return map[level] || level
}

export function formatNutritionalGoal(goal: string): string {
  const map: Record<string, string> = {
    PERDER_PESO: 'Perder Peso',
    MANTER_PESO: 'Manter Peso',
    GANHAR_PESO: 'Ganhar Peso',
  }
  return map[goal] || goal
}

export function formatGender(gender: string): string {
  const map: Record<string, string> = {
    MASCULINO: 'Masculino',
    FEMININO: 'Feminino',
    OUTRO: 'Outro',
  }
  return map[gender] || gender
}
