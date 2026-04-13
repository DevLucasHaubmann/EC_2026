package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.entity.Recommendation;

import java.time.Instant;
import java.util.List;

public record RecomendacaoResponse(
        Integer id,

        @JsonProperty("usuarioId")
        Integer userId,

        @JsonProperty("resumo")
        String summary,

        @JsonProperty("recomendacoes")
        List<String> recommendations,

        @JsonProperty("alertas")
        List<String> alerts,

        String provider,
        String model,

        Recommendation.RecommendationStatus status,

        @JsonProperty("criadoEm")
        Instant createdAt
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RecomendacaoResponse from(Recommendation recommendation) {
        return new RecomendacaoResponse(
                recommendation.getId(),
                recommendation.getUser().getId(),
                recommendation.getSummary(),
                parseJsonList(recommendation.getRecommendations()),
                parseJsonList(recommendation.getAlerts()),
                recommendation.getProvider(),
                recommendation.getModel(),
                recommendation.getStatus(),
                recommendation.getCreatedAt()
        );
    }

    private static List<String> parseJsonList(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Dado persistido em formato inválido. Valor: " + json, e);
        }
    }
}
