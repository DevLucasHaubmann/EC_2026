package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MealPlanFoodItem(
        @JsonProperty("alimentoId") Integer foodId,
        @JsonProperty("nome") String name,
        @JsonProperty("nomeExibicao") String displayName,
        @JsonProperty("categoria") String category,
        @JsonProperty("porcaoG") BigDecimal portionGrams,
        @JsonProperty("calorias") BigDecimal calories,
        @JsonProperty("proteina") BigDecimal protein,
        @JsonProperty("carboidrato") BigDecimal carbs,
        @JsonProperty("gordura") BigDecimal fat,
        @JsonProperty("fibra") BigDecimal fiber
) {}
