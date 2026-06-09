package com.tukan.api.dto.meallog;

import com.tukan.api.entity.MealLog;

import java.time.Instant;
import java.time.LocalDate;

public record MealLogResponse(
        Long id,
        Integer recommendationId,
        LocalDate mealDate,
        String mealType,
        Integer optionNumber,
        Double calories,
        Double protein,
        Double carbs,
        Double fat,
        String notes,
        Instant createdAt
) {
    public static MealLogResponse from(MealLog log) {
        return new MealLogResponse(
                log.getId(),
                log.getRecommendationId(),
                log.getMealDate(),
                log.getMealType(),
                log.getOptionNumber(),
                log.getCalories(),
                log.getProtein(),
                log.getCarbs(),
                log.getFat(),
                log.getNotes(),
                log.getCreatedAt()
        );
    }
}
