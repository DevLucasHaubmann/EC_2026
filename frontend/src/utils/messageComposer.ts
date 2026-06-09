export function composeMessage(baseMessage: string, suffix: string): string {
  const normalized = baseMessage.trim()
  const normalizedSuffix = suffix.trim()
  if (!normalized) return normalizedSuffix
  if (!normalizedSuffix) return normalized
  const separator = /[.!?]$/.test(normalized) ? ' ' : '. '
  return `${normalized}${separator}${normalizedSuffix}`
}
