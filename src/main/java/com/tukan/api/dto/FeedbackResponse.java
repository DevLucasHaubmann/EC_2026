package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.RecommendationFeedback;

import java.time.Instant;

public record FeedbackResponse(
        Integer id,

        @JsonProperty("recomendacaoId")
        Integer recommendationId,

        @JsonProperty("avaliacao")
        RecommendationFeedback.Rating rating,

        @JsonProperty("motivo")
        String reason,

        @JsonProperty("observacao")
        String observation,

        @JsonProperty("criadoEm")
        Instant createdAt
) {

    public static FeedbackResponse from(RecommendationFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRecommendation().getId(),
                feedback.getRating(),
                feedback.getReason(),
                feedback.getObservation(),
                feedback.getCreatedAt()
        );
    }
}
