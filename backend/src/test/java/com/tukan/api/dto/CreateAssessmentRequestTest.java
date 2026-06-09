package com.tukan.api.dto;

import com.tukan.api.entity.Assessment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure Bean Validation unit test for CreateAssessmentRequest — the payload used by both the
 * user self-service and the admin "create triagem for user" endpoint.
 *
 * Guards the API boundary directly: even if the UI is bypassed, an assessment cannot be created
 * without goal, dietType and mealsPerDay. No Spring context — uses the Jakarta Validator.
 */
class CreateAssessmentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<CreateAssessmentRequest>> validate(CreateAssessmentRequest request) {
        return validator.validate(request);
    }

    private CreateAssessmentRequest complete() {
        return new CreateAssessmentRequest(
                Assessment.NutritionalGoal.WEIGHT_LOSS,
                Assessment.DietType.ONIVORA,
                null, null, null, 4, null);
    }

    @Test
    @DisplayName("payload completo (goal + dietType + mealsPerDay) — válido")
    void completePayload_isValid() {
        // given / when
        Set<ConstraintViolation<CreateAssessmentRequest>> violations = validate(complete());

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("goal ausente — inválido")
    void missingGoal_isInvalid() {
        // given
        CreateAssessmentRequest request = new CreateAssessmentRequest(
                null, Assessment.DietType.ONIVORA, null, null, null, 4, null);

        // when
        Set<ConstraintViolation<CreateAssessmentRequest>> violations = validate(request);

        // then
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("O objetivo nutricional é obrigatório."));
    }

    @Test
    @DisplayName("dietType ausente — inválido")
    void missingDietType_isInvalid() {
        // given
        CreateAssessmentRequest request = new CreateAssessmentRequest(
                Assessment.NutritionalGoal.WEIGHT_LOSS, null, null, null, null, 4, null);

        // when
        Set<ConstraintViolation<CreateAssessmentRequest>> violations = validate(request);

        // then
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("O tipo de dieta é obrigatório."));
    }

    @Test
    @DisplayName("mealsPerDay ausente — inválido")
    void missingMealsPerDay_isInvalid() {
        // given
        CreateAssessmentRequest request = new CreateAssessmentRequest(
                Assessment.NutritionalGoal.WEIGHT_LOSS, Assessment.DietType.ONIVORA,
                null, null, null, null, null);

        // when
        Set<ConstraintViolation<CreateAssessmentRequest>> violations = validate(request);

        // then
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("O número de refeições por dia é obrigatório."));
    }

    @Test
    @DisplayName("mealsPerDay fora do intervalo (6) — inválido")
    void mealsPerDayOutOfRange_isInvalid() {
        // given
        CreateAssessmentRequest request = new CreateAssessmentRequest(
                Assessment.NutritionalGoal.WEIGHT_LOSS, Assessment.DietType.ONIVORA,
                null, null, null, 6, null);

        // when
        Set<ConstraintViolation<CreateAssessmentRequest>> violations = validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("todos os campos obrigatórios ausentes — inválido (3 violações)")
    void allRequiredMissing_isInvalid() {
        // given
        CreateAssessmentRequest request = new CreateAssessmentRequest(
                null, null, null, null, null, null, null);

        // when
        Set<ConstraintViolation<CreateAssessmentRequest>> violations = validate(request);

        // then
        assertThat(violations).hasSize(3);
    }
}
