package com.tukan.api.service.mealplan;

import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealPlanContext;

/**
 * Deterministic data prepared before the external AI call.
 * Produced inside a short read-only transaction and consumed by the AI
 * enrichment phase, which runs without holding a database connection.
 */
public record MealPlanPreparation(
        DailyMealPlan plan,
        MealPlanContext context
) {}
