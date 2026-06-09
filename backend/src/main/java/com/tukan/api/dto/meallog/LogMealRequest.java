package com.tukan.api.dto.meallog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LogMealRequest(
        @NotNull Integer recommendationId,
        @NotNull LocalDate mealDate,
        @NotBlank String mealType,
        @NotNull @Min(1) Integer optionNumber,
        String notes
) {}
