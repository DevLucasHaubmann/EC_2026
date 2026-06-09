import { describe, it, expect } from 'vitest'
import { resolveErrorMessage } from '../resolveErrorMessage'
import { AUTH_MESSAGES, COMMON_MESSAGES } from '@/constants/messages'

const axiosError = (status?: number, message?: string) =>
  ({
    isAxiosError: true,
    response: { status, data: { message } },
  }) as unknown as import('axios').AxiosError<{ message?: string }>

describe('resolveErrorMessage', () => {
  it('non-Axios error returns UNEXPECTED_ERROR', () => {
    expect(resolveErrorMessage(new Error('generic'))).toBe(COMMON_MESSAGES.UNEXPECTED_ERROR)
  })

  it('non-Axios plain object returns UNEXPECTED_ERROR', () => {
    expect(resolveErrorMessage({ code: 'ECONNREFUSED' })).toBe(COMMON_MESSAGES.UNEXPECTED_ERROR)
  })

  it('Axios error with serverMessage returns serverMessage', () => {
    expect(resolveErrorMessage(axiosError(400, 'Campo obrigatório.'))).toBe('Campo obrigatório.')
  })

  it('Axios 401 without serverMessage returns INVALID_CREDENTIALS', () => {
    expect(resolveErrorMessage(axiosError(401))).toBe(AUTH_MESSAGES.INVALID_CREDENTIALS)
  })

  it('Axios 409 without serverMessage returns EMAIL_EXISTS', () => {
    expect(resolveErrorMessage(axiosError(409))).toBe(AUTH_MESSAGES.EMAIL_EXISTS)
  })

  it('Axios 400 without serverMessage returns INVALID_DATA', () => {
    expect(resolveErrorMessage(axiosError(400))).toBe(AUTH_MESSAGES.INVALID_DATA)
  })

  it('Axios 422 without serverMessage returns INVALID_DATA', () => {
    expect(resolveErrorMessage(axiosError(422))).toBe(AUTH_MESSAGES.INVALID_DATA)
  })

  it('Axios 500 without serverMessage returns SERVICE_UNAVAILABLE', () => {
    expect(resolveErrorMessage(axiosError(500))).toBe(AUTH_MESSAGES.SERVICE_UNAVAILABLE)
  })

  it('Axios 503 without serverMessage returns SERVICE_UNAVAILABLE', () => {
    expect(resolveErrorMessage(axiosError(503))).toBe(AUTH_MESSAGES.SERVICE_UNAVAILABLE)
  })

  it('Axios error with unknown status returns UNEXPECTED_ERROR', () => {
    expect(resolveErrorMessage(axiosError(404))).toBe(COMMON_MESSAGES.UNEXPECTED_ERROR)
  })

  it('Axios error without response returns UNEXPECTED_ERROR', () => {
    expect(resolveErrorMessage({ isAxiosError: true, response: undefined })).toBe(
      COMMON_MESSAGES.UNEXPECTED_ERROR,
    )
  })
})
