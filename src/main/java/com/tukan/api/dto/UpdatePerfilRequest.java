package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.NutritionalProfile;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record UpdatePerfilRequest(

        @DecimalMin(value = "20.0", message = "O peso mínimo aceito é 20 kg.")
        @DecimalMax(value = "500.0", message = "O peso máximo aceito é 500 kg.")
        Double pesoKg,

        @DecimalMin(value = "50.0", message = "A altura mínima aceita é 50 cm.")
        @DecimalMax(value = "300.0", message = "A altura máxima aceita é 300 cm.")
        Double alturaCm,

        @JsonProperty("nivelAtividade")
        NutritionalProfile.ActivityLevel activityLevel
) {
}