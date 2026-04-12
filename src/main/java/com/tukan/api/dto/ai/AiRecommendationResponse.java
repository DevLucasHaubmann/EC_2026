package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRecommendationResponse(
        @JsonProperty("resumo")
        String summary,

        @JsonProperty("recomendacoes")
        List<String> recommendations,

        @JsonProperty("alertas")
        List<String> alerts
) {
}