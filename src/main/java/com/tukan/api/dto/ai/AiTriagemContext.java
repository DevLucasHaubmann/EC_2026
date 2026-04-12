package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiTriagemContext(
        @JsonProperty("objetivo")
        String goal,

        @JsonProperty("restricoesAlimentares")
        List<String> dietaryRestrictions,

        @JsonProperty("alergias")
        List<String> allergies,

        @JsonProperty("condicoesSaude")
        List<String> healthConditions
) {
}