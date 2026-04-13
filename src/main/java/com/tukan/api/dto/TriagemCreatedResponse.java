package com.tukan.api.dto;

import com.tukan.api.entity.Assessment;

public record TriagemCreatedResponse(
        TriagemResponse triagem,
        boolean onboardingRequired,
        String nextStep
) {

    public static TriagemCreatedResponse of(Assessment assessment, OnboardingStatus onboarding) {
        return new TriagemCreatedResponse(
                TriagemResponse.from(assessment),
                onboarding.onboardingRequired(),
                onboarding.nextStep()
        );
    }
}