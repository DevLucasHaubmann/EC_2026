package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealOption(
        @JsonProperty("opcao") int optionNumber,
        @JsonProperty("itens") List<MealPlanFoodItem> items,
        @JsonProperty("totalCalorias") double totalCalories,
        @JsonProperty("totalProteina") double totalProtein,
        @JsonProperty("totalCarboidrato") double totalCarbs,
        @JsonProperty("totalGordura") double totalFat
) {}
