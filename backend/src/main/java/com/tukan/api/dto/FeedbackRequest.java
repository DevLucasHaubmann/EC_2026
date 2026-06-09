package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.RecommendationFeedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FeedbackRequest(

        @JsonProperty("rating")
        @NotNull(message = "A avaliação é obrigatória.")
        @Min(value = 1, message = "A avaliação deve ser entre 1 e 5.")
        @Max(value = 5, message = "A avaliação deve ser entre 1 e 5.")
        Integer rating,

        @JsonProperty("likedTags")
        List<RecommendationFeedback.FeedbackTag> likedTags,

        @JsonProperty("dislikedTags")
        List<RecommendationFeedback.FeedbackTag> dislikedTags,

        @JsonProperty("comment")
        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.")
        String comment
) {}
