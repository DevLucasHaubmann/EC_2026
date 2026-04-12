package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPerfilContext(
        @JsonProperty("sexo")
        String gender,

        @JsonProperty("idade")
        int age,

        @JsonProperty("pesoKg")
        double weightKg,

        @JsonProperty("alturaCm")
        double heightCm,

        @JsonProperty("nivelAtividade")
        String activityLevel
) {
}