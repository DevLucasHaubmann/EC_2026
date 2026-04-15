package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DailyMealPlan(
        @JsonProperty("dailyCalorieTarget") double dailyCalorieTarget,
        @JsonProperty("goal") String goal,
        @JsonProperty("meals") List<MealPlanMeal> meals
) {}
