package com.tukan.api.service;

import com.tukan.api.entity.GenerationJob;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.repository.GenerationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationJobRecoveryServiceTest {

    private static final long STUCK_THRESHOLD_MINUTES = 30;

    @Mock
    private GenerationJobRepository generationJobRepository;

    @Mock
    private GenerationJobService generationJobService;

    @Captor
    private ArgumentCaptor<Collection<GenerationJobStatus>> statusesCaptor;

    @Captor
    private ArgumentCaptor<Instant> cutoffCaptor;

    @Captor
    private ArgumentCaptor<UUID> idCaptor;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private GenerationJobRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new GenerationJobRecoveryService(
                generationJobRepository, generationJobService, STUCK_THRESHOLD_MINUTES);
    }

    private GenerationJob activeJob(GenerationJobStatus status) {
        GenerationJob job = new GenerationJob();
        job.setId(UUID.randomUUID());
        job.setStatus(status);
        return job;
    }

    @Nested
    @DisplayName("recoverStuckJobs (scheduled)")
    class RecoverStuckJobs {

        @Test
        @DisplayName("fails every stuck job exactly once with the recovery message")
        void recoverStuckJobs_whenJobsAreStuck_failsEachOnceWithRecoveryMessage() {
            // given
            GenerationJob jobA = activeJob(GenerationJobStatus.PENDING);
            GenerationJob jobB = activeJob(GenerationJobStatus.GENERATING_WITH_AI);
            GenerationJob jobC = activeJob(GenerationJobStatus.SAVING_RECOMMENDATION);
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenReturn(List.of(jobA, jobB, jobC));

            // when
            recoveryService.recoverStuckJobs();

            // then
            verify(generationJobService, times(3)).failJob(idCaptor.capture(), messageCaptor.capture());
            assertThat(idCaptor.getAllValues())
                    .containsExactlyInAnyOrder(jobA.getId(), jobB.getId(), jobC.getId());
            assertThat(messageCaptor.getAllValues())
                    .containsOnly(GenerationJobRecoveryService.RECOVERY_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("queries only active statuses, never terminal ones")
        void recoverStuckJobs_always_queriesOnlyActiveStatuses() {
            // given
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenReturn(List.of());

            // when
            recoveryService.recoverStuckJobs();

            // then
            verify(generationJobRepository)
                    .findByStatusInAndUpdatedAtBefore(statusesCaptor.capture(), any());
            assertThat(statusesCaptor.getValue())
                    .containsExactlyInAnyOrderElementsOf(GenerationJobStatus.activeStatuses())
                    .doesNotContain(GenerationJobStatus.COMPLETED, GenerationJobStatus.FAILED);
        }

        @Test
        @DisplayName("uses now-minus-threshold as the cutoff")
        void recoverStuckJobs_always_usesThresholdCutoff() {
            // given
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenReturn(List.of());
            Instant before = Instant.now();

            // when
            recoveryService.recoverStuckJobs();

            // then
            Instant after = Instant.now();
            verify(generationJobRepository)
                    .findByStatusInAndUpdatedAtBefore(any(), cutoffCaptor.capture());
            assertThat(cutoffCaptor.getValue())
                    .isAfterOrEqualTo(before.minus(STUCK_THRESHOLD_MINUTES, ChronoUnit.MINUTES))
                    .isBeforeOrEqualTo(after.minus(STUCK_THRESHOLD_MINUTES, ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("does nothing when there are no stuck jobs")
        void recoverStuckJobs_whenNoStuckJobs_doesNotTouchJobService() {
            // given
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenReturn(List.of());

            // when
            recoveryService.recoverStuckJobs();

            // then
            verifyNoInteractions(generationJobService);
        }

        @Test
        @DisplayName("keeps processing the batch when one job fails to be recovered")
        void recoverStuckJobs_whenOneJobFails_stillProcessesTheRest() {
            // given
            GenerationJob jobA = activeJob(GenerationJobStatus.PENDING);
            GenerationJob jobB = activeJob(GenerationJobStatus.GENERATING_WITH_AI);
            GenerationJob jobC = activeJob(GenerationJobStatus.SAVING_RECOMMENDATION);
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenReturn(List.of(jobA, jobB, jobC));
            doThrow(new RuntimeException("transient"))
                    .when(generationJobService).failJob(eq(jobB.getId()), any());

            // when
            recoveryService.recoverStuckJobs();

            // then
            verify(generationJobService, times(3)).failJob(idCaptor.capture(), any());
            assertThat(idCaptor.getAllValues())
                    .containsExactlyInAnyOrder(jobA.getId(), jobB.getId(), jobC.getId());
        }

        @Test
        @DisplayName("does not propagate when the repository throws")
        void recoverStuckJobs_whenRepositoryThrows_doesNotPropagate() {
            // given
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenThrow(new RuntimeException("DB unavailable"));

            // when / then
            assertThatCode(() -> recoveryService.recoverStuckJobs()).doesNotThrowAnyException();
            verify(generationJobService, never()).failJob(any(), any());
        }
    }

    @Nested
    @DisplayName("recoverOnStartup")
    class RecoverOnStartup {

        @Test
        @DisplayName("uses now as the cutoff so every active job is recovered")
        void recoverOnStartup_always_usesNowAsCutoff() {
            // given
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenReturn(List.of());
            Instant before = Instant.now();

            // when
            recoveryService.recoverOnStartup();

            // then
            Instant after = Instant.now();
            verify(generationJobRepository)
                    .findByStatusInAndUpdatedAtBefore(any(), cutoffCaptor.capture());
            assertThat(cutoffCaptor.getValue())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("never propagates a failure so the application can still start")
        void recoverOnStartup_whenRepositoryThrows_doesNotPropagate() {
            // given
            when(generationJobRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                    .thenThrow(new RuntimeException("DB unavailable at startup"));

            // when / then
            assertThatCode(() -> recoveryService.recoverOnStartup()).doesNotThrowAnyException();
            verify(generationJobService, never()).failJob(any(), any());
        }
    }
}
