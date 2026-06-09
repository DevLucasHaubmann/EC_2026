import { describe, it, expect } from 'vitest'
import {
  resolveGenerationStatusMessage,
  DIET_GENERATION_STATUS_MESSAGES,
} from '../dietMessages'
import type { GenerationJob } from '@/types/api'

describe('resolveGenerationStatusMessage', () => {
  const cases: Array<[GenerationJob.Status, string]> = [
    ['PENDING', 'Preparando sua solicitação...'],
    ['PREPARING_CONTEXT', 'Analisando seu perfil nutricional...'],
    ['GENERATING_WITH_AI', 'Gerando sua recomendação com IA...'],
    ['SAVING_RECOMMENDATION', 'Salvando sua dieta...'],
    ['COMPLETED', 'Dieta gerada com sucesso.'],
    ['FAILED', 'Não foi possível gerar sua dieta agora.'],
  ]

  it.each(cases)('maps %s to its honest message', (status, expected) => {
    // given a real backend status
    // when resolving its user-facing message
    // then it returns the honest, percentage-free copy
    expect(resolveGenerationStatusMessage(status)).toBe(expected)
  })

  it('exposes exactly one message per real status', () => {
    // given the status->message map
    // when counting its keys
    // then every backend status is covered, with no extras
    expect(Object.keys(DIET_GENERATION_STATUS_MESSAGES)).toHaveLength(cases.length)
  })

  it('never exposes fake progress (no percentage, time or "step X of Y")', () => {
    // given every status message
    // when scanning for fabricated-progress markers
    // then none is present
    const forbidden = /%|\d+\s*(s|seg|segundo|min)|etapa\s*\d|passo\s*\d/i
    for (const message of Object.values(DIET_GENERATION_STATUS_MESSAGES)) {
      expect(message).not.toMatch(forbidden)
    }
  })
})
