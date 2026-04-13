package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.Assessment;
import jakarta.validation.constraints.Size;

public record UpdateTriagemRequest(

        @JsonProperty("objetivo")
        Assessment.NutritionalGoal goal,

        @JsonProperty("restricoesAlimentares")
        @Size(max = 500, message = "Restrições alimentares devem ter no máximo 500 caracteres.")
        String dietaryRestrictions,

        @JsonProperty("alergias")
        @Size(max = 500, message = "Alergias devem ter no máximo 500 caracteres.")
        String allergies,

        @JsonProperty("condicoesSaude")
        @Size(max = 500, message = "Condições de saúde devem ter no máximo 500 caracteres.")
        String healthConditions
) {
}