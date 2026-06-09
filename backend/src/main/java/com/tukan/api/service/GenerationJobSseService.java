package com.tukan.api.service;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.event.JobStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Streams real generation-job status changes to subscribed clients over SSE.
 *
 * <p>Responsibilities are isolated here so neither the controller nor {@code GenerationJobService}
 * carry streaming concerns. One emitter is tracked per {@code jobId} in a thread-safe map;
 * every terminal/timeout/error/completion path removes it, so connections never leak.
 *
 * <p>Future events arrive through {@link #onJobStatusChanged}, bound to {@code AFTER_COMMIT}
 * so a client is never notified of state that has not been durably persisted.
 */
@Slf4j
@Service
public class GenerationJobSseService {

    /** Named SSE event consumed by the frontend. */
    static final String EVENT_NAME = "job-update";

    /** Caps a connection's lifetime so an abandoned client cannot hold a server thread forever. */
    static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Opens an emitter, replays the current state, and either closes immediately (terminal job)
     * or registers the emitter for future events (active job). Ownership must already be
     * validated by the caller before this method is reached.
     *
     * <p>Closes the SSE race on a fast terminal job (Task 2.3H.1): the emitter is registered
     * <em>before</em> the state is re-read through {@code stateReloader}. A terminal transition
     * that commits from registration onward is delivered by {@link #onJobStatusChanged}; one that
     * committed in the gap between the caller's read and this registration — whose {@code
     * AFTER_COMMIT} listener found no emitter and dropped the event — is recovered by the re-read.
     * The two paths cannot both miss it, so the terminal event is never lost.
     *
     * @param currentState  the snapshot the caller already read and authorized; replayed on connect
     * @param stateReloader re-reads the latest state after registration, under the same ownership
     */
    public SseEmitter subscribe(GenerationJobResponse currentState,
                                Supplier<GenerationJobResponse> stateReloader) {
        UUID jobId = currentState.jobId();
        SseEmitter emitter = createEmitter();
        registerLifecycleCallbacks(emitter, jobId);

        // Replay the authorized snapshot before deciding whether to keep the connection open.
        if (!trySend(emitter, currentState)) {
            safeComplete(emitter);
            return emitter;
        }

        if (currentState.status().isTerminal()) {
            safeComplete(emitter);
            return emitter;
        }

        registerEmitter(jobId, emitter);

        // Recover a terminal transition that committed during the registration window above.
        // A failing re-read must not leave the just-registered emitter orphaned in the map.
        try {
            GenerationJobResponse latest = stateReloader.get();
            if (latest.status().isTerminal()) {
                deliverTerminalAndClose(jobId, emitter, latest);
            }
        } catch (RuntimeException e) {
            emitters.remove(jobId, emitter);
            safeComplete(emitter);
            throw e;
        }
        return emitter;
    }

    /**
     * Pushes a status change to the subscribed client (if any) after the writing transaction
     * commits. Closes and removes the emitter on terminal status or on a failed send.
     *
     * <p>Bound to {@code AFTER_COMMIT} (no {@code fallbackExecution}) on purpose: the publishers
     * ({@code transitionTo}/{@code completeJob}/{@code failJob}) are all {@code @Transactional},
     * so a client is only notified of state already durably persisted. An event published outside
     * a transaction is intentionally dropped rather than streaming uncommitted state.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobStatusChanged(JobStatusChangedEvent event) {
        SseEmitter emitter = emitters.get(event.jobId());
        if (emitter == null) {
            return;
        }

        GenerationJobResponse payload = new GenerationJobResponse(
                event.jobId(), event.status(), event.recommendationId(), event.errorMessage());

        if (event.status().isTerminal()) {
            deliverTerminalAndClose(event.jobId(), emitter, payload);
            return;
        }

        if (!trySend(emitter, payload)) {
            emitters.remove(event.jobId(), emitter);
            safeComplete(emitter);
        }
    }

    /**
     * Atomically claims the emitter, then sends the terminal event and closes it. The removal is
     * the single coordination token shared with {@link #subscribe}: only the caller that wins
     * {@link Map#remove(Object, Object)} delivers the terminal event and completes, so a terminal
     * status reached concurrently by the re-read and {@code onJobStatusChanged} is sent exactly
     * once with no double completion.
     */
    private void deliverTerminalAndClose(UUID jobId, SseEmitter emitter, GenerationJobResponse payload) {
        if (emitters.remove(jobId, emitter)) {
            trySend(emitter, payload);
            safeComplete(emitter);
        }
    }

    /** Binds cleanup before any send so a failed replay or a closed connection still tears down. */
    private void registerLifecycleCallbacks(SseEmitter emitter, UUID jobId) {
        emitter.onCompletion(() -> emitters.remove(jobId, emitter));
        emitter.onError(e -> emitters.remove(jobId, emitter));
        emitter.onTimeout(() -> {
            emitters.remove(jobId, emitter);
            safeComplete(emitter);
        });
    }

    /**
     * Single emitter per {@code jobId}: a second subscriber replaces the previous one, which is
     * completed so an abandoned connection is not leaked until its own timeout. Completion runs
     * outside the map operation on purpose: {@code compute} would hold the bin lock while
     * {@code safeComplete} performs an external emitter call (which may block or re-enter the
     * {@link ConcurrentHashMap} via the previous emitter's {@code onCompletion} callback).
     */
    private void registerEmitter(UUID jobId, SseEmitter emitter) {
        SseEmitter previous = emitters.put(jobId, emitter);
        if (previous != null) {
            safeComplete(previous);
        }
    }

    /**
     * Completes an emitter, tolerating one already completed/closed by a concurrent
     * timeout or error callback — {@link SseEmitter#complete()} may throw in that race.
     */
    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("Emitter already completed: reason={}", e.getClass().getSimpleName());
        }
    }

    /**
     * Creates the emitter for a subscription. Isolated as a seam so tests can supply a
     * controllable emitter and assert send/cleanup behavior without a live HTTP request.
     */
    SseEmitter createEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }

    /**
     * Sends one named event, tolerating a disconnected client. Returns {@code false} instead of
     * propagating so a dead connection never breaks the generation flow.
     */
    private boolean trySend(SseEmitter emitter, GenerationJobResponse payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(payload, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE delivery failed, dropping emitter: jobId={} reason={}",
                    payload.jobId(), e.getClass().getSimpleName());
            return false;
        }
    }
}
