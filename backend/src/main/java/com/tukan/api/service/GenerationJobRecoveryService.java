package com.tukan.api.service;

import com.tukan.api.entity.GenerationJob;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.repository.GenerationJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Recovers and fails {@link GenerationJob}s left in an active (non-terminal) state by an
 * operational fault — an application restart mid-generation, a dead async thread, or a
 * crash before {@code failJob} ran. Without this, such jobs would stay active forever.
 *
 * <p>Design constraints:
 * <ul>
 *   <li>NOT {@code @Transactional}: it fetches the stuck jobs in one read, then delegates
 *       each fail to {@link GenerationJobService#failJob} which owns its own short write
 *       transaction — never one long transaction over the whole batch;</li>
 *   <li>never calls Gemini or any AI logic — pure state cleanup;</li>
 *   <li>idempotent: the query only returns active jobs and {@code failJob} ignores terminal
 *       ones, so repeated runs are harmless;</li>
 *   <li>resilient on boot: a failure here must never prevent the application from starting.</li>
 * </ul>
 */
@Slf4j
@Service
public class GenerationJobRecoveryService {

    // Package-private so tests assert against the single source of truth, not a copy.
    static final String RECOVERY_ERROR_MESSAGE =
            "A geração foi interrompida e não pôde ser concluída. Tente novamente.";

    private final GenerationJobRepository generationJobRepository;
    private final GenerationJobService generationJobService;
    private final Duration stuckThreshold;

    public GenerationJobRecoveryService(
            GenerationJobRepository generationJobRepository,
            GenerationJobService generationJobService,
            @Value("${tukan.jobs.recovery.stuck-threshold-minutes:10}") long stuckThresholdMinutes) {
        this.generationJobRepository = generationJobRepository;
        this.generationJobService = generationJobService;
        this.stuckThreshold = Duration.ofMinutes(stuckThresholdMinutes);
    }

    /**
     * On startup every active job is necessarily orphaned: a single-instance restart kills
     * the async executor threads, so no active job can still be progressing. We fail all of
     * them by using {@code now} as the cutoff. The cutoff is intentionally conservative — a
     * job created in the tiny window before this event fires would also be failed, which is
     * harmless because {@code failJob} is idempotent against terminal jobs and the user can
     * simply retry. A boot failure is swallowed so it can never stop the application starting.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        try {
            recover(Instant.now(), "startup");
        } catch (Exception e) {
            log.error("Startup job recovery failed: errorType={}", e.getClass().getSimpleName(), e);
        }
    }

    /**
     * Periodic safety net for jobs that got stuck during normal operation (a dead thread, a
     * fault before {@code failJob}). Only jobs untouched for longer than the configured
     * threshold are failed, so jobs legitimately in progress are never disturbed.
     */
    @Scheduled(fixedDelayString = "${tukan.jobs.recovery.interval-ms:300000}")
    public void recoverStuckJobs() {
        try {
            recover(Instant.now().minus(stuckThreshold), "scheduled");
        } catch (Exception e) {
            log.error("Scheduled job recovery failed: errorType={}", e.getClass().getSimpleName(), e);
        }
    }

    private void recover(Instant cutoff, String trigger) {
        List<GenerationJob> stuckJobs = generationJobRepository
                .findByStatusInAndUpdatedAtBefore(GenerationJobStatus.activeStatuses(), cutoff);

        if (stuckJobs.isEmpty()) {
            return;
        }

        int recovered = 0;
        for (GenerationJob job : stuckJobs) {
            if (failOne(job)) {
                recovered++;
            }
        }

        int total = stuckJobs.size();
        if (recovered == total) {
            log.info("Job recovery ({}) recovered {} stuck job(s)", trigger, total);
        } else {
            log.warn("Job recovery ({}) recovered {}/{} stuck job(s); {} could not be recovered",
                    trigger, recovered, total, total - recovered);
        }
    }

    /**
     * Fails a single job in its own short transaction, isolating its failure so one bad job
     * never aborts the rest of the batch. Returns whether the job was failed successfully.
     */
    private boolean failOne(GenerationJob job) {
        try {
            generationJobService.failJob(job.getId(), RECOVERY_ERROR_MESSAGE);
            return true;
        } catch (Exception e) {
            log.error("Could not recover stuck job: jobId={} errorType={}",
                    job.getId(), e.getClass().getSimpleName(), e);
            return false;
        }
    }
}
