package com.tukan.api.controller;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.service.AiRecommendationAsyncService;
import com.tukan.api.service.GenerationJobService;
import com.tukan.api.service.GenerationJobSseService;
import com.tukan.api.service.JobStartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * HTTP boundary for asynchronous diet generation jobs. Thin by design: it resolves the
 * authenticated user, delegates the decision (create vs. reuse, ownership) to the service,
 * and maps outcomes to HTTP status. No business rule lives here.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class GenerationJobController {

    private final GenerationJobService generationJobService;
    private final GenerationJobSseService generationJobSseService;
    private final AiRecommendationAsyncService aiRecommendationAsyncService;

    /**
     * Starts a generation job. Returns 202 with the freshly created job and dispatches async
     * generation; or 409 with the already-active job (for re-attachment) without dispatching.
     */
    @PostMapping("/ai/recommendations/jobs")
    public ResponseEntity<GenerationJobResponse> startJob(Authentication authentication) {
        JobStartResult result = generationJobService.startJob(authentication.getName());

        if (!result.created()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result.job());
        }

        aiRecommendationAsyncService.generateForJob(result.job().jobId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.job());
    }

    /**
     * Polling fallback for clients that cannot keep the SSE stream open. Returns the current
     * job state for the authenticated owner, or 404 when the job is not theirs / does not exist.
     */
    @GetMapping("/ai/recommendations/jobs/{jobId}")
    public ResponseEntity<GenerationJobResponse> getStatus(@PathVariable UUID jobId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(generationJobService.getStatusForOwner(jobId, authentication.getName()));
    }

    /**
     * Server-Sent Events stream of real status changes. Ownership is validated before the emitter
     * is opened; the current state is replayed on connect and the stream closes on a terminal job.
     */
    @GetMapping(value = "/ai/recommendations/jobs/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable UUID jobId, Authentication authentication) {
        String owner = authentication.getName();
        GenerationJobResponse currentState = generationJobService.getStatusForOwner(jobId, owner);
        // Same ownership-checked reader as the initial read; the service re-reads after registering
        // the emitter to recover a terminal transition that raced the subscription (Task 2.3H.1).
        return generationJobSseService.subscribe(currentState,
                () -> generationJobService.getStatusForOwner(jobId, owner));
    }
}
