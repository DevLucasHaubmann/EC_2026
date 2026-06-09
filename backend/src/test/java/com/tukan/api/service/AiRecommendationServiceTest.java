package com.tukan.api.service;

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
import com.tukan.api.service.mealplan.MealPlanGenerationResult;
import com.tukan.api.service.mealplan.MealPlanPreparation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private MealPlanReadinessValidator readinessValidator;

    @Mock
    private MealPlanAiService mealPlanAiService;

    @Mock
    private RecommendationPersistenceService recommendationPersistenceService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

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

    private MealPlanPreparation samplePreparation() {
        return new MealPlanPreparation(
                new DailyMealPlan(2000, "MAINTENANCE", Collections.emptyList()), sampleContext());
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

        // Orchestration only. The transactional boundaries of each phase (prepare =
        // read-only, persist = write) and the absence of a transaction during enrich
        // are enforced by Spring proxies and verified by review, not by unit tests.

        @Test
        @DisplayName("should orchestrate prepare, enrich and persist and return the persisted recommendation")
        void shouldOrchestratePhasesAndReturnPersistedRecommendation() {
            MealPlanPreparation preparation = samplePreparation();
            MealPlanGenerationResult result = sampleResult();
            Recommendation persisted = savedRecommendation(user, Recommendation.RecommendationStatus.GENERATED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(mealPlanAiService.prepare("lucas@email.com")).thenReturn(preparation);
            when(mealPlanAiService.enrich(preparation)).thenReturn(result);
            when(recommendationPersistenceService.persist("lucas@email.com", result)).thenReturn(persisted);

            Recommendation returned = aiRecommendationService.generateAndSave("lucas@email.com");

            assertThat(returned).isSameAs(persisted);
        }

        @Test
        @DisplayName("should run the phases in order, feeding each one with the previous output")
        void shouldRunPhasesInOrder() {
            MealPlanPreparation preparation = samplePreparation();
            MealPlanGenerationResult result = sampleResult();

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(mealPlanAiService.prepare("lucas@email.com")).thenReturn(preparation);
            when(mealPlanAiService.enrich(preparation)).thenReturn(result);
            when(recommendationPersistenceService.persist("lucas@email.com", result))
                    .thenReturn(savedRecommendation(user, Recommendation.RecommendationStatus.GENERATED));

            aiRecommendationService.generateAndSave("lucas@email.com");

            InOrder inOrder = inOrder(readinessValidator, mealPlanAiService, recommendationPersistenceService);
            inOrder.verify(readinessValidator).validate(1);
            inOrder.verify(mealPlanAiService).prepare("lucas@email.com");
            inOrder.verify(mealPlanAiService).enrich(preparation);
            inOrder.verify(recommendationPersistenceService).persist("lucas@email.com", result);
        }

        @Test
        @DisplayName("should not access the recommendation repository directly during generation")
        void shouldNotAccessRepositoryDirectly() {
            MealPlanPreparation preparation = samplePreparation();
            MealPlanGenerationResult result = sampleResult();

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(mealPlanAiService.prepare("lucas@email.com")).thenReturn(preparation);
            when(mealPlanAiService.enrich(preparation)).thenReturn(result);
            when(recommendationPersistenceService.persist("lucas@email.com", result))
                    .thenReturn(savedRecommendation(user, Recommendation.RecommendationStatus.GENERATED));

            aiRecommendationService.generateAndSave("lucas@email.com");

            // Persistence is delegated entirely to RecommendationPersistenceService.
            verifyNoInteractions(recommendationRepository);
        }

        @Test
        @DisplayName("should not enrich or persist when the preparation phase fails")
        void shouldNotEnrichOrPersistWhenPrepareFails() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(mealPlanAiService.prepare("lucas@email.com"))
                    .thenThrow(new BusinessException("Perfil incompleto.", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

            assertThatThrownBy(() -> aiRecommendationService.generateAndSave("lucas@email.com"))
                    .isInstanceOf(BusinessException.class);

            verify(mealPlanAiService, never()).enrich(any());
            verifyNoInteractions(recommendationPersistenceService);
        }

        @Test
        @DisplayName("should reject an incomplete assessment via the readiness gate before reaching the engine")
        void shouldRejectIncompleteAssessmentBeforeEngine() {
            // The synchronous flow must use the same readiness gate as the async flow: an incomplete
            // triage fails with a clear error and never reaches MealPlanAiService (the engine).
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            doThrow(new BusinessException(
                    "Triagem incompleta. Informe objetivo, tipo de dieta e número de refeições antes de gerar um plano alimentar.",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY))
                    .when(readinessValidator).validate(1);

            assertThatThrownBy(() -> aiRecommendationService.generateAndSave("lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Triagem incompleta");

            verifyNoInteractions(mealPlanAiService);
            verifyNoInteractions(recommendationPersistenceService);
        }
    }

    @Nested
    @DisplayName("findLatest")
    class FindLatest {

        @Test
        @DisplayName("should return the latest active (GENERATED) recommendation")
        void shouldReturnLatestActiveRecommendation() {
            // given
            Recommendation recommendation = savedRecommendation(user, Recommendation.RecommendationStatus.GENERATED);
            recommendation.setId(1);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                    1, Recommendation.ACTIVE_STATUSES))
                    .thenReturn(Optional.of(recommendation));

            // when
            Recommendation result = aiRecommendationService.findLatest("lucas@email.com");

            // then
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getStatus()).isIn(Recommendation.ACTIVE_STATUSES);
        }

        @Test
        @DisplayName("should return VIEWED recommendation as active diet")
        void shouldReturnViewedRecommendationAsActiveDiet() {
            // given
            Recommendation recommendation = savedRecommendation(user, Recommendation.RecommendationStatus.VIEWED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                    1, Recommendation.ACTIVE_STATUSES))
                    .thenReturn(Optional.of(recommendation));

            // when
            Recommendation result = aiRecommendationService.findLatest("lucas@email.com");

            // then
            assertThat(result.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
        }

        @Test
        @DisplayName("should not return ARCHIVED recommendation — throws when only ARCHIVED exists")
        void shouldNotReturnArchivedRecommendationAsActiveDiet() {
            // given — repository returns empty because the filter excludes ARCHIVED
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                    1, Recommendation.ACTIVE_STATUSES))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> aiRecommendationService.findLatest("lucas@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("dieta ativa");
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
    @DisplayName("upsertFeedback")
    class UpsertFeedback {

        private FeedbackRequest validRequest() {
            return new FeedbackRequest(4, List.of(RecommendationFeedback.FeedbackTag.PRACTICAL), List.of(), "Gostei bastante.");
        }

        @Test
        @DisplayName("should create feedback and transition GENERATED to VIEWED")
        void shouldCreateFeedbackAndTransitionGeneratedToViewed() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.findByRecommendationId(1)).thenReturn(Optional.empty());
            when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(recommendationFeedbackRepository.save(any(RecommendationFeedback.class))).thenAnswer(inv -> {
                RecommendationFeedback f = inv.getArgument(0);
                f.setId(1);
                return f;
            });

            RecommendationFeedback result = aiRecommendationService.upsertFeedback(1, "lucas@email.com", validRequest());

            assertThat(result.getRating()).isEqualTo(4);
            assertThat(recommendation.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
            verify(recommendationRepository).save(recommendation);
        }

        @Test
        @DisplayName("should update existing feedback without changing status when VIEWED")
        void shouldUpdateFeedbackWithoutChangingStatusWhenViewed() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);

            RecommendationFeedback existing = new RecommendationFeedback();
            existing.setId(1);
            existing.setRecommendation(recommendation);
            existing.setRating(2);

            FeedbackRequest updateRequest = new FeedbackRequest(5, List.of(), List.of(), "Atualizado.");

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.findByRecommendationId(1)).thenReturn(Optional.of(existing));
            when(recommendationFeedbackRepository.save(any(RecommendationFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

            RecommendationFeedback result = aiRecommendationService.upsertFeedback(1, "lucas@email.com", updateRequest);

            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getComment()).isEqualTo("Atualizado.");
            assertThat(recommendation.getStatus()).isEqualTo(Recommendation.RecommendationStatus.VIEWED);
            verify(recommendationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation is ARCHIVED")
        void shouldThrowWhenArchivedOnFeedback() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);
            recommendation.setStatus(Recommendation.RecommendationStatus.ARCHIVED);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));

            assertThatThrownBy(() -> aiRecommendationService.upsertFeedback(1, "lucas@email.com", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("arquivadas");
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation does not belong to user")
        void shouldThrowWhenOwnershipFailsOnFeedback() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(99, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.upsertFeedback(99, "lucas@email.com", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não encontrada");
        }
    }

    @Nested
    @DisplayName("getFeedback")
    class GetFeedback {

        @Test
        @DisplayName("should return existing feedback when it exists")
        void shouldReturnFeedbackWhenExists() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);

            RecommendationFeedback feedback = new RecommendationFeedback();
            feedback.setId(1);
            feedback.setRecommendation(recommendation);
            feedback.setRating(4);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.findByRecommendationId(1)).thenReturn(Optional.of(feedback));

            Optional<RecommendationFeedback> result = aiRecommendationService.getFeedback(1, "lucas@email.com");

            assertThat(result).isPresent();
            assertThat(result.get().getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("should return empty when no feedback exists")
        void shouldReturnEmptyWhenNoFeedback() {
            Recommendation recommendation = new Recommendation();
            recommendation.setId(1);
            recommendation.setUser(user);

            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(recommendation));
            when(recommendationFeedbackRepository.findByRecommendationId(1)).thenReturn(Optional.empty());

            Optional<RecommendationFeedback> result = aiRecommendationService.getFeedback(1, "lucas@email.com");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw BusinessException when recommendation does not belong to user")
        void shouldThrowWhenOwnershipFailsOnGetFeedback() {
            when(userService.findByEmail("lucas@email.com")).thenReturn(user);
            when(recommendationRepository.findByIdAndUserId(99, 1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> aiRecommendationService.getFeedback(99, "lucas@email.com"))
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
