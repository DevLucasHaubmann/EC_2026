package com.tukan.api.service;

import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.entity.Assessment;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCompletenessPolicy completenessPolicy;

    @Transactional(readOnly = true)
    public OnboardingStatus checkOnboarding(Integer userId) {
        boolean hasProfile = nutritionalProfileRepository.existsByUserId(userId);
        boolean hasCompleteAssessment = hasCompleteAssessment(userId);

        boolean onboardingRequired = !hasProfile || !hasCompleteAssessment;
        String nextStep = resolveNextStep(hasProfile, hasCompleteAssessment);

        return new OnboardingStatus(onboardingRequired, nextStep);
    }

    private boolean hasCompleteAssessment(Integer userId) {
        Optional<Assessment> assessment = assessmentRepository.findByUserId(userId);
        return assessment.isPresent() && completenessPolicy.isComplete(assessment.get());
    }

    private String resolveNextStep(boolean hasProfile, boolean hasCompleteAssessment) {
        if (!hasProfile) return "/profiles/first-access";
        if (!hasCompleteAssessment) return "/assessments";
        return "/dashboard";
    }
}
