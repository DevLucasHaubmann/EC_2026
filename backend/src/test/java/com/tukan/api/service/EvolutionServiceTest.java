package com.tukan.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.evolution.DailyEvolutionMetrics;
import com.tukan.api.dto.evolution.WeeklyEvolutionSummary;
import com.tukan.api.entity.MealLog;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.Recommendation.RecommendationStatus;
import com.tukan.api.entity.User;
import com.tukan.api.repository.MealLogRepository;
import com.tukan.api.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionServiceTest {

    @Mock
    private MealLogRepository mealLogRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserService userService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private EvolutionService evolutionService;

    private static final String EMAIL = "user@example.com";
    private static final Integer USER_ID = 1;
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 1);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 6, 7);

    // Plan with 3 meal types
    private static final String PLAN_3_MEALS_JSON = """
            {
              "dailyCalorieTarget": 2000,
              "goal": "WEIGHT_LOSS",
              "meals": [
                { "mealType": "BREAKFAST", "calorieTarget": 400, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 380.0,
                      "totalProtein": 25.0, "totalCarbs": 40.0, "totalFat": 10.0 }
                ]},
                { "mealType": "LUNCH", "calorieTarget": 700, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 680.0,
                      "totalProtein": 50.0, "totalCarbs": 70.0, "totalFat": 15.0 }
                ]},
                { "mealType": "DINNER", "calorieTarget": 600, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 580.0,
                      "totalProtein": 45.0, "totalCarbs": 55.0, "totalFat": 18.0 }
                ]}
              ]
            }
            """;

    // Plan with 5 meal types — used to represent a historical (possibly archived) recommendation
    private static final String PLAN_5_MEALS_JSON = """
            {
              "dailyCalorieTarget": 2200,
              "goal": "WEIGHT_GAIN",
              "meals": [
                { "mealType": "BREAKFAST",        "calorieTarget": 400, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 380.0,
                      "totalProtein": 25.0, "totalCarbs": 40.0, "totalFat": 10.0 }
                ]},
                { "mealType": "MORNING_SNACK",    "calorieTarget": 200, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 200.0,
                      "totalProtein": 10.0, "totalCarbs": 25.0, "totalFat": 5.0 }
                ]},
                { "mealType": "LUNCH",            "calorieTarget": 700, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 680.0,
                      "totalProtein": 50.0, "totalCarbs": 70.0, "totalFat": 15.0 }
                ]},
                { "mealType": "AFTERNOON_SNACK",  "calorieTarget": 200, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 200.0,
                      "totalProtein": 10.0, "totalCarbs": 25.0, "totalFat": 5.0 }
                ]},
                { "mealType": "DINNER",           "calorieTarget": 600, "options": [
                    { "optionNumber": 1, "items": [], "totalCalories": 580.0,
                      "totalProtein": 45.0, "totalCarbs": 55.0, "totalFat": 18.0 }
                ]}
              ]
            }
            """;

    private User user;
    private Recommendation activeRecommendation;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);

        activeRecommendation = new Recommendation();
        activeRecommendation.setId(10);
        activeRecommendation.setUser(user);
        activeRecommendation.setStatus(RecommendationStatus.GENERATED);
        activeRecommendation.setPlanJson(PLAN_3_MEALS_JSON);
        activeRecommendation.setSummary("Test");
        activeRecommendation.setContextJson("{}");
        activeRecommendation.setProvider("gemini");
        activeRecommendation.setModel("gemini-pro");
        activeRecommendation.setCreatedAt(Instant.now());
    }

    // --- helpers ---

    private MealLog buildLog(LocalDate date, String mealType, double calories,
                              double protein, double carbs, double fat) {
        MealLog log = new MealLog();
        log.setUser(user);
        log.setRecommendationId(10);
        log.setMealDate(date);
        log.setMealType(mealType);
        log.setOptionNumber(1);
        log.setCalories(calories);
        log.setProtein(protein);
        log.setCarbs(carbs);
        log.setFat(fat);
        log.setCreatedAt(Instant.now());
        return log;
    }

    @Nested
    @DisplayName("getDailyMetrics")
    class GetDailyMetricsTests {

        @Test
        @DisplayName("logs exist for a day — calculates correct adherence and macros")
        void getDailyMetrics_logsExist_calculatesCorrectAdherence() {
            // given — user logged 2 out of 3 planned meals on MONDAY
            MealLog breakfast = buildLog(MONDAY, "BREAKFAST", 380.0, 25.0, 40.0, 10.0);
            MealLog lunch = buildLog(MONDAY, "LUNCH", 680.0, 50.0, 70.0, 15.0);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDateBetween(USER_ID, MONDAY, MONDAY))
                    .thenReturn(List.of(breakfast, lunch));
            when(recommendationRepository.findAllById(anyIterable()))
                    .thenReturn(List.of(activeRecommendation));

            // when
            List<DailyEvolutionMetrics> result = evolutionService.getDailyMetrics(EMAIL, MONDAY, MONDAY);

            // then
            assertThat(result).hasSize(1);
            DailyEvolutionMetrics day = result.get(0);
            assertThat(day.date()).isEqualTo(MONDAY);
            assertThat(day.completedMeals()).isEqualTo(2);
            assertThat(day.plannedMeals()).isEqualTo(3);
            assertThat(day.consumedCalories()).isEqualTo(1060.0);
            assertThat(day.adherencePercentage()).isEqualTo(66.7);
        }

        @Test
        @DisplayName("no logs for a day — returns zero completed and null adherence (no historical plan data)")
        void getDailyMetrics_noLogsForDay_returnsNullAdherenceAndZeroPlannedMeals() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDateBetween(USER_ID, MONDAY, MONDAY))
                    .thenReturn(List.of());
            // No findByUserIdAndStatusIn stub: method is no longer called for days without logs

            // when
            List<DailyEvolutionMetrics> result = evolutionService.getDailyMetrics(EMAIL, MONDAY, MONDAY);

            // then
            DailyEvolutionMetrics day = result.get(0);
            assertThat(day.completedMeals()).isEqualTo(0);
            assertThat(day.plannedMeals()).isEqualTo(0);
            assertThat(day.consumedCalories()).isEqualTo(0.0);
            assertThat(day.adherencePercentage()).isNull();
        }

        @Test
        @DisplayName("day with log uses the recommendation referenced by the log, not the current active one")
        void getDailyMetrics_usesLogRecommendation_notCurrentActive() {
            // given — archived recommendation (id=20, 5 meals) was active when the log was created;
            // current active recommendation (id=10, 3 meals) must NOT be used for this historical day
            Recommendation archivedRec = new Recommendation();
            archivedRec.setId(20);
            archivedRec.setUser(user);
            archivedRec.setStatus(RecommendationStatus.ARCHIVED);
            archivedRec.setPlanJson(PLAN_5_MEALS_JSON);
            archivedRec.setSummary("Old plan");
            archivedRec.setContextJson("{}");
            archivedRec.setProvider("gemini");
            archivedRec.setModel("gemini-pro");
            archivedRec.setCreatedAt(Instant.now().minusSeconds(7200));

            MealLog log = buildLog(MONDAY, "BREAKFAST", 380.0, 25.0, 40.0, 10.0);
            log.setRecommendationId(20); // references the archived recommendation

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDateBetween(USER_ID, MONDAY, MONDAY))
                    .thenReturn(List.of(log));
            when(recommendationRepository.findAllById(anyIterable()))
                    .thenReturn(List.of(archivedRec));

            // when
            List<DailyEvolutionMetrics> result = evolutionService.getDailyMetrics(EMAIL, MONDAY, MONDAY);

            // then — plannedMeals must be 5 (archived rec), not 3 (current active rec)
            assertThat(result).hasSize(1);
            DailyEvolutionMetrics day = result.get(0);
            assertThat(day.plannedMeals()).isEqualTo(5);
            assertThat(day.completedMeals()).isEqualTo(1);
            // 1/5 * 100 = 20.0
            assertThat(day.adherencePercentage()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("no active recommendation — adherence is null for all days")
        void getDailyMetrics_noActiveRecommendation_adherenceNull() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDateBetween(USER_ID, MONDAY, MONDAY))
                    .thenReturn(List.of());

            // when
            List<DailyEvolutionMetrics> result = evolutionService.getDailyMetrics(EMAIL, MONDAY, MONDAY);

            // then
            DailyEvolutionMetrics day = result.get(0);
            assertThat(day.plannedMeals()).isEqualTo(0);
            assertThat(day.adherencePercentage()).isNull();
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummaryTests {

        @Test
        @DisplayName("multiple days — calculates average adherence and total calories")
        void getSummary_multipleDays_calculatesAverageAndTotals() {
            // given — Monday: 3/3 meals (100%), Tuesday: 1/3 meals (33.3%), rest: 0 logs
            MealLog mon1 = buildLog(MONDAY, "BREAKFAST", 380.0, 25.0, 40.0, 10.0);
            MealLog mon2 = buildLog(MONDAY, "LUNCH", 680.0, 50.0, 70.0, 15.0);
            MealLog mon3 = buildLog(MONDAY, "DINNER", 580.0, 45.0, 55.0, 18.0);
            LocalDate tuesday = MONDAY.plusDays(1);
            MealLog tue1 = buildLog(tuesday, "BREAKFAST", 380.0, 25.0, 40.0, 10.0);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDateBetween(USER_ID, MONDAY, SUNDAY))
                    .thenReturn(List.of(mon1, mon2, mon3, tue1));
            when(recommendationRepository.findAllById(anyIterable()))
                    .thenReturn(List.of(activeRecommendation));

            // when
            WeeklyEvolutionSummary summary = evolutionService.getSummary(EMAIL, MONDAY);

            // then
            assertThat(summary.weekStart()).isEqualTo(MONDAY);
            assertThat(summary.weekEnd()).isEqualTo(SUNDAY);
            assertThat(summary.days()).hasSize(7);
            assertThat(summary.activeDays()).isEqualTo(2);
            // Monday: 380+680+580=1640, Tuesday: 380, rest: 0
            assertThat(summary.totalCalories()).isEqualTo(2020.0);
            // Average adherence of days with plannedMeals>0: (100.0 + 33.3 + 0 + 0 + 0 + 0 + 0) / 7
            assertThat(summary.averageAdherencePercentage()).isNotNull();
        }

        @Test
        @DisplayName("no meals logged — activeDays 0, averageAdherence null (no log history)")
        void getSummary_noActiveDays_returnsNullAverageAdherence() {
            // given — no logs in the week; no fallback to active recommendation
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDateBetween(USER_ID, MONDAY, SUNDAY))
                    .thenReturn(List.of());

            // when
            WeeklyEvolutionSummary summary = evolutionService.getSummary(EMAIL, MONDAY);

            // then
            assertThat(summary.activeDays()).isEqualTo(0);
            assertThat(summary.totalCalories()).isEqualTo(0.0);
            assertThat(summary.averageAdherencePercentage()).isNull();
        }
    }
}
