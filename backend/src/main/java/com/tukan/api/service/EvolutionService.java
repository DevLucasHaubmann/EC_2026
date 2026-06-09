package com.tukan.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.evolution.DailyEvolutionMetrics;
import com.tukan.api.dto.evolution.WeeklyEvolutionSummary;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.entity.MealLog;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.User;
import com.tukan.api.repository.MealLogRepository;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvolutionService {

    private final MealLogRepository mealLogRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DailyEvolutionMetrics> getDailyMetrics(String email, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessException(
                    "Data inicial deve ser anterior ou igual à data final.", HttpStatus.BAD_REQUEST);
        }
        User user = userService.findByEmail(email);
        List<MealLog> logs = mealLogRepository.findByUserIdAndMealDateBetween(user.getId(), start, end);

        Map<LocalDate, List<MealLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(MealLog::getMealDate));

        // Batch-fetch all recommendations referenced by logs so each day uses the plan
        // that was active when those logs were created, not the current active one.
        // Avoids N+1: one batch query, then O(1) Map lookup per day.
        Set<Integer> referencedIds = logs.stream()
                .map(MealLog::getRecommendationId)
                .collect(Collectors.toSet());
        Map<Integer, Recommendation> recommendationById = referencedIds.isEmpty()
                ? Map.of()
                : recommendationRepository.findAllById(referencedIds).stream()
                        .collect(Collectors.toMap(Recommendation::getId, r -> r));

        List<DailyEvolutionMetrics> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            List<MealLog> dayLogs = logsByDate.getOrDefault(current, List.of());
            int plannedMeals = resolvePlannedMealsForDay(dayLogs, recommendationById);
            result.add(buildDailyMetrics(current, dayLogs, plannedMeals));
            current = current.plusDays(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public WeeklyEvolutionSummary getSummary(String email, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<DailyEvolutionMetrics> days = getDailyMetrics(email, weekStart, weekEnd);

        double totalCalories = days.stream()
                .mapToDouble(DailyEvolutionMetrics::consumedCalories)
                .sum();

        int activeDays = (int) days.stream()
                .filter(d -> d.completedMeals() >= 1)
                .count();

        Double averageAdherence = computeAverageAdherence(days);

        return new WeeklyEvolutionSummary(weekStart, weekEnd, days, averageAdherence, totalCalories, activeDays);
    }

    // --- private helpers ---

    private int resolvePlannedMealsForDay(List<MealLog> dayLogs,
                                           Map<Integer, Recommendation> recommendationById) {
        if (dayLogs.isEmpty()) {
            return 0; // no logs = no historical plan data for this day
        }
        // All logs for the same day should reference the same recommendation.
        // If they differ (edge case), the first log's recommendation is used.
        Integer recId = dayLogs.get(0).getRecommendationId();
        Recommendation rec = recommendationById.get(recId);
        if (rec == null) {
            log.warn("Recommendation id={} referenced by meal log not found; plannedMeals set to 0", recId);
            return 0;
        }
        return parsePlannedMealsCount(rec.getPlanJson());
    }

    private int parsePlannedMealsCount(String planJson) {
        try {
            DailyMealPlan plan = objectMapper.readValue(planJson, new TypeReference<DailyMealPlan>() {});
            return plan.meals() != null ? plan.meals().size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private DailyEvolutionMetrics buildDailyMetrics(LocalDate date, List<MealLog> dayLogs, int plannedMeals) {
        double consumedCalories = dayLogs.stream().mapToDouble(MealLog::getCalories).sum();
        double consumedProtein = dayLogs.stream().mapToDouble(MealLog::getProtein).sum();
        double consumedCarbs = dayLogs.stream().mapToDouble(MealLog::getCarbs).sum();
        double consumedFat = dayLogs.stream().mapToDouble(MealLog::getFat).sum();
        int completedMeals = dayLogs.size();

        Double adherencePercentage = computeDailyAdherence(completedMeals, plannedMeals);

        return new DailyEvolutionMetrics(
                date,
                consumedCalories,
                consumedProtein,
                consumedCarbs,
                consumedFat,
                completedMeals,
                plannedMeals,
                adherencePercentage
        );
    }

    private Double computeDailyAdherence(int completedMeals, int plannedMeals) {
        if (plannedMeals == 0) {
            return null;
        }
        double raw = (double) completedMeals / plannedMeals * 100.0;
        return Math.round(raw * 10.0) / 10.0;
    }

    private Double computeAverageAdherence(List<DailyEvolutionMetrics> days) {
        List<Double> validAdherences = days.stream()
                .filter(d -> d.plannedMeals() > 0 && d.adherencePercentage() != null)
                .map(DailyEvolutionMetrics::adherencePercentage)
                .toList();

        if (validAdherences.isEmpty()) {
            return null;
        }

        double sum = validAdherences.stream().mapToDouble(Double::doubleValue).sum();
        double raw = sum / validAdherences.size();
        return Math.round(raw * 10.0) / 10.0;
    }
}
