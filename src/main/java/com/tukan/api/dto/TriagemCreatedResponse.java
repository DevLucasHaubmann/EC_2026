package com.tukan.api.dto;

import com.tukan.api.entity.Triagem;

public record TriagemCreatedResponse(
        TriagemResponse triagem,
        boolean onboardingRequired,
        String nextStep
) {

    public static TriagemCreatedResponse of(Triagem triagem, OnboardingStatus onboarding) {
        return new TriagemCreatedResponse(
                TriagemResponse.from(triagem),
                onboarding.onboardingRequired(),
                onboarding.nextStep()
        );
    }
}
