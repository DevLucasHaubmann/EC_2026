package com.tukan.api.service;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.entity.GenerationJob;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.entity.User;
import com.tukan.api.event.JobStatusChangedEvent;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.GenerationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns the lifecycle mutations of a {@link GenerationJob}. Each public method runs in
 * its own short transaction so the asynchronous orchestrator can update job state in
 * isolated write transactions, never holding a connection across the external AI call.
 *
 * <p>No SSE / streaming concerns live here — this service only persists discrete state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationJobService {

    private final GenerationJobRepository generationJobRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final MealPlanReadinessValidator readinessValidator;

    /**
     * Starts generation for the authenticated user: returns the existing active job (for
     * re-attachment) if one is already running, otherwise persists a fresh {@code PENDING} job.
     * The "create vs. reuse" decision lives here, not in the controller. The caller dispatches
     * async work only when {@link JobStartResult#created()} is {@code true}.
     */
    @Transactional
    public JobStartResult startJob(String authenticatedEmail) {
        User owner = userService.findByEmail(authenticatedEmail);
        readinessValidator.validate(owner.getId());
        return generationJobRepository
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        owner.getId(), GenerationJobStatus.activeStatuses())
                .map(active -> JobStartResult.alreadyActive(GenerationJobResponse.from(active)))
                .orElseGet(() -> JobStartResult.created(createPendingJob(owner)));
    }

    /**
     * Returns the job's current state for the authenticated owner, or 404 if it does not exist
     * or does not belong to the user. The UUID alone never grants access — ownership is always
     * checked against the resolved user id.
     */
    @Transactional(readOnly = true)
    public GenerationJobResponse getStatusForOwner(UUID jobId, String authenticatedEmail) {
        Integer userId = userService.findByEmail(authenticatedEmail).getId();
        return generationJobRepository.findByIdAndUserId(jobId, userId)
                .map(GenerationJobResponse::from)
                .orElseThrow(() -> new BusinessException(
                        "Job de geração não encontrado.", HttpStatus.NOT_FOUND));
    }

    /**
     * Persists a fresh {@code PENDING} job owned by the given user and returns its DTO snapshot.
     */
    private GenerationJobResponse createPendingJob(User owner) {
        GenerationJob job = new GenerationJob();
        job.setUser(owner);
        job.setStatus(GenerationJobStatus.PENDING);
        return GenerationJobResponse.from(generationJobRepository.save(job));
    }

    /**
     * Moves an active job to the given non-terminal status. The first transition out of
     * {@code PENDING} stamps {@code startedAt}. Rejects transitions on terminal jobs and
     * rejects terminal targets — use {@link #completeJob} / {@link #failJob} for those.
     */
    @Transactional
    public void transitionTo(UUID jobId, GenerationJobStatus status) {
        if (status.isTerminal()) {
            throw new IllegalArgumentException(
                    "Use completeJob/failJob for terminal status: " + status);
        }

        GenerationJob job = requireJob(jobId);
        ensureNotTerminal(job);

        if (job.getStartedAt() == null) {
            job.setStartedAt(Instant.now());
        }
        job.setStatus(status);
        publishStatusChange(job);
    }

    /**
     * Marks the job as {@code COMPLETED} with the persisted recommendation id.
     */
    @Transactional
    public void completeJob(UUID jobId, Integer recommendationId) {
        GenerationJob job = requireJob(jobId);
        ensureNotTerminal(job);

        job.setStatus(GenerationJobStatus.COMPLETED);
        job.setRecommendationId(recommendationId);
        job.setFinishedAt(Instant.now());
        publishStatusChange(job);
    }

    /**
     * Marks the job as {@code FAILED} with a user-safe error message. Idempotent against
     * already-terminal jobs: a completed job is never flipped to failed.
     */
    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        GenerationJob job = requireJob(jobId);

        if (job.getStatus().isTerminal()) {
            log.warn("Ignoring failJob on already-terminal job: jobId={} status={}",
                    jobId, job.getStatus());
            return;
        }

        job.setStatus(GenerationJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setFinishedAt(Instant.now());
        publishStatusChange(job);
    }

    /**
     * Resolves the owning user's email inside a read-only transaction so the lazy
     * {@code user} association is initialized without leaking outside this boundary.
     */
    @Transactional(readOnly = true)
    public String getJobOwnerEmail(UUID jobId) {
        return requireJob(jobId).getUser().getEmail();
    }

    /**
     * Publishes a status-change snapshot so the SSE layer can stream it after commit. Kept as a
     * plain domain event so this service stays unaware of streaming concerns.
     */
    private void publishStatusChange(GenerationJob job) {
        eventPublisher.publishEvent(new JobStatusChangedEvent(
                job.getId(), job.getStatus(), job.getRecommendationId(), job.getErrorMessage()));
    }

    private GenerationJob requireJob(UUID jobId) {
        return generationJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(
                        "Job de geração não encontrado.", HttpStatus.NOT_FOUND));
    }

    private void ensureNotTerminal(GenerationJob job) {
        if (job.getStatus().isTerminal()) {
            throw new BusinessException(
                    "Job de geração já foi finalizado.", HttpStatus.CONFLICT);
        }
    }
}
