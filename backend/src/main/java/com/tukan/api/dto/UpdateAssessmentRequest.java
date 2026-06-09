package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.Assessment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateAssessmentRequest(

        @JsonProperty("goal")
        Assessment.NutritionalGoal goal,

        @JsonProperty("dietType")
        Assessment.DietType dietType,

        @JsonProperty("dietaryRestrictions")
        @Size(max = 500, message = "Restrições alimentares devem ter no máximo 500 caracteres.")
        String dietaryRestrictions,

        @JsonProperty("allergies")
        @Size(max = 500, message = "Alergias devem ter no máximo 500 caracteres.")
        String allergies,

        @JsonProperty("healthConditions")
        @Size(max = 500, message = "Condições de saúde devem ter no máximo 500 caracteres.")
        String healthConditions,

        @JsonProperty("mealsPerDay")
        @Min(value = 3, message = "Número de refeições deve ser entre 3 e 5.")
        @Max(value = 5, message = "Número de refeições deve ser entre 3 e 5.")
        Integer mealsPerDay,

        @JsonProperty("targetWeightKg")
        @Positive(message = "Peso alvo deve ser positivo.")
        @DecimalMax(value = "500.0", message = "Peso alvo não pode exceder 500 kg.")
        Double targetWeightKg
) {
}