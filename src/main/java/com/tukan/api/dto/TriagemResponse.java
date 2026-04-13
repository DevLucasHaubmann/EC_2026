package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.Assessment;

import java.time.Instant;

public record TriagemResponse(
        Integer id,

        @JsonProperty("usuarioId")
        Integer userId,

        @JsonProperty("usuarioNome")
        String userName,

        @JsonProperty("objetivo")
        Assessment.NutritionalGoal goal,

        @JsonProperty("restricoesAlimentares")
        String dietaryRestrictions,

        @JsonProperty("alergias")
        String allergies,

        @JsonProperty("condicoesSaude")
        String healthConditions,

        @JsonProperty("criadoEm")
        Instant createdAt,

        @JsonProperty("atualizadoEm")
        Instant updatedAt
) {

    public static TriagemResponse from(Assessment assessment) {
        return new TriagemResponse(
                assessment.getId(),
                assessment.getUser().getId(),
                assessment.getUser().getName(),
                assessment.getGoal(),
                assessment.getDietaryRestrictions(),
                assessment.getAllergies(),
                assessment.getHealthConditions(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
        );
    }
}