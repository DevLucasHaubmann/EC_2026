package com.tukan.api.dto;

import com.tukan.api.entity.Triagem;

import java.time.Instant;

public record TriagemResponse(
        Integer id,
        Integer usuarioId,
        String usuarioNome,
        Triagem.ObjetivoNutricional objetivo,
        String restricoesAlimentares,
        String alergias,
        String condicoesSaude,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static TriagemResponse from(Triagem triagem) {
        return new TriagemResponse(
                triagem.getId(),
                triagem.getUsuario().getId(),
                triagem.getUsuario().getNome(),
                triagem.getObjetivo(),
                triagem.getRestricoesAlimentares(),
                triagem.getAlergias(),
                triagem.getCondicoesSaude(),
                triagem.getCriadoEm(),
                triagem.getAtualizadoEm()
        );
    }
}
