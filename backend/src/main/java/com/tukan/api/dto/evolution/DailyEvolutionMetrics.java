package com.tukan.api.dto.evolution;

import java.time.LocalDate;

public record DailyEvolutionMetrics(
        LocalDate date,
        Double consumedCalories,
        Double consumedProtein,
        Double consumedCarbs,
        Double consumedFat,
        Integer completedMeals,
        Integer plannedMeals,
        Double adherencePercentage  // null when plannedMeals == 0 (no active recommendation for that day)
) {}
