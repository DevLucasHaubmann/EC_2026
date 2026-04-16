package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.dto.ai.AiProfileContext;
import com.tukan.api.dto.ai.AiAssessmentContext;

import java.util.List;
import java.util.Map;

/**
 * Contexto completo preparado pelo backend para a IA montar o plano alimentar.
 * Contém perfil, triagem, meta calórica e alimentos já filtrados por refeição.
 */
public record MealPlanContext(
        @JsonProperty("profile") AiProfileContext profile,
        @JsonProperty("assessment") AiAssessmentContext assessment,
        @JsonProperty("dailyCalorieTarget") double dailyCalorieTarget,
        @JsonProperty("mealDistribution") Map<String, Double> mealDistribution,
        @JsonProperty("foodsByMeal") Map<String, List<EligibleFoodSummary>> foodsByMeal
) {

    /**
     * Resumo compacto de um alimento elegível, para envio à IA
     * sem sobrecarregar o prompt com todos os campos da entidade.
     */
    public record EligibleFoodSummary(
            @JsonProperty("id") Integer id,
            @JsonProperty("name") String name,
            @JsonProperty("category") String category,
            @JsonProperty("caloriesPer100g") double caloriesPer100g,
            @JsonProperty("proteinPer100g") double proteinPer100g,
            @JsonProperty("carbsPer100g") double carbsPer100g,
            @JsonProperty("fatPer100g") double fatPer100g,
            @JsonProperty("referencePortionGrams") double referencePortionGrams
    ) {}
}
