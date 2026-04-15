package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.RecommendationFeedback;

import java.time.Instant;

public record FeedbackResponse(
        Integer id,

        @JsonProperty("recommendationId")
        Integer recommendationId,

        @JsonProperty("rating")
        RecommendationFeedback.Rating rating,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("observation")
        String observation,

        @JsonProperty("createdAt")
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
