import { isAxiosError } from 'axios'
import { AUTH_MESSAGES, COMMON_MESSAGES } from '@/constants/messages'

export function resolveErrorMessage(err: unknown): string {
  if (!isAxiosError<{ message?: string }>(err)) return COMMON_MESSAGES.UNEXPECTED_ERROR

  const status = err.response?.status
  const serverMessage = err.response?.data?.message

  if (serverMessage) return serverMessage

  if (status === 401) return AUTH_MESSAGES.INVALID_CREDENTIALS
  if (status === 409) return AUTH_MESSAGES.EMAIL_EXISTS
  if (status === 400 || status === 422) return AUTH_MESSAGES.INVALID_DATA
  if (status === 500 || status === 503) return AUTH_MESSAGES.SERVICE_UNAVAILABLE

  return COMMON_MESSAGES.UNEXPECTED_ERROR
}
