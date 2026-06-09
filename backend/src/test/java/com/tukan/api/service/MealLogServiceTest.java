package com.tukan.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.meallog.LogMealRequest;
import com.tukan.api.dto.meallog.MealLogResponse;
import com.tukan.api.entity.MealLog;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.Recommendation.RecommendationStatus;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
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
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealLogServiceTest {

    @Mock
    private MealLogRepository mealLogRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserService userService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private MealLogService mealLogService;

    private static final String EMAIL = "user@example.com";
    private static final Integer USER_ID = 1;
    private static final Integer REC_ID = 10;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 8);

    private User user;
    private Recommendation recommendation;

    // Minimal valid plan_json with one meal type and two options
    private static final String VALID_PLAN_JSON = """
            {
              "dailyCalorieTarget": 2000,
              "goal": "WEIGHT_LOSS",
              "meals": [
                {
                  "mealType": "BREAKFAST",
                  "calorieTarget": 400,
                  "options": [
                    {
                      "optionNumber": 1,
                      "items": [],
                      "totalCalories": 380.0,
                      "totalProtein": 25.0,
                      "totalCarbs": 40.0,
                      "totalFat": 10.0
                    },
                    {
                      "optionNumber": 2,
                      "items": [],
                      "totalCalories": 420.0,
                      "totalProtein": 30.0,
                      "totalCarbs": 35.0,
                      "totalFat": 12.0
                    }
                  ]
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);

        recommendation = new Recommendation();
        recommendation.setId(REC_ID);
        recommendation.setUser(user);
        recommendation.setStatus(RecommendationStatus.GENERATED);
        recommendation.setPlanJson(VALID_PLAN_JSON);
        recommendation.setSummary("Test summary");
        recommendation.setContextJson("{}");
        recommendation.setProvider("gemini");
        recommendation.setModel("gemini-pro");
    }

    @Nested
    @DisplayName("logMeal")
    class LogMealTests {

        @Test
        @DisplayName("valid input persists and returns response")
        void logMeal_validInput_persistsAndReturnsResponse() {
            // given
            LogMealRequest request = new LogMealRequest(REC_ID, TODAY, "BREAKFAST", 1, "Sem açúcar");

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(recommendationRepository.findById(REC_ID)).thenReturn(Optional.of(recommendation));
            when(mealLogRepository.existsByUserIdAndMealDateAndMealType(USER_ID, TODAY, "BREAKFAST"))
                    .thenReturn(false);

            MealLog savedLog = new MealLog();
            savedLog.setId(100L);
            savedLog.setUser(user);
            savedLog.setRecommendationId(REC_ID);
            savedLog.setMealDate(TODAY);
            savedLog.setMealType("BREAKFAST");
            savedLog.setOptionNumber(1);
            savedLog.setCalories(380.0);
            savedLog.setProtein(25.0);
            savedLog.setCarbs(40.0);
            savedLog.setFat(10.0);
            savedLog.setNotes("Sem açúcar");
            savedLog.setCreatedAt(Instant.now());

            when(mealLogRepository.save(any(MealLog.class))).thenReturn(savedLog);

            // when
            MealLogResponse response = mealLogService.logMeal(EMAIL, request);

            // then
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.mealType()).isEqualTo("BREAKFAST");
            assertThat(response.calories()).isEqualTo(380.0);
            assertThat(response.protein()).isEqualTo(25.0);
            assertThat(response.notes()).isEqualTo("Sem açúcar");
            verify(mealLogRepository).save(any(MealLog.class));
        }

        @Test
        @DisplayName("recommendation belongs to other user throws 403")
        void logMeal_recommendationBelongsToOtherUser_throwsForbidden() {
            // given
            User otherUser = new User();
            otherUser.setId(999);
            recommendation.setUser(otherUser);

            LogMealRequest request = new LogMealRequest(REC_ID, TODAY, "BREAKFAST", 1, null);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(recommendationRepository.findById(REC_ID)).thenReturn(Optional.of(recommendation));

            // when / then
            assertThatThrownBy(() -> mealLogService.logMeal(EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));

            verify(mealLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("recommendation with ARCHIVED status throws 422")
        void logMeal_recommendationArchived_throwsUnprocessableEntity() {
            // given
            recommendation.setStatus(RecommendationStatus.ARCHIVED);
            LogMealRequest request = new LogMealRequest(REC_ID, TODAY, "BREAKFAST", 1, null);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(recommendationRepository.findById(REC_ID)).thenReturn(Optional.of(recommendation));

            // when / then
            assertThatThrownBy(() -> mealLogService.logMeal(EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("mealType not found in plan throws 422")
        void logMeal_mealTypeNotFoundInPlan_throwsUnprocessableEntity() {
            // given
            LogMealRequest request = new LogMealRequest(REC_ID, TODAY, "DINNER", 1, null);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(recommendationRepository.findById(REC_ID)).thenReturn(Optional.of(recommendation));
            when(mealLogRepository.existsByUserIdAndMealDateAndMealType(USER_ID, TODAY, "DINNER"))
                    .thenReturn(false);

            // when / then
            assertThatThrownBy(() -> mealLogService.logMeal(EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                    .hasMessageContaining("DINNER");
        }

        @Test
        @DisplayName("optionNumber not found in plan throws 422")
        void logMeal_optionNumberNotFoundInPlan_throwsUnprocessableEntity() {
            // given
            LogMealRequest request = new LogMealRequest(REC_ID, TODAY, "BREAKFAST", 99, null);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(recommendationRepository.findById(REC_ID)).thenReturn(Optional.of(recommendation));
            when(mealLogRepository.existsByUserIdAndMealDateAndMealType(USER_ID, TODAY, "BREAKFAST"))
                    .thenReturn(false);

            // when / then
            assertThatThrownBy(() -> mealLogService.logMeal(EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("duplicate for same date and mealType throws 409")
        void logMeal_duplicateForSameDateAndType_throwsConflict() {
            // given
            LogMealRequest request = new LogMealRequest(REC_ID, TODAY, "BREAKFAST", 1, null);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(recommendationRepository.findById(REC_ID)).thenReturn(Optional.of(recommendation));
            when(mealLogRepository.existsByUserIdAndMealDateAndMealType(USER_ID, TODAY, "BREAKFAST"))
                    .thenReturn(true);

            // when / then
            assertThatThrownBy(() -> mealLogService.logMeal(EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.CONFLICT));

            verify(mealLogRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getLogsByDate")
    class GetLogsByDateTests {

        @Test
        @DisplayName("valid date returns mapped responses")
        void getLogsByDate_validDate_returnsLogs() {
            // given
            MealLog log = new MealLog();
            log.setId(1L);
            log.setUser(user);
            log.setRecommendationId(REC_ID);
            log.setMealDate(TODAY);
            log.setMealType("BREAKFAST");
            log.setOptionNumber(1);
            log.setCalories(380.0);
            log.setProtein(25.0);
            log.setCarbs(40.0);
            log.setFat(10.0);
            log.setCreatedAt(Instant.now());

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDate(USER_ID, TODAY)).thenReturn(List.of(log));

            // when
            List<MealLogResponse> result = mealLogService.getLogsByDate(EMAIL, TODAY);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).mealType()).isEqualTo("BREAKFAST");
        }

        @Test
        @DisplayName("date with no logs returns empty list")
        void getLogsByDate_emptyDate_returnsEmptyList() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByUserIdAndMealDate(USER_ID, TODAY)).thenReturn(List.of());

            // when
            List<MealLogResponse> result = mealLogService.getLogsByDate(EMAIL, TODAY);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteLog")
    class DeleteLogTests {

        @Test
        @DisplayName("owner deletes own log successfully")
        void deleteLog_ownerDeletes_success() {
            // given
            MealLog log = new MealLog();
            log.setId(1L);
            log.setUser(user);

            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(log));

            // when
            mealLogService.deleteLog(EMAIL, 1L);

            // then
            verify(mealLogRepository).delete(log);
        }

        @Test
        @DisplayName("other user attempts to delete throws 404")
        void deleteLog_otherUserAttempts_throwsNotFound() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(mealLogRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> mealLogService.deleteLog(EMAIL, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            verify(mealLogRepository, never()).delete(any());
        }
    }
}
