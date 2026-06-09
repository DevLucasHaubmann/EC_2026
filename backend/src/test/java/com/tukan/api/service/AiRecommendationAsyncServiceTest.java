package com.tukan.api.service;

import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.exception.BusinessException;
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
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendationAsyncServiceTest {

    private static final String EMAIL = "owner@test.com";
    private static final String SAFE_ERROR_MESSAGE = AiRecommendationAsyncService.SAFE_ERROR_MESSAGE;

    @Mock
    private GenerationJobService generationJobService;

    @Mock
    private MealPlanAiService mealPlanAiService;

    @Mock
    private RecommendationPersistenceService recommendationPersistenceService;

    @InjectMocks
    private AiRecommendationAsyncService asyncService;

    private final UUID jobId = UUID.randomUUID();

    @Mock
    private MealPlanPreparation preparation;

    @Mock
    private MealPlanGenerationResult result;

    @Mock
    private Recommendation recommendation;

    @BeforeEach
    void resolveOwnerEmail() {
        // Lenient by default via MockitoExtension; the failure path that throws here overrides it.
        when(generationJobService.getJobOwnerEmail(jobId)).thenReturn(EMAIL);
    }

    @Nested
    @DisplayName("generateForJob — happy path")
    class HappyPath {

        @Test
        @DisplayName("runs the full pipeline and updates status in the exact order")
        void generateForJob_whenPipelineSucceeds_callsCollaboratorsInOrder() {
            // given
            when(mealPlanAiService.prepare(EMAIL)).thenReturn(preparation);
            when(mealPlanAiService.enrich(preparation)).thenReturn(result);
            when(recommendationPersistenceService.persist(EMAIL, result)).thenReturn(recommendation);
            when(recommendation.getId()).thenReturn(7);

            // when
            asyncService.generateForJob(jobId);

            // then
            InOrder inOrder = inOrder(
                    generationJobService, mealPlanAiService, recommendationPersistenceService);
            inOrder.verify(generationJobService).getJobOwnerEmail(jobId);
            inOrder.verify(generationJobService).transitionTo(jobId, GenerationJobStatus.PREPARING_CONTEXT);
            inOrder.verify(mealPlanAiService).prepare(EMAIL);
            inOrder.verify(generationJobService).transitionTo(jobId, GenerationJobStatus.GENERATING_WITH_AI);
            inOrder.verify(mealPlanAiService).enrich(same(preparation));
            inOrder.verify(generationJobService).transitionTo(jobId, GenerationJobStatus.SAVING_RECOMMENDATION);
            inOrder.verify(recommendationPersistenceService).persist(EMAIL, result);
            inOrder.verify(generationJobService).completeJob(jobId, 7);

            verify(generationJobService, never()).failJob(any(), any());
        }
    }

    @Nested
    @DisplayName("generateForJob — failure paths")
    class FailurePaths {

        @Test
        @DisplayName("prepare throws: marks job FAILED with safe message and does not propagate")
        void generateForJob_whenPrepareThrows_callsFailJobAndDoesNotPropagate() {
            // given
            when(mealPlanAiService.prepare(EMAIL)).thenThrow(new RuntimeException("AI timeout"));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq(SAFE_ERROR_MESSAGE));
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("enrich throws: stops before persist and marks job FAILED")
        void generateForJob_whenEnrichThrows_doesNotPersistAndFailsJob() {
            // given
            when(mealPlanAiService.prepare(EMAIL)).thenReturn(preparation);
            when(mealPlanAiService.enrich(preparation)).thenThrow(new RuntimeException("parse error"));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq(SAFE_ERROR_MESSAGE));
            verify(recommendationPersistenceService, never()).persist(any(), any());
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("persist throws: marks job FAILED and never completes it")
        void generateForJob_whenPersistThrows_failsJobWithoutCompleting() {
            // given
            when(mealPlanAiService.prepare(EMAIL)).thenReturn(preparation);
            when(mealPlanAiService.enrich(preparation)).thenReturn(result);
            when(recommendationPersistenceService.persist(EMAIL, result))
                    .thenThrow(new RuntimeException("DB down"));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq(SAFE_ERROR_MESSAGE));
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("getJobOwnerEmail throws business error: preserves its message, no status transition")
        void generateForJob_whenOwnerEmailThrows_failsJobWithoutTransition() {
            // given
            when(generationJobService.getJobOwnerEmail(jobId))
                    .thenThrow(new BusinessException("Job de geração não encontrado.", HttpStatus.NOT_FOUND));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq("Job de geração não encontrado."));
            verify(generationJobService, never()).transitionTo(any(), any());
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("getJobOwnerEmail throws technical error: falls back to the safe generic message")
        void generateForJob_whenOwnerEmailThrowsTechnical_usesSafeMessage() {
            // given
            when(generationJobService.getJobOwnerEmail(jobId))
                    .thenThrow(new RuntimeException("connection reset"));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq(SAFE_ERROR_MESSAGE));
            verify(generationJobService, never()).transitionTo(any(), any());
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("business failure is preserved: the specific message reaches the job, not the generic one")
        void generateForJob_whenBusinessExceptionThrown_preservesSpecificMessage() {
            // given: a real business rule violation surfaced by the deterministic plan generation
            String businessMessage = "Não foi possível gerar uma refeição válida para JANTAR: "
                    + "nenhuma opção atingiu o piso calórico mínimo.";
            when(mealPlanAiService.prepare(EMAIL))
                    .thenThrow(new BusinessException(businessMessage, HttpStatus.UNPROCESSABLE_ENTITY));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq(businessMessage));
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("business failure with blank message falls back to the safe generic message")
        void generateForJob_whenBusinessExceptionHasBlankMessage_usesSafeMessage() {
            // given
            when(mealPlanAiService.prepare(EMAIL))
                    .thenThrow(new BusinessException("   ", HttpStatus.UNPROCESSABLE_ENTITY));

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
            verify(generationJobService).failJob(eq(jobId), eq(SAFE_ERROR_MESSAGE));
            verify(generationJobService, never()).completeJob(any(), any());
        }

        @Test
        @DisplayName("failJob also throws: the secondary failure is still absorbed")
        void generateForJob_whenFailJobAlsoThrows_doesNotPropagate() {
            // given
            when(mealPlanAiService.prepare(EMAIL)).thenThrow(new RuntimeException("AI timeout"));
            doThrow(new RuntimeException("DB down")).when(generationJobService).failJob(any(), any());

            // when / then
            assertThatCode(() -> asyncService.generateForJob(jobId)).doesNotThrowAnyException();
        }
    }
}
