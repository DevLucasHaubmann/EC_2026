package com.tukan.api.service;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.event.JobStatusChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pure unit tests for the SSE streaming layer. The {@code createEmitter()} seam is overridden so a
 * controllable {@link SseEmitter} mock replaces the real one — no live HTTP request, no servlet
 * async context, no timing/thread flakiness. Map registration is asserted through observable
 * behavior (a later event delivers, or does not), never through reflection on private state.
 *
 * <p>The fast-terminal race (Task 2.3H.1) is reproduced deterministically by the {@code
 * stateReloader} supplier: a reloader returning a terminal state models a job that turned
 * terminal in the window between the caller's read and the emitter registration.
 */
class GenerationJobSseServiceTest {

    private final UUID jobId = UUID.randomUUID();

    private GenerationJobResponse state(GenerationJobStatus status) {
        return new GenerationJobResponse(jobId, status, null, null);
    }

    private JobStatusChangedEvent event(GenerationJobStatus status) {
        return new JobStatusChangedEvent(jobId, status, null, null);
    }

    /** A reloader that re-reads the same (active) state — the no-race default. */
    private Supplier<GenerationJobResponse> reloads(GenerationJobStatus status) {
        return () -> state(status);
    }

    /** Builds a service whose emitter factory always returns the supplied mock. */
    private GenerationJobSseService serviceReturning(SseEmitter emitter) {
        return new GenerationJobSseService() {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
    }

    /** Builds a service whose emitter factory returns each mock in order, one per subscribe. */
    private GenerationJobSseService serviceReturningInOrder(SseEmitter... sequence) {
        Deque<SseEmitter> queue = new ArrayDeque<>(Arrays.asList(sequence));
        return new GenerationJobSseService() {
            @Override
            SseEmitter createEmitter() {
                return queue.poll();
            }
        };
    }

    @Nested
    @DisplayName("subscribe")
    class Subscribe {

        @Test
        @DisplayName("replays current state and registers the emitter for an active job")
        void subscribe_whenJobIsActive_replaysStateAndRegisters() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);

            // when
            SseEmitter returned = sut.subscribe(
                    state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING));

            // then
            assertThat(returned).isSameAs(emitter);
            verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
            verify(emitter, never()).complete();

            // registered: a later event is delivered to the same emitter
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("replays final state, closes, and does not register for a terminal job")
        void subscribe_whenJobIsTerminal_replaysStateAndCompletesWithoutRegistering() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);

            // when
            sut.subscribe(state(GenerationJobStatus.COMPLETED), reloads(GenerationJobStatus.COMPLETED));

            // then
            verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
            verify(emitter).complete();

            // not registered: a later event reaches no emitter
            sut.onJobStatusChanged(event(GenerationJobStatus.COMPLETED));
            verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("closes the emitter and does not register when the replay send fails")
        void subscribe_whenReplaySendFails_completesWithoutRegistering() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            doThrow(new IOException("client gone"))
                    .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
            GenerationJobSseService sut = serviceReturning(emitter);

            // when
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING));

            // then
            verify(emitter).complete();

            // not registered: no further send is attempted on a later event
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("delivers terminal and closes when the job turns terminal during the subscribe window")
        void subscribe_whenJobBecomesTerminalDuringWindow_deliversTerminalAndCloses() throws IOException {
            // given: snapshot is active, but the post-registration re-read sees a terminal state —
            // exactly the fast-failure race the emitter registration alone cannot recover.
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);

            // when
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.FAILED));

            // then: replay (send #1) + recovered terminal (send #2), then closed
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
            verify(emitter).complete();

            // removed: a later (duplicate) terminal event is a no-op, no double delivery
            sut.onJobStatusChanged(event(GenerationJobStatus.FAILED));
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("does not propagate and removes the emitter when the recovered terminal send fails")
        void subscribe_whenRecoveredTerminalSendFails_removesWithoutPropagating() throws IOException {
            // given: replay succeeds, the recovered terminal send throws
            SseEmitter emitter = mock(SseEmitter.class);
            doNothing()
                    .doThrow(new IOException("client gone"))
                    .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
            GenerationJobSseService sut = serviceReturning(emitter);

            // when / then: no exception escapes subscribe
            assertThatNoException().isThrownBy(() ->
                    sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.COMPLETED)));

            verify(emitter).complete();

            // removed: a later event reaches no emitter
            sut.onJobStatusChanged(event(GenerationJobStatus.COMPLETED));
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("removes and closes the emitter without leaking when the re-read throws")
        void subscribe_whenStateReloaderThrows_removesEmitterWithoutLeak() throws IOException {
            // given: the post-registration re-read fails (e.g. the job vanished between reads)
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);
            Supplier<GenerationJobResponse> failingReload = () -> {
                throw new IllegalStateException("job gone");
            };

            // when / then: the exception surfaces but the emitter is not left orphaned
            assertThatThrownBy(() ->
                    sut.subscribe(state(GenerationJobStatus.PENDING), failingReload))
                    .isInstanceOf(IllegalStateException.class);

            verify(emitter).complete();

            // removed: a later event reaches no emitter
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("replaces and closes the previous subscriber for the same job")
        void subscribe_whenSecondSubscriberForSameJob_replacesAndClosesPrevious() throws IOException {
            // given
            SseEmitter first = mock(SseEmitter.class);
            SseEmitter second = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturningInOrder(first, second);

            // when: a second subscription for the same jobId arrives
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING));
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING));

            // then: the previous emitter is completed and replaced
            verify(first).complete();

            // events now reach only the current emitter
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(second, times(2)).send(any(SseEmitter.SseEventBuilder.class)); // replay + event
            verify(first, times(1)).send(any(SseEmitter.SseEventBuilder.class));  // only its own replay
        }
    }

    @Nested
    @DisplayName("onJobStatusChanged")
    class OnJobStatusChanged {

        @Test
        @DisplayName("ignores events for a jobId with no registered emitter")
        void onJobStatusChanged_whenNoEmitterRegistered_doesNothing() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);

            // when
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));

            // then
            verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("delivers a terminal event then closes and removes the emitter")
        void onJobStatusChanged_whenTerminal_deliversThenCompletesAndRemoves() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING)); // send #1

            // when
            sut.onJobStatusChanged(new JobStatusChangedEvent(
                    jobId, GenerationJobStatus.COMPLETED, 7, null)); // send #2

            // then
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
            verify(emitter).complete();

            // removed: a subsequent event is a no-op
            sut.onJobStatusChanged(event(GenerationJobStatus.COMPLETED));
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("removes and closes the emitter when delivery fails")
        void onJobStatusChanged_whenSendFails_removesAndCompletes() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            GenerationJobSseService sut = serviceReturning(emitter);
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING)); // send #1 ok
            doThrow(new IOException("client gone"))
                    .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

            // when
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI)); // send #2 fails

            // then
            verify(emitter).complete();

            // removed: no third send attempt
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Nested
    @DisplayName("lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("completion callback removes the emitter from the registry")
        void completionCallback_removesEmitter() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            ArgumentCaptor<Runnable> completion = ArgumentCaptor.forClass(Runnable.class);
            GenerationJobSseService sut = serviceReturning(emitter);
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING)); // send #1
            verify(emitter).onCompletion(completion.capture());

            // when
            completion.getValue().run();

            // then: no further delivery after removal
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("error callback removes the emitter from the registry")
        @SuppressWarnings("unchecked")
        void errorCallback_removesEmitter() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            ArgumentCaptor<Consumer<Throwable>> error = ArgumentCaptor.forClass(Consumer.class);
            GenerationJobSseService sut = serviceReturning(emitter);
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING)); // send #1
            verify(emitter).onError(error.capture());

            // when
            error.getValue().accept(new RuntimeException("broken pipe"));

            // then: no further delivery after removal
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("timeout callback completes the emitter and removes it from the registry")
        void timeoutCallback_completesAndRemovesEmitter() throws IOException {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
            GenerationJobSseService sut = serviceReturning(emitter);
            sut.subscribe(state(GenerationJobStatus.PENDING), reloads(GenerationJobStatus.PENDING)); // send #1
            verify(emitter).onTimeout(timeout.capture());

            // when
            timeout.getValue().run();

            // then
            verify(emitter).complete();
            sut.onJobStatusChanged(event(GenerationJobStatus.GENERATING_WITH_AI));
            verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        }
    }
}
