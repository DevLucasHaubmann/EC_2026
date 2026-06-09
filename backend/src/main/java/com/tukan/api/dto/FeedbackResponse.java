package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.RecommendationFeedback;

import java.time.Instant;
import java.util.List;

public record FeedbackResponse(
        Integer id,

        @JsonProperty("recommendationId")
        Integer recommendationId,

        @JsonProperty("rating")
        int rating,

        @JsonProperty("likedTags")
        List<RecommendationFeedback.FeedbackTag> likedTags,

        @JsonProperty("dislikedTags")
        List<RecommendationFeedback.FeedbackTag> dislikedTags,

        @JsonProperty("comment")
        String comment,

        @JsonProperty("createdAt")
        Instant createdAt,

        @JsonProperty("updatedAt")
        Instant updatedAt
) {

    public static FeedbackResponse from(RecommendationFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRecommendation().getId(),
                feedback.getRating(),
                feedback.getLikedTags(),
                feedback.getDislikedTags(),
                feedback.getComment(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
