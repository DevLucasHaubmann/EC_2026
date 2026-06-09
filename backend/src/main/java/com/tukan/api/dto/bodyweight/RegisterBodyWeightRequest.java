package com.tukan.api.dto.bodyweight;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterBodyWeightRequest(

        @NotNull(message = "O peso é obrigatório.")
        @DecimalMin(value = "20.0", message = "O peso deve ser no mínimo 20 kg.")
        @DecimalMax(value = "500.0", message = "O peso deve ser no máximo 500 kg.")
        @Digits(integer = 3, fraction = 2, message = "O peso deve ter no máximo 2 casas decimais.")
        BigDecimal weightKg
) {
}
