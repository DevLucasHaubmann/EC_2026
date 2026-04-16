package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record MealPlanAiResponse(
        @JsonProperty("summary") String summary,
        @JsonProperty("mealExplanations") Map<String, String> mealExplanations,
        @JsonProperty("tips") List<String> tips,
        @JsonProperty("alerts") List<String> alerts
) {}