import { ACTIVITY_LABELS } from '@/constants/labels'

export function formatActivityLevel(level: string): string {
  return (ACTIVITY_LABELS as Record<string, string>)[level] ?? level
}

export function calcularIdade(dateOfBirth: string): number {
  const nasc = new Date(dateOfBirth)
  const hoje = new Date()
  let anos = hoje.getFullYear() - nasc.getFullYear()
  const m = hoje.getMonth() - nasc.getMonth()
  if (m < 0 || (m === 0 && hoje.getDate() < nasc.getDate())) anos--
  return anos
}

export function splitField(raw: string | null | undefined): string[] {
  if (!raw) return []
  return raw.split(',').map(s => s.trim()).filter(Boolean)
}
