package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record MealPlanAiResponse(
        @JsonProperty("resumo") String summary,
        @JsonProperty("explicacaoRefeicoes") Map<String, String> mealExplanations,
        @JsonProperty("dicas") List<String> tips,
        @JsonProperty("alertas") List<String> alerts
) {}