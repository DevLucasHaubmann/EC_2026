package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MealPlanFoodItem(
        @JsonProperty("foodId") Integer foodId,
        @JsonProperty("name") String name,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("category") String category,
        @JsonProperty("portionGrams") BigDecimal portionGrams,
        @JsonProperty("calories") BigDecimal calories,
        @JsonProperty("protein") BigDecimal protein,
        @JsonProperty("carbs") BigDecimal carbs,
        @JsonProperty("fat") BigDecimal fat,
        @JsonProperty("fiber") BigDecimal fiber
) {}
