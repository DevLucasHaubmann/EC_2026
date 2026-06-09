package com.tukan.api.dto.evolution;

import java.time.LocalDate;
import java.util.List;

public record WeeklyEvolutionSummary(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<DailyEvolutionMetrics> days,
        Double averageAdherencePercentage,  // average of days where plannedMeals > 0; null if none
        Double totalCalories,
        Integer activeDays  // days with completedMeals >= 1
) {}
