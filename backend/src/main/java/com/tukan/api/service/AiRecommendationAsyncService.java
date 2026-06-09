package com.tukan.api.service;

import com.tukan.api.config.AsyncConfig;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.service.mealplan.MealPlanAiService;
import com.tukan.api.service.mealplan.MealPlanGenerationResult;
import com.tukan.api.service.mealplan.MealPlanPreparation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Asynchronous orchestrator for diet generation tied to a {@link com.tukan.api.entity.GenerationJob}.
 * Runs the same three decoupled phases as the synchronous flow — prepare / enrich / persist —
 * updating the job status before each phase and resolving it to a terminal state at the end.
 *
 * <p>Design constraints:
 * <ul>
 *   <li>NOT {@code @Transactional}: each phase and each status mutation owns its own short
 *       transaction through a separate bean proxy, so the external AI call in {@code enrich}
 *       never runs inside a database transaction;</li>
 *   <li>pure orchestration: no persistence logic and no AI logic live here;</li>
 *   <li>never propagates to the executor — any failure is captured and routed to {@code failJob}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationAsyncService {

    // Package-private so tests assert against the single source of truth, not a copy.
    static final String SAFE_ERROR_MESSAGE =
            "Não foi possível gerar a recomendação. Tente novamente em instantes.";

    private final GenerationJobService generationJobService;
    private final MealPlanAiService mealPlanAiService;
    private final RecommendationPersistenceService recommendationPersistenceService;

    /**
     * Executes the full generation pipeline for an existing job on the dedicated AI executor.
     * Fire-and-forget: callers observe progress through the job status, not a return value.
     */
    @Async(AsyncConfig.AI_RECOMMENDATION_EXECUTOR)
    public void generateForJob(UUID jobId) {
        try {
            runPipeline(jobId);
        } catch (Exception e) {
            handleFailure(jobId, e);
        }
    }

    private void runPipeline(UUID jobId) {
        String email = generationJobService.getJobOwnerEmail(jobId);

        generationJobService.transitionTo(jobId, GenerationJobStatus.PREPARING_CONTEXT);
        MealPlanPreparation preparation = mealPlanAiService.prepare(email);

        generationJobService.transitionTo(jobId, GenerationJobStatus.GENERATING_WITH_AI);
        MealPlanGenerationResult result = mealPlanAiService.enrich(preparation);

        generationJobService.transitionTo(jobId, GenerationJobStatus.SAVING_RECOMMENDATION);
        Recommendation recommendation = recommendationPersistenceService.persist(email, result);

        generationJobService.completeJob(jobId, recommendation.getId());
    }

    private void handleFailure(UUID jobId, Exception cause) {
        log.error("Async diet generation failed: jobId={} errorType={}",
                jobId, cause.getClass().getSimpleName(), cause);
        try {
            generationJobService.failJob(jobId, resolveErrorMessage(cause));
        } catch (Exception failure) {
            log.error("Could not mark job as FAILED: jobId={} errorType={}",
                    jobId, failure.getClass().getSimpleName(), failure);
        }
    }

    /**
     * Chooses the message persisted on the failed job. A {@link BusinessException} carries an
     * intentional, user-safe message (e.g. an unreachable calorie floor for a meal) that must reach
     * the client unchanged, so the real cause is no longer masked. Any other exception is technical
     * or unexpected: it collapses to {@link #SAFE_ERROR_MESSAGE} while its full stack trace stays in
     * the log above, never leaking to the client. A business message that is blank also falls back,
     * so the user never sees an empty error.
     */
    private String resolveErrorMessage(Exception cause) {
        if (cause instanceof BusinessException && StringUtils.hasText(cause.getMessage())) {
            return cause.getMessage();
        }
        return SAFE_ERROR_MESSAGE;
    }
}
