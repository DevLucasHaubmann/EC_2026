package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DailyMealPlan(
        @JsonProperty("metaCaloricaDiaria") double dailyCalorieTarget,
        @JsonProperty("objetivo") String goal,
        @JsonProperty("refeicoes") List<MealPlanMeal> meals
) {}
