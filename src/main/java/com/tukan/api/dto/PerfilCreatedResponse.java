package com.tukan.api.dto;

import com.tukan.api.entity.NutritionalProfile;

public record PerfilCreatedResponse(
        PerfilResponse perfil,
        boolean onboardingRequired,
        String nextStep
) {

    public static PerfilCreatedResponse of(NutritionalProfile profile, OnboardingStatus onboarding) {
        return new PerfilCreatedResponse(
                PerfilResponse.from(profile),
                onboarding.onboardingRequired(),
                onboarding.nextStep()
        );
    }
}