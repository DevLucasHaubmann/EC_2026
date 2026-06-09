/**
 * Returns true only for absolute http/https URLs with a non-empty host.
 * Rejects javascript:, data:, file:, protocol-relative URLs, and empty strings.
 */
export function isSafeAvatarUrl(url: string): boolean {
  if (!url || url.trim() === '') return false
  try {
    const parsed = new URL(url)
    return (parsed.protocol === 'http:' || parsed.protocol === 'https:') && parsed.host !== ''
  } catch {
    return false
  }
}

/**
 * Returns 2-letter initials derived from a display name or email.
 * - Name with ≥2 words → first letter of first word + first letter of last word (uppercase).
 * - Name with 1 word → first letter of that word (uppercase).
 * - No name → first letter of email (uppercase).
 * - Fallback → "?".
 */
export function getInitials(name?: string, email?: string): string {
  const trimmedName = name?.trim()
  if (trimmedName) {
    const words = trimmedName.split(/\s+/).filter(Boolean)
    if (words.length >= 2) {
      return (words[0]![0]! + words[words.length - 1]![0]!).toUpperCase()
    }
    return words[0]![0]!.toUpperCase()
  }
  const trimmedEmail = email?.trim()
  if (trimmedEmail) {
    return trimmedEmail[0]!.toUpperCase()
  }
  return '?'
}
