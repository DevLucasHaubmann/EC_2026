package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealPlanMeal(
        @JsonProperty("mealType") String mealType,
        @JsonProperty("calorieTarget") double calorieTarget,
        @JsonProperty("options") List<MealOption> options
) {}
