package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.entity.Recommendation;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record RecommendationResponse(
        Integer id,

        @JsonProperty("userId")
        Integer userId,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("plan")
        DailyMealPlan plan,

        @JsonProperty("mealExplanations")
        Map<String, String> mealExplanations,

        @JsonProperty("tips")
        List<String> tips,

        @JsonProperty("alerts")
        List<String> alerts,

        String provider,
        String model,

        @JsonProperty("generationStatus")
        String generationStatus,

        Recommendation.RecommendationStatus status,

        @JsonProperty("createdAt")
        Instant createdAt
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RecommendationResponse from(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getUser().getId(),
                recommendation.getSummary(),
                parseJson(recommendation.getPlanJson(), new TypeReference<DailyMealPlan>() {}),
                parseJson(recommendation.getMealExplanationsJson(), new TypeReference<Map<String, String>>() {}),
                parseJsonOrEmpty(recommendation.getTipsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()),
                parseJsonOrEmpty(recommendation.getAlertsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()),
                recommendation.getProvider(),
                recommendation.getModel(),
                recommendation.getGenerationStatus(),
                recommendation.getStatus(),
                recommendation.getCreatedAt()
        );
    }

    private static <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Dado persistido em formato inválido. Valor: " + json, e);
        }
    }

    private static <T> T parseJsonOrEmpty(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.isBlank()) return defaultValue;
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Dado persistido em formato inválido. Valor: " + json, e);
        }
    }
}
