package com.tukan.api.dto;

import com.tukan.api.entity.FeedbackRecomendacao;

import java.time.Instant;

public record FeedbackResponse(
        Integer id,
        Integer recomendacaoId,
        FeedbackRecomendacao.Avaliacao avaliacao,
        String motivo,
        String observacao,
        Instant criadoEm
) {

    public static FeedbackResponse from(FeedbackRecomendacao feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRecomendacao().getId(),
                feedback.getAvaliacao(),
                feedback.getMotivo(),
                feedback.getObservacao(),
                feedback.getCriadoEm()
        );
    }
}
