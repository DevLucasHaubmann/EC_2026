package com.tukan.api.service;

import com.tukan.api.dto.AdminUpdateAssessmentRequest;
import com.tukan.api.dto.CreateAssessmentRequest;
import com.tukan.api.dto.UpdateAssessmentRequest;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AssessmentService assessmentService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("user@test.com");
    }

    private CreateAssessmentRequest validCreateRequest() {
        // A complete, API-valid request: goal + dietType + mealsPerDay are all required since B3.
        return new CreateAssessmentRequest(
                Assessment.NutritionalGoal.WEIGHT_LOSS,
                Assessment.DietType.VEGETARIANA,
                null, null, null, 3, null
        );
    }

    @Nested
    class CreateOwn {

        @Test
        void shouldCreateAssessmentWithRequiredFieldsSuccessfully() {
            // given
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.existsByUserId(1)).thenReturn(false);
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
                Assessment a = inv.getArgument(0);
                a.setId(1);
                return a;
            });

            // when
            Assessment result = assessmentService.createOwn("user@test.com", validCreateRequest());

            // then
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.WEIGHT_LOSS);
            assertThat(result.getMealsPerDay()).isEqualTo(3);
            assertThat(result.getUser()).isEqualTo(user);
            verify(assessmentRepository).save(any(Assessment.class));
        }

        @Test
        void shouldThrowConflictWhenAssessmentAlreadyExists() {
            // given
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.existsByUserId(1)).thenReturn(true);

            // when / then
            assertThatThrownBy(() -> assessmentService.createOwn("user@test.com", validCreateRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já possui uma triagem");
        }
    }

    @Nested
    class CreateWithDietType {

        @Test
        void shouldPersistDietTypeWhenProvided() {
            // given
            CreateAssessmentRequest request = new CreateAssessmentRequest(
                    Assessment.NutritionalGoal.WEIGHT_LOSS,
                    Assessment.DietType.VEGETARIANA,
                    null, null, null, 3, null
            );
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.existsByUserId(1)).thenReturn(false);
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.createOwn("user@test.com", request);

            // then
            assertThat(result.getDietType()).isEqualTo(Assessment.DietType.VEGETARIANA);
        }
    }

    @Nested
    class UpdateDietType {

        @Test
        void shouldUpdateDietTypeAlone() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(1);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);

            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    null, Assessment.DietType.VEGANA, null, null, null, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.updateOwn("user@test.com", request);

            // then
            assertThat(result.getDietType()).isEqualTo(Assessment.DietType.VEGANA);
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.MAINTENANCE);
        }
    }

    @Nested
    class FindOwn {

        @Test
        void shouldReturnAssessmentForAuthenticatedUser() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(1);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));

            // when
            Assessment result = assessmentService.findOwn("user@test.com");

            // then
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.MAINTENANCE);
        }

        @Test
        void shouldThrowNotFoundWhenAssessmentDoesNotExist() {
            // given
            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> assessmentService.findOwn("user@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Triagem não encontrada");
        }
    }

    @Nested
    class UpdateOwn {

        @Test
        void shouldUpdateGoalWhenOnlyGoalIsProvided() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(1);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.WEIGHT_LOSS);

            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    Assessment.NutritionalGoal.MUSCLE_GAIN, null, null, null, null, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.updateOwn("user@test.com", request);

            // then
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.MUSCLE_GAIN);
        }

        @Test
        void shouldThrowBadRequestWhenAllFieldsAreNull() {
            // given
            UpdateAssessmentRequest emptyRequest = new UpdateAssessmentRequest(
                    null, null, null, null, null, null, null);

            // when / then
            assertThatThrownBy(() -> assessmentService.updateOwn("user@test.com", emptyRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pelo menos um campo");
        }

        @Test
        void shouldThrowNotFoundWhenAssessmentDoesNotExistOnUpdate() {
            // given
            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    Assessment.NutritionalGoal.WEIGHT_LOSS, null, null, null, null, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> assessmentService.updateOwn("user@test.com", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Triagem não encontrada");
        }
    }

    @Nested
    class UpdateOwnPreservesFields {

        @Test
        void shouldPreserveMealsPerDayWhenOnlyGoalIsProvided() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(1);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.WEIGHT_LOSS);
            assessment.setDietType(Assessment.DietType.ONIVORA);
            assessment.setMealsPerDay(3);

            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    Assessment.NutritionalGoal.MUSCLE_GAIN, null, null, null, null, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.updateOwn("user@test.com", request);

            // then
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.MUSCLE_GAIN);
            assertThat(result.getMealsPerDay()).isEqualTo(3);
        }

        @Test
        void shouldPreserveTargetWeightKgWhenNotProvided() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(1);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.WEIGHT_LOSS);
            assessment.setMealsPerDay(4);
            assessment.setTargetWeightKg(70.0);

            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    Assessment.NutritionalGoal.MAINTENANCE, null, null, null, null, null, null);

            when(userService.findByEmail("user@test.com")).thenReturn(user);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.updateOwn("user@test.com", request);

            // then
            assertThat(result.getTargetWeightKg()).isEqualTo(70.0);
        }
    }

    @Nested
    class AdminUpdate {

        @Test
        void shouldUpdateDietTypeViaAdminUpdate() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(5);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);

            AdminUpdateAssessmentRequest request = new AdminUpdateAssessmentRequest(
                    null, Assessment.DietType.PESCATARIANA, null, null, null, null, null);

            when(assessmentRepository.findById(5)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.update(5, request);

            // then
            assertThat(result.getDietType()).isEqualTo(Assessment.DietType.PESCATARIANA);
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.MAINTENANCE);
        }

        @Test
        void shouldThrowBadRequestWhenAllFieldsNullInAdminUpdate() {
            // given
            AdminUpdateAssessmentRequest emptyRequest = new AdminUpdateAssessmentRequest(
                    null, null, null, null, null, null, null);

            // when / then
            assertThatThrownBy(() -> assessmentService.update(1, emptyRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pelo menos um campo");
        }

        @Test
        void shouldPreserveMealsPerDayWhenOnlyGoalIsProvidedInAdminUpdate() {
            // given
            Assessment assessment = new Assessment();
            assessment.setId(5);
            assessment.setUser(user);
            assessment.setGoal(Assessment.NutritionalGoal.WEIGHT_LOSS);
            assessment.setDietType(Assessment.DietType.ONIVORA);
            assessment.setMealsPerDay(5);

            AdminUpdateAssessmentRequest request = new AdminUpdateAssessmentRequest(
                    Assessment.NutritionalGoal.MAINTENANCE, null, null, null, null, null, null);

            when(assessmentRepository.findById(5)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Assessment result = assessmentService.update(5, request);

            // then
            assertThat(result.getGoal()).isEqualTo(Assessment.NutritionalGoal.MAINTENANCE);
            assertThat(result.getMealsPerDay()).isEqualTo(5);
        }
    }
}
