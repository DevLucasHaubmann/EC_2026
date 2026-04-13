package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealPlanMeal(
        @JsonProperty("refeicao") String mealType,
        @JsonProperty("metaCalorica") double calorieTarget,
        @JsonProperty("opcoes") List<MealOption> options
) {}
