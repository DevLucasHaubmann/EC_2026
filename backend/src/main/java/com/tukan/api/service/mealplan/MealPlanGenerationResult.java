package com.tukan.api.service.mealplan;

import com.tukan.api.dto.mealplan.MealPlanContext;
import com.tukan.api.dto.mealplan.MealPlanRecommendationResponse;

/**
 * Resultado interno de uma geração de plano alimentar.
 * Carrega o response público e o contexto da engine para evitar reexecução.
 */
public record MealPlanGenerationResult(
        MealPlanRecommendationResponse response,
        MealPlanContext context
) {}
