import { describe, it, expect } from 'vitest'
import { composeMessage } from '../messageComposer'

describe('composeMessage', () => {

  // Grupo 1 — cenários do AdminUserModal (erro de exclusão de perfil)
  it('baseMessage com ponto final: adiciona espaço antes do suffix', () => {
    expect(composeMessage('Erro ao excluir perfil.', 'O perfil não foi excluído.')).toBe(
      'Erro ao excluir perfil. O perfil não foi excluído.'
    )
  })

  it('baseMessage sem ponto final: adiciona ". " antes do suffix', () => {
    expect(composeMessage('Erro ao excluir perfil', 'O perfil não foi excluído.')).toBe(
      'Erro ao excluir perfil. O perfil não foi excluído.'
    )
  })

  it('baseMessage vazia: retorna apenas o suffix', () => {
    expect(composeMessage('', 'O perfil não foi excluído.')).toBe('O perfil não foi excluído.')
  })

  // Grupo 2 — cenários do AdminUserModal (triagem já removida)
  it('baseMessage com ponto: adiciona espaço antes do suffix de triagem', () => {
    expect(
      composeMessage('Erro ao excluir triagem vinculada.', 'A triagem já foi removida. Recarregando dados.')
    ).toBe('Erro ao excluir triagem vinculada. A triagem já foi removida. Recarregando dados.')
  })

  it('baseMessage sem ponto: adiciona ". " antes do suffix de triagem', () => {
    expect(
      composeMessage('Erro ao excluir triagem vinculada', 'A triagem já foi removida. Recarregando dados.')
    ).toBe('Erro ao excluir triagem vinculada. A triagem já foi removida. Recarregando dados.')
  })

  it('baseMessage vazia com suffix de triagem: retorna apenas o suffix', () => {
    expect(composeMessage('', 'A triagem já foi removida. Recarregando dados.')).toBe(
      'A triagem já foi removida. Recarregando dados.'
    )
  })

  // Grupo 3 — edge cases gerais
  it('baseMessage com apenas espaços: retorna apenas o suffix', () => {
    expect(composeMessage('   ', 'suffix')).toBe('suffix')
  })

  it('suffix vazio: retorna apenas a baseMessage normalizada', () => {
    expect(composeMessage('Algum erro.', '')).toBe('Algum erro.')
  })

  it('suffix com apenas espaços: retorna apenas a baseMessage normalizada', () => {
    expect(composeMessage('Algum erro.', '   ')).toBe('Algum erro.')
  })

  it('baseMessage terminando com "!": adiciona espaço antes do suffix', () => {
    expect(composeMessage('Falha!', 'Tente novamente.')).toBe('Falha! Tente novamente.')
  })

  it('baseMessage terminando com "?": adiciona espaço antes do suffix', () => {
    expect(composeMessage('Erro?', 'Verifique.')).toBe('Erro? Verifique.')
  })
})
