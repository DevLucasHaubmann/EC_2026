import { describe, it, expect } from 'vitest'
import { isSafeAvatarUrl, getInitials } from '@/utils/avatarUrl'

// ── isSafeAvatarUrl ───────────────────────────────────────────────────────────

describe('isSafeAvatarUrl', () => {
  it('accepts a standard https URL', () => {
    expect(isSafeAvatarUrl('https://example.com/avatar.png')).toBe(true)
  })

  it('accepts a standard http URL', () => {
    expect(isSafeAvatarUrl('http://example.com/avatar.jpg')).toBe(true)
  })

  it('accepts a URL with path and query params', () => {
    expect(isSafeAvatarUrl('https://cdn.example.com/users/42/avatar.webp?v=3')).toBe(true)
  })

  it('rejects a javascript: URL (XSS vector)', () => {
    expect(isSafeAvatarUrl('javascript:alert(1)')).toBe(false)
  })

  it('rejects a data: URL (potential XSS vector)', () => {
    expect(isSafeAvatarUrl('data:image/png;base64,abc123')).toBe(false)
  })

  it('rejects an empty string', () => {
    expect(isSafeAvatarUrl('')).toBe(false)
  })

  it('rejects a plain filename without protocol', () => {
    expect(isSafeAvatarUrl('avatar.png')).toBe(false)
  })

  it('rejects a URL with an empty host (e.g. just the protocol)', () => {
    expect(isSafeAvatarUrl('https://')).toBe(false)
  })

  it('rejects a ftp: URL (not in the http/https whitelist)', () => {
    expect(isSafeAvatarUrl('ftp://files.example.com/pic.png')).toBe(false)
  })

  it('rejects a completely random string', () => {
    expect(isSafeAvatarUrl('not-a-url-at-all')).toBe(false)
  })
})

// ── getInitials ───────────────────────────────────────────────────────────────

describe('getInitials', () => {
  it('returns first and last initial from a two-word name', () => {
    expect(getInitials('Lucas Haubmann')).toBe('LH')
  })

  it('returns first and last initial from a multi-word name', () => {
    expect(getInitials('Maria das Dores Santos')).toBe('MS')
  })

  it('returns a single uppercase letter for a one-word name', () => {
    expect(getInitials('Lucas')).toBe('L')
  })

  it('returns initials in uppercase', () => {
    expect(getInitials('alice bob')).toBe('AB')
  })

  it('falls back to first char of email when name is empty string', () => {
    expect(getInitials('', 'user@example.com')).toBe('U')
  })

  it('falls back to first char of email when name is undefined', () => {
    expect(getInitials(undefined, 'test@tukan.app')).toBe('T')
  })

  it('returns "?" when both name and email are undefined', () => {
    expect(getInitials(undefined, undefined)).toBe('?')
  })

  it('returns "?" when both name and email are empty strings', () => {
    expect(getInitials('', '')).toBe('?')
  })

  it('trims leading/trailing whitespace in the name before deriving initials', () => {
    expect(getInitials('  Ana Clara  ')).toBe('AC')
  })

  it('email fallback is uppercase', () => {
    expect(getInitials(undefined, 'zoey@example.com')).toBe('Z')
  })
})
