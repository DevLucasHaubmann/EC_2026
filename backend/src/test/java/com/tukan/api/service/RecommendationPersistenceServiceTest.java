package com.tukan.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealPlanContext;
import com.tukan.api.dto.mealplan.MealPlanRecommendationResponse;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.service.mealplan.MealPlanGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPersistenceServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RecommendationPersistenceService recommendationPersistenceService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("Lucas");
        user.setEmail("lucas@email.com");
    }

    private MealPlanContext sampleContext() {
        return new MealPlanContext(null, null, 2000, Map.of(), Map.of());
    }

    private MealPlanGenerationResult sampleResult() {
        DailyMealPlan plan = new DailyMealPlan(2000, "MAINTENANCE", Collections.emptyList());
        MealPlanRecommendationResponse response = new MealPlanRecommendationResponse(
                "COMPLETE",
                "Plano equilibrado.",
                plan,
                Map.of("BREAKFAST", "Café energético."),
                List.of("Beba água"),
                List.of("Consulte um nutricionista"),
                "gemini",
                "gemini-2.0-flash"
        );
        return new MealPlanGenerationResult(response, sampleContext());
    }

    private MealPlanGenerationResult fallbackResult() {
        DailyMealPlan plan = new DailyMealPlan(2000, "MAINTENANCE", Collections.emptyList());
        MealPlanRecommendationResponse response = new MealPlanRecommendationResponse(
                "PARTIAL",
                "Plano alimentar gerado com sucesso. O complemento da IA está temporariamente indisponível.",
                plan,
                Map.of(),
                Collections.emptyList(),
                Collections.emptyList(),
                "fallback",
                "none"
        );
        return new MealPlanGenerationResult(response, sampleContext());
    }

    private Recommendation existingRecommendation(Recommendation.RecommendationStatus status) {
        Recommendation rec = new Recommendation();
        rec.setId(99);
        rec.setUser(user);
        rec.setStatus(status);
        rec.setSummary("Resumo anterior");
        return rec;
    }

    @Test
    @DisplayName("should save and return recommendation with meal plan content")
    void shouldSaveAndReturnRecommendation() {
        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> {
            Recommendation r = inv.getArgument(0);
            r.setId(1);
            return r;
        });

        Recommendation result = recommendationPersistenceService.persist("lucas@email.com", sampleResult());

        assertThat(result.getSummary()).isEqualTo("Plano equilibrado.");
        assertThat(result.getPlanJson()).contains("MAINTENANCE");
        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.GENERATED);
        assertThat(result.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("should persist tips, alerts, meal explanations and context as JSON")
    void shouldPersistTipsAlertsMealExplanationsAndContext() {
        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        Recommendation result = recommendationPersistenceService.persist("lucas@email.com", sampleResult());

        assertThat(result.getTipsJson()).contains("Beba água");
        assertThat(result.getAlertsJson()).contains("Consulte um nutricionista");
        assertThat(result.getMealExplanationsJson()).contains("BREAKFAST");
        assertThat(result.getContextJson()).isNotBlank();
        assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.GENERATED);
    }

    @Test
    @DisplayName("should persist GENERATED status even when generation used fallback (PARTIAL)")
    void shouldPersistGeneratedStatusWhenFallback() {
        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        Recommendation result = recommendationPersistenceService.persist("lucas@email.com", fallbackResult());

        assertThat(result.getProvider()).isEqualTo("fallback");
        assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.GENERATED);
    }

    @Test
    @DisplayName("should archive GENERATED recommendation before saving the new one")
    void shouldArchivePreviousGeneratedRecommendation() {
        Recommendation existing = existingRecommendation(Recommendation.RecommendationStatus.GENERATED);

        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(List.of(existing));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        recommendationPersistenceService.persist("lucas@email.com", sampleResult());

        assertThat(existing.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
        verify(recommendationRepository).saveAll(List.of(existing));
    }

    @Test
    @DisplayName("should archive VIEWED recommendation before saving the new one")
    void shouldArchivePreviousViewedRecommendation() {
        Recommendation existing = existingRecommendation(Recommendation.RecommendationStatus.VIEWED);

        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(List.of(existing));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        recommendationPersistenceService.persist("lucas@email.com", sampleResult());

        assertThat(existing.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
        verify(recommendationRepository).saveAll(List.of(existing));
    }

    @Test
    @DisplayName("should archive all active recommendations when more than one exists")
    void shouldArchiveAllActiveRecommendations() {
        Recommendation generated = existingRecommendation(Recommendation.RecommendationStatus.GENERATED);
        Recommendation viewed = existingRecommendation(Recommendation.RecommendationStatus.VIEWED);
        List<Recommendation> active = List.of(generated, viewed);

        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(active);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        recommendationPersistenceService.persist("lucas@email.com", sampleResult());

        assertThat(generated.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
        assertThat(viewed.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
        verify(recommendationRepository).saveAll(active);
    }

    @Test
    @DisplayName("should call saveAll with empty list when there are no active recommendations")
    void shouldCallSaveAllWithEmptyListWhenNoActiveRecommendations() {
        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        recommendationPersistenceService.persist("lucas@email.com", sampleResult());

        verify(recommendationRepository).saveAll(Collections.emptyList());
    }

    @Test
    @DisplayName("should wrap serialization failure into AiProviderException")
    void shouldWrapSerializationFailure() throws JsonProcessingException {
        when(userService.findByEmail("lucas@email.com")).thenReturn(user);
        when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                .thenReturn(Collections.emptyList());
        doThrow(new JsonProcessingException("boom") {})
                .when(objectMapper).writeValueAsString(any(Object.class));

        assertThatThrownBy(() -> recommendationPersistenceService.persist("lucas@email.com", sampleResult()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("serializar");
    }
}
