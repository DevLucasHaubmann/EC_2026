package com.tukan.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealPlanContext;
import com.tukan.api.dto.mealplan.MealPlanRecommendationResponse;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.RecommendationFeedbackRepository;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.service.mealplan.MealPlanAiService;
import com.tukan.api.service.mealplan.MealPlanEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private MealPlanAiService mealPlanAiService;

    @Mock
    private MealPlanEngine mealPlanEngine;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiRecommendationService aiRecommendationService;

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

    private MealPlanRecommendationResponse sampleResponse() {
        DailyMealPlan plan = new DailyMealPlan(2000, "MAINTENANCE", Collections.emptyList());
        return new MealPlanRecommendationResponse(
                "COMPLETE",
                "Plano equilibrado.",
                plan,
                Map.of("BREAKFAST", "Café energético."),
                List.of("Beba água"),
                List.of("Consulte um nutricionista"),
                "gemini",
                "gemini-2.0-flash"
        );
    }

    private MealPlanRecommendationResponse fallbackResponse() {
        DailyMealPlan plan = new DailyMealPlan(2000, "MAINTENANCE", Collections.emptyList());
        return new MealPlanRecommendationResponse(
                "PARTIAL",
                "Plano alimentar gerado com sucesso. O complemento da IA está temporariamente indisponível.",
                plan,
                Map.of(),
                Collections.emptyList(),
                Collections.emptyList(),
                "fallback",
                "none"
        );
    }

    private Recommendation savedRecommendation(User owner, Recommendation.RecommendationStatus status) {
        Recommendation rec = new Recommendation();
        rec.setId(99);
        rec.setUser(owner);
        rec.setStatus(status);
        rec.setSummary("Resumo anterior");
        return rec;
    }

    @Nested
    @DisplayName("generateAndSave")
    class GenerateAndSave {

        @Test
        @DisplayName("should generate, save and return recommendation with meal plan")
        void shouldGenerateSaveAndReturnRecommendation() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                    .thenReturn(Collections.emptyList());
            when(mealPlanAiService.generate("lucas@email.com")).thenReturn(sampleResponse());
            when(mealPlanEngine.buildContext("lucas@email.com")).thenReturn(sampleContext());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> {
                Recommendation r = inv.getArgument(0);
                r.setId(1);
                return r;
            });

            Recommendation result = aiRecommendationService.generateAndSave("lucas@email.com");

            assertThat(result.getSummary()).isEqualTo("Plano equilibrado.");
            assertThat(result.getPlanJson()).contains("MAINTENANCE");
            assertThat(result.getProvider()).isEqualTo("gemini");
            assertThat(result.getModel()).isEqualTo("gemini-2.0-flash");
            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.GENERATED);
            assertThat(result.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("should persist tips, alerts and meal explanations as JSON")
        void shouldPersistTipsAlertsAndMealExplanations() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                    .thenReturn(Collections.emptyList());
            when(mealPlanAiService.generate("lucas@email.com")).thenReturn(sampleResponse());
            when(mealPlanEngine.buildContext("lucas@email.com")).thenReturn(sampleContext());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            Recommendation result = aiRecommendationService.generateAndSave("lucas@email.com");

            assertThat(result.getTipsJson()).contains("Beba água");
            assertThat(result.getAlertsJson()).contains("Consulte um nutricionista");
            assertThat(result.getMealExplanationsJson()).contains("BREAKFAST");
            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.GENERATED);
        }

        @Test
        @DisplayName("should persist PARTIAL status when generation uses fallback")
        void shouldPersistStatusGeneratedWhenFallback() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                    .thenReturn(Collections.emptyList());
            when(mealPlanAiService.generate("lucas@email.com")).thenReturn(fallbackResponse());
            when(mealPlanEngine.buildContext("lucas@email.com")).thenReturn(sampleContext());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            Recommendation result = aiRecommendationService.generateAndSave("lucas@email.com");

            assertThat(result.getProvider()).isEqualTo("fallback");
            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.GENERATED);
        }

        @Test
        @DisplayName("should archive GENERATED recommendation before generating a new one")
        void shouldArchivePreviousGeneratedRecommendationOnNewGeneration() {
            Recommendation existing = savedRecommendation(user, Recommendation.RecommendationStatus.GENERATED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                    .thenReturn(List.of(existing));
            when(mealPlanAiService.generate("lucas@email.com")).thenReturn(sampleResponse());
            when(mealPlanEngine.buildContext("lucas@email.com")).thenReturn(sampleContext());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            aiRecommendationService.generateAndSave("lucas@email.com");

            assertThat(existing.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
            verify(recommendationRepository).saveAll(List.of(existing));
        }

        @Test
        @DisplayName("should archive VIEWED recommendation before generating a new one")
        void shouldArchivePreviousViewedRecommendationOnNewGeneration() {
            Recommendation existing = savedRecommendation(user, Recommendation.RecommendationStatus.VIEWED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                    .thenReturn(List.of(existing));
            when(mealPlanAiService.generate("lucas@email.com")).thenReturn(sampleResponse());
            when(mealPlanEngine.buildContext("lucas@email.com")).thenReturn(sampleContext());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            aiRecommendationService.generateAndSave("lucas@email.com");

            assertThat(existing.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
            verify(recommendationRepository).saveAll(List.of(existing));
        }

        @Test
        @DisplayName("should not call saveAll when there are no active recommendations to archive")
        void shouldNotCallSaveAllWhenNoActiveRecommendationsExist() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdAndStatusIn(eq(1), anyList()))
                    .thenReturn(Collections.emptyList());
            when(mealPlanAiService.generate("lucas@email.com")).thenReturn(sampleResponse());
            when(mealPlanEngine.buildContext("lucas@email.com")).thenReturn(sampleContext());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            aiRecommendationService.generateAndSave("lucas@email.com");

            verify(recommendationRepository).saveAll(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("findLatest")
    class FindLatest {

        @Test
        @DisplayName("should return the latest recommendation for the user")
        void shouldReturnLatestRecommendation() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setSummary("Resumo");

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findFirstByUserIdOrderByCreatedAtDesc(1))
                    .thenReturn(Optional.of(recommendation));

            Recommendation result = aiRecommendationService.findLatest("lucas@email.com");

            assertThat(result.getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw BusinessException when no recommendation exists")
        void shouldThrowWhenNoRecommendationFound() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findFirstByUserIdOrderByCreatedAtDesc(1))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.findLatest("lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Nenhuma recomendação encontrada");
        }
    }

    @Nested
    @DisplayName("findAllByUser")
    class FindAllByUser {

        @Test
        @DisplayName("should return all recommendations ordered by creation date descending")
        void shouldReturnAllRecommendationsForUser() {
            Recommendation r1 = new Recommendation();
            r1.setId(1);
            r1.setUser(user);

            Recommendation r2 = new Recommendation();
            r2.setId(2);
            r2.setUser(user);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1))
                    .thenReturn(List.of(r2, r1));

            List<Recommendation> result = aiRecommendationService.findAllByUser("lucas@email.com");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when user has no recommendations")
        void shouldReturnEmptyListWhenNoRecommendations() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1))
                    .thenReturn(Collections.emptyList());

            List<Recommendation> result = aiRecommendationService.findAllByUser("lucas@email.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return recommendation when it belongs to the user")
        void shouldReturnRecommendationByOwner() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(10);
            recommendation.setUser(user);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(10, 1)).thenReturn(Optional.of(recommendation));

            Recommendation result = aiRecommendationService.findById(10, "lucas@email.com");

            assertThat(result.getId()).isEqualTo(10);
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation does not belong to the user")
        void shouldThrowWhenRecommendationDoesNotBelongToUser() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(10, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.findById(10, "lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    @Nested
    @DisplayName("markAsViewed")
    class MarkAsViewed {

        @Test
        @DisplayName("should transition GENERATED recommendation to VIEWED")
        void shouldTransitionGeneratedToViewed() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            Recommendation result = aiRecommendationService.markAsViewed(1, "lucas@email.com");

            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
            verify(recommendationRepository).save(recommendation);
        }

        @Test
        @DisplayName("should return recommendation unchanged when already VIEWED (idempotent)")
        void shouldReturnUnchangedWhenAlreadyViewed() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));

            Recommendation result = aiRecommendationService.markAsViewed(1, "lucas@email.com");

            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
            verify(recommendationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation is ARCHIVED")
        void shouldThrowWhenRecommendationIsArchived() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.ARCHIVED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));

            assertThatThrownBy(() -> aiRecommendationService.markAsViewed(1, "lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("arquivadas");
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation does not belong to user")
        void shouldThrowWhenOwnershipFails() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(99, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.markAsViewed(99, "lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    @Nested
    @DisplayName("addFeedback")
    class AddFeedback {

        @Test
        @DisplayName("should register feedback and transition GENERATED to VIEWED")
        void shouldRegisterFeedbackAndTransitionGeneratedToViewed() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);

            FeedbackRequest request = new FeedbackRequest(
                    RecommendationFeedback.Rating.LIKED, "Útil", "Vou seguir");

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.existsByRecommendationId(1)).thenReturn(false);
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(recommendationFeedbackRepository.save(any(RecommendationFeedback.class))).thenAnswer(inv -> {
                RecommendationFeedback f = inv.getArgument(0);
                f.setId(1);
                return f;
            });

            RecommendationFeedback result = aiRecommendationService.addFeedback(1, "lucas@email.com", request);

            assertThat(result.getRating()).isEqualTo(RecommendationFeedback.Rating.LIKED);
            assertThat(result.getReason()).isEqualTo("Útil");
            assertThat(result.getObservation()).isEqualTo("Vou seguir");
            assertThat(recommendation.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
            verify(recommendationRepository).save(recommendation);
        }

        @Test
        @DisplayName("should register feedback without changing status when already VIEWED")
        void shouldRegisterFeedbackWithoutChangingStatusWhenAlreadyViewed() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);

            FeedbackRequest request = new FeedbackRequest(
                    RecommendationFeedback.Rating.LIKED, null, null);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.existsByRecommendationId(1)).thenReturn(false);
            when(recommendationFeedbackRepository.save(any(RecommendationFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

            aiRecommendationService.addFeedback(1, "lucas@email.com", request);

            assertThat(recommendation.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
            verify(recommendationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should register feedback without changing status when ARCHIVED")
        void shouldRegisterFeedbackWithoutChangingStatusWhenArchived() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.ARCHIVED);

            FeedbackRequest request = new FeedbackRequest(
                    RecommendationFeedback.Rating.DISLIKED, "Não gostei", null);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.existsByRecommendationId(1)).thenReturn(false);
            when(recommendationFeedbackRepository.save(any(RecommendationFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

            aiRecommendationService.addFeedback(1, "lucas@email.com", request);

            assertThat(recommendation.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
            verify(recommendationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when feedback already exists")
        void shouldThrowWhenFeedbackAlreadyExists() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);

            FeedbackRequest request = new FeedbackRequest(
                    RecommendationFeedback.Rating.DISLIKED, null, null);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.existsByRecommendationId(1)).thenReturn(true);

            assertThatThrownBy(() -> aiRecommendationService.addFeedback(1, "lucas@email.com", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já possui feedback");
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation does not belong to user")
        void shouldThrowWhenOwnershipFailsOnFeedback() {
            FeedbackRequest request = new FeedbackRequest(
                    RecommendationFeedback.Rating.LIKED, null, null);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(99, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.addFeedback(99, "lucas@email.com", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("should archive VIEWED recommendation successfully")
        void shouldArchiveViewedRecommendation() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            Recommendation result = aiRecommendationService.archive(1, "lucas@email.com");

            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should archive GENERATED recommendation successfully")
        void shouldArchiveGeneratedRecommendation() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            Recommendation result = aiRecommendationService.archive(1, "lucas@email.com");

            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should throw BusinessException when already archived")
        void shouldThrowWhenAlreadyArchived() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.ARCHIVED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));

            assertThatThrownBy(() -> aiRecommendationService.archive(1, "lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já está arquivada");
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation does not belong to user")
        void shouldThrowWhenOwnershipFailsOnArchive() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(99, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.archive(99, "lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não encontrada");
        }
    }
}
