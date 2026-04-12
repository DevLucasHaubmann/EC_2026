package com.tukan.api.dto;

import com.tukan.api.entity.FeedbackRecomendacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(

        @NotNull(message = "A avaliação é obrigatória.")
        FeedbackRecomendacao.Avaliacao avaliacao,

        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres.")
        String motivo,

        @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres.")
        String observacao
) {
}
