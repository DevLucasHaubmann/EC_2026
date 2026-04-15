package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealOption(
        @JsonProperty("optionNumber") int optionNumber,
        @JsonProperty("items") List<MealPlanFoodItem> items,
        @JsonProperty("totalCalories") double totalCalories,
        @JsonProperty("totalProtein") double totalProtein,
        @JsonProperty("totalCarbs") double totalCarbs,
        @JsonProperty("totalFat") double totalFat
) {}
