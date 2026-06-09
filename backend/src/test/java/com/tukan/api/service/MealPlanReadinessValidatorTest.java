package com.tukan.api.service;

import com.tukan.api.entity.Assessment;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanReadinessValidator — readiness gate compartilhado (sync e async)")
class MealPlanReadinessValidatorTest {

    @Mock
    private NutritionalProfileRepository nutritionalProfileRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    private MealPlanReadinessValidator validator;

    private static final Integer USER_ID = 7;

    private MealPlanReadinessValidator newValidator() {
        return new MealPlanReadinessValidator(
                nutritionalProfileRepository, assessmentRepository, new AssessmentCompletenessPolicy());
    }

    private Assessment completeAssessment() {
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        assessment.setDietType(Assessment.DietType.ONIVORA);
        assessment.setMealsPerDay(3);
        return assessment;
    }

    @Test
    @DisplayName("perfil e triagem completos: não lança")
    void completeUser_passes() {
        // given
        validator = newValidator();
        when(nutritionalProfileRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(assessmentRepository.findByUserId(USER_ID)).thenReturn(Optional.of(completeAssessment()));

        // when / then
        assertThatCode(() -> validator.validate(USER_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil ausente: lança UNPROCESSABLE_ENTITY")
    void missingProfile_throws() {
        // given
        validator = newValidator();
        when(nutritionalProfileRepository.existsByUserId(USER_ID)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> validator.validate(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Perfil nutricional não encontrado")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("triagem ausente: lança UNPROCESSABLE_ENTITY")
    void missingAssessment_throws() {
        // given
        validator = newValidator();
        when(nutritionalProfileRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(assessmentRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> validator.validate(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Triagem não encontrada")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("triagem incompleta (dietType nulo): lança erro claro de triagem incompleta")
    void incompleteAssessment_throws() {
        // given
        validator = newValidator();
        Assessment incomplete = new Assessment();
        incomplete.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        incomplete.setMealsPerDay(3);
        // dietType intentionally null
        when(nutritionalProfileRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(assessmentRepository.findByUserId(USER_ID)).thenReturn(Optional.of(incomplete));

        // when / then
        assertThatThrownBy(() -> validator.validate(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Triagem incompleta")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
