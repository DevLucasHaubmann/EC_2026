package com.tukan.api.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.dto.ai.AiPerfilContext;
import com.tukan.api.dto.ai.AiTriagemContext;

import java.util.List;
import java.util.Map;

/**
 * Contexto completo preparado pelo backend para a IA montar o plano alimentar.
 * Contém perfil, triagem, meta calórica e alimentos já filtrados por refeição.
 */
public record MealPlanContext(
        @JsonProperty("perfil") AiPerfilContext profile,
        @JsonProperty("triagem") AiTriagemContext assessment,
        @JsonProperty("metaCaloricaDiaria") double dailyCalorieTarget,
        @JsonProperty("distribuicaoRefeicoes") Map<String, Double> mealDistribution,
        @JsonProperty("alimentosPorRefeicao") Map<String, List<EligibleFoodSummary>> foodsByMeal
) {

    /**
     * Resumo compacto de um alimento elegível, para envio à IA
     * sem sobrecarregar o prompt com todos os campos da entidade.
     */
    public record EligibleFoodSummary(
            @JsonProperty("id") Integer id,
            @JsonProperty("nome") String name,
            @JsonProperty("categoria") String category,
            @JsonProperty("calorias100g") double caloriesPer100g,
            @JsonProperty("proteina100g") double proteinPer100g,
            @JsonProperty("carboidrato100g") double carbsPer100g,
            @JsonProperty("gordura100g") double fatPer100g,
            @JsonProperty("porcaoReferenciaG") double referencePortionGrams
    ) {}
}
