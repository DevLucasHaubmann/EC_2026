package com.tukan.api.service;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.GenerationJob;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.entity.User;
import com.tukan.api.event.JobStatusChangedEvent;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.GenerationJobRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationJobServiceTest {

    @Mock
    private GenerationJobRepository generationJobRepository;

    @Mock
    private NutritionalProfileRepository nutritionalProfileRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GenerationJobService generationJobService;

    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // The readiness validator is exercised through the service with the same mocked repositories
        // and the real completeness policy, so readiness behaviour stays covered end-to-end here.
        MealPlanReadinessValidator readinessValidator = new MealPlanReadinessValidator(
                nutritionalProfileRepository, assessmentRepository, new AssessmentCompletenessPolicy());
        generationJobService = new GenerationJobService(
                generationJobRepository, userService, eventPublisher, readinessValidator);
    }

    private GenerationJob jobWithStatus(GenerationJobStatus status) {
        GenerationJob job = new GenerationJob();
        job.setStatus(status);
        return job;
    }

    @Nested
    @DisplayName("transitionTo")
    class TransitionTo {

        @ParameterizedTest
        @EnumSource(value = GenerationJobStatus.class, names = {"COMPLETED", "FAILED"})
        @DisplayName("rejects terminal target status before touching the repository")
        void transitionTo_whenStatusIsTerminal_throwsIllegalArgumentException(GenerationJobStatus terminal) {
            // when / then
            assertThatThrownBy(() -> generationJobService.transitionTo(jobId, terminal))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(generationJobRepository);
        }

        @Test
        @DisplayName("rejects transition on an already-terminal job with CONFLICT")
        void transitionTo_whenJobIsAlreadyTerminal_throwsBusinessExceptionConflict() {
            // given
            when(generationJobRepository.findById(jobId))
                    .thenReturn(Optional.of(jobWithStatus(GenerationJobStatus.COMPLETED)));

            // when / then
            assertThatThrownBy(() ->
                    generationJobService.transitionTo(jobId, GenerationJobStatus.PREPARING_CONTEXT))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("stamps startedAt on the first transition out of PENDING")
        void transitionTo_whenJobHasNoStartedAt_stampsStartedAt() {
            // given
            GenerationJob job = jobWithStatus(GenerationJobStatus.PENDING);
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            Instant before = Instant.now();

            // when
            generationJobService.transitionTo(jobId, GenerationJobStatus.PREPARING_CONTEXT);

            // then
            assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.PREPARING_CONTEXT);
            assertThat(job.getStartedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(Instant.now());
            verify(eventPublisher).publishEvent(any(JobStatusChangedEvent.class));
        }

        @Test
        @DisplayName("does not overwrite startedAt on a subsequent transition")
        void transitionTo_whenJobAlreadyHasStartedAt_doesNotOverwriteIt() {
            // given
            Instant original = Instant.parse("2025-01-01T00:00:00Z");
            GenerationJob job = jobWithStatus(GenerationJobStatus.PREPARING_CONTEXT);
            job.setStartedAt(original);
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));

            // when
            generationJobService.transitionTo(jobId, GenerationJobStatus.GENERATING_WITH_AI);

            // then
            assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.GENERATING_WITH_AI);
            assertThat(job.getStartedAt()).isEqualTo(original);
        }

        @Test
        @DisplayName("throws NOT_FOUND when the job does not exist")
        void transitionTo_whenJobNotFound_throwsBusinessExceptionNotFound() {
            // given
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() ->
                    generationJobService.transitionTo(jobId, GenerationJobStatus.PREPARING_CONTEXT))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("completeJob")
    class CompleteJob {

        @Test
        @DisplayName("marks the job COMPLETED with recommendationId and finishedAt")
        void completeJob_whenJobIsActive_setsCompletedState() {
            // given
            GenerationJob job = jobWithStatus(GenerationJobStatus.SAVING_RECOMMENDATION);
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            Instant before = Instant.now();

            // when
            generationJobService.completeJob(jobId, 42);

            // then
            assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.COMPLETED);
            assertThat(job.getRecommendationId()).isEqualTo(42);
            assertThat(job.getFinishedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(Instant.now());
            verify(eventPublisher).publishEvent(any(JobStatusChangedEvent.class));
        }

        @Test
        @DisplayName("rejects completing an already-terminal job with CONFLICT")
        void completeJob_whenJobIsAlreadyTerminal_throwsBusinessExceptionConflict() {
            // given
            when(generationJobRepository.findById(jobId))
                    .thenReturn(Optional.of(jobWithStatus(GenerationJobStatus.COMPLETED)));

            // when / then
            assertThatThrownBy(() -> generationJobService.completeJob(jobId, 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("failJob")
    class FailJob {

        @Test
        @DisplayName("marks an active job FAILED with errorMessage and finishedAt")
        void failJob_whenJobIsActive_setsFailedState() {
            // given
            GenerationJob job = jobWithStatus(GenerationJobStatus.GENERATING_WITH_AI);
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
            Instant before = Instant.now();

            // when
            generationJobService.failJob(jobId, "boom");

            // then
            assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.FAILED);
            assertThat(job.getErrorMessage()).isEqualTo("boom");
            assertThat(job.getFinishedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(Instant.now());
            verify(eventPublisher).publishEvent(any(JobStatusChangedEvent.class));
        }

        @Test
        @DisplayName("is idempotent: never flips a COMPLETED job to FAILED")
        void failJob_whenJobIsAlreadyTerminal_returnsWithoutChangingState() {
            // given
            GenerationJob job = jobWithStatus(GenerationJobStatus.COMPLETED);
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));

            // when
            generationJobService.failJob(jobId, "should be ignored");

            // then
            assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.COMPLETED);
            assertThat(job.getErrorMessage()).isNull();
            assertThat(job.getFinishedAt()).isNull();
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("getJobOwnerEmail")
    class GetJobOwnerEmail {

        @Test
        @DisplayName("returns the owning user's email")
        void getJobOwnerEmail_whenJobExists_returnsEmail() {
            // given
            User user = new User();
            user.setEmail("owner@test.com");
            GenerationJob job = jobWithStatus(GenerationJobStatus.PENDING);
            job.setUser(user);
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));

            // when
            String email = generationJobService.getJobOwnerEmail(jobId);

            // then
            assertThat(email).isEqualTo("owner@test.com");
        }

        @Test
        @DisplayName("throws NOT_FOUND when the job does not exist")
        void getJobOwnerEmail_whenJobNotFound_throwsBusinessExceptionNotFound() {
            // given
            when(generationJobRepository.findById(jobId)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> generationJobService.getJobOwnerEmail(jobId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("startJob")
    class StartJob {

        private final String email = "owner@test.com";

        private User ownerWithId(Integer id) {
            User user = new User();
            user.setId(id);
            user.setEmail(email);
            return user;
        }

        private Assessment completeAssessment() {
            Assessment assessment = new Assessment();
            assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
            assessment.setDietType(Assessment.DietType.ONIVORA);
            assessment.setMealsPerDay(3);
            return assessment;
        }

        private void stubReadyUser(User owner) {
            when(nutritionalProfileRepository.existsByUserId(owner.getId())).thenReturn(true);
            when(assessmentRepository.findByUserId(owner.getId())).thenReturn(Optional.of(completeAssessment()));
        }

        @Test
        @DisplayName("creates a PENDING job and returns created=true when profile and assessment are complete")
        void startJob_whenNoActiveJob_createsPendingJobAndReturnsCreated() {
            // given
            User owner = ownerWithId(1);
            when(userService.findByEmail(email)).thenReturn(owner);
            stubReadyUser(owner);
            when(generationJobRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                    1, GenerationJobStatus.activeStatuses())).thenReturn(Optional.empty());
            when(generationJobRepository.save(any(GenerationJob.class)))
                    .thenAnswer(invocation -> {
                        GenerationJob saved = invocation.getArgument(0);
                        saved.setId(jobId);
                        return saved;
                    });

            // when
            JobStartResult result = generationJobService.startJob(email);

            // then
            assertThat(result.created()).isTrue();
            assertThat(result.job().jobId()).isEqualTo(jobId);
            assertThat(result.job().status()).isEqualTo(GenerationJobStatus.PENDING);
            verify(generationJobRepository).save(any(GenerationJob.class));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("returns the active job with created=false and never saves when one already exists")
        void startJob_whenActiveJobExists_returnsAlreadyActiveWithoutSaving() {
            // given
            User owner = ownerWithId(1);
            GenerationJob active = jobWithStatus(GenerationJobStatus.GENERATING_WITH_AI);
            active.setId(jobId);
            when(userService.findByEmail(email)).thenReturn(owner);
            stubReadyUser(owner);
            when(generationJobRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                    1, GenerationJobStatus.activeStatuses())).thenReturn(Optional.of(active));

            // when
            JobStartResult result = generationJobService.startJob(email);

            // then
            assertThat(result.created()).isFalse();
            assertThat(result.job().jobId()).isEqualTo(jobId);
            assertThat(result.job().status()).isEqualTo(GenerationJobStatus.GENERATING_WITH_AI);
            verify(generationJobRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("throws UNPROCESSABLE_ENTITY and never creates a job when nutritional profile is missing")
        void startJob_whenNutritionalProfileMissing_throwsUnprocessableEntityAndDoesNotCreateJob() {
            // given
            User owner = ownerWithId(1);
            when(userService.findByEmail(email)).thenReturn(owner);
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(false);

            // when / then
            assertThatThrownBy(() -> generationJobService.startJob(email))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Perfil nutricional não encontrado")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            verify(generationJobRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNPROCESSABLE_ENTITY and never creates a job when assessment is missing")
        void startJob_whenAssessmentMissing_throwsUnprocessableEntityAndDoesNotCreateJob() {
            // given
            User owner = ownerWithId(1);
            when(userService.findByEmail(email)).thenReturn(owner);
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> generationJobService.startJob(email))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Triagem não encontrada")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            verify(generationJobRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNPROCESSABLE_ENTITY and never creates a job when assessment dietType is null")
        void startJob_whenAssessmentDietTypeIsNull_throwsUnprocessableEntityAndDoesNotCreateJob() {
            // given
            User owner = ownerWithId(1);
            when(userService.findByEmail(email)).thenReturn(owner);
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);
            Assessment incomplete = new Assessment();
            incomplete.setMealsPerDay(3);
            // dietType intentionally null
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(incomplete));

            // when / then
            assertThatThrownBy(() -> generationJobService.startJob(email))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Triagem incompleta")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            verify(generationJobRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNPROCESSABLE_ENTITY and never creates a job when assessment mealsPerDay is null")
        void startJob_whenAssessmentMealsPerDayIsNull_throwsUnprocessableEntityAndDoesNotCreateJob() {
            // given
            User owner = ownerWithId(1);
            when(userService.findByEmail(email)).thenReturn(owner);
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);
            Assessment incomplete = new Assessment();
            incomplete.setDietType(Assessment.DietType.ONIVORA);
            // mealsPerDay intentionally null
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(incomplete));

            // when / then
            assertThatThrownBy(() -> generationJobService.startJob(email))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Triagem incompleta")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            verify(generationJobRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getStatusForOwner")
    class GetStatusForOwner {

        private final String email = "owner@test.com";

        @Test
        @DisplayName("returns the job state when it belongs to the authenticated user")
        void getStatusForOwner_whenJobBelongsToUser_returnsState() {
            // given
            User owner = new User();
            owner.setId(1);
            GenerationJob job = jobWithStatus(GenerationJobStatus.PENDING);
            job.setId(jobId);
            when(userService.findByEmail(email)).thenReturn(owner);
            when(generationJobRepository.findByIdAndUserId(jobId, 1)).thenReturn(Optional.of(job));

            // when
            GenerationJobResponse response = generationJobService.getStatusForOwner(jobId, email);

            // then
            assertThat(response.jobId()).isEqualTo(jobId);
            assertThat(response.status()).isEqualTo(GenerationJobStatus.PENDING);
        }

        @Test
        @DisplayName("throws NOT_FOUND when the job does not belong to the user")
        void getStatusForOwner_whenJobNotOwnedByUser_throwsNotFound() {
            // given
            User owner = new User();
            owner.setId(1);
            when(userService.findByEmail(email)).thenReturn(owner);
            when(generationJobRepository.findByIdAndUserId(jobId, 1)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> generationJobService.getStatusForOwner(jobId, email))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
