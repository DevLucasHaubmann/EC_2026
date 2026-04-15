package com.tukan.api.dto;

import com.tukan.api.entity.Assessment;

public record AssessmentCreatedResponse(
        AssessmentResponse assessment,
        boolean onboardingRequired,
        String nextStep
) {

    public static AssessmentCreatedResponse of(Assessment assessment, OnboardingStatus onboarding) {
        return new AssessmentCreatedResponse(
                AssessmentResponse.from(assessment),
                onboarding.onboardingRequired(),
                onboarding.nextStep()
        );
    }
}