package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.RecommendationFeedback;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(

        @JsonProperty("avaliacao")
        @NotNull(message = "A avaliação é obrigatória.")
        RecommendationFeedback.Rating rating,

        @JsonProperty("motivo")
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres.")
        String reason,

        @JsonProperty("observacao")
        @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres.")
        String observation
) {
}