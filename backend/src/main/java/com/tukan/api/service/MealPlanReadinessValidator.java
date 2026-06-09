package com.tukan.api.service;

import com.tukan.api.entity.Assessment;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for the readiness gate that must pass before any meal plan generation,
 * synchronous or asynchronous. A user is ready when a nutritional profile exists and a complete
 * assessment (goal + dietType + mealsPerDay) is present.
 *
 * <p>Both the async flow ({@code GenerationJobService}) and the synchronous flow
 * ({@code AiRecommendationService}) delegate here so the engine is never reached with an
 * incomplete profile/assessment — the user gets a clear triage error instead of an engine error.
 */
@Component
@RequiredArgsConstructor
public class MealPlanReadinessValidator {

    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCompletenessPolicy completenessPolicy;

    /**
     * Validates that the user is ready for meal plan generation.
     *
     * @param userId id of the user to validate
     * @throws BusinessException ({@link HttpStatus#UNPROCESSABLE_ENTITY}) when the profile is
     *                           missing, the assessment is missing, or the assessment is incomplete
     */
    public void validate(Integer userId) {
        if (!nutritionalProfileRepository.existsByUserId(userId)) {
            throw new BusinessException(
                    "Perfil nutricional não encontrado. Complete seu perfil antes de gerar um plano alimentar.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Assessment assessment = assessmentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "Triagem não encontrada. Complete sua triagem antes de gerar um plano alimentar.",
                        HttpStatus.UNPROCESSABLE_ENTITY));
        if (!completenessPolicy.isComplete(assessment)) {
            throw new BusinessException(
                    "Triagem incompleta. Informe objetivo, tipo de dieta e número de refeições antes de gerar um plano alimentar.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
