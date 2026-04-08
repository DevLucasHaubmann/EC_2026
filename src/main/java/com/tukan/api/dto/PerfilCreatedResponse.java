package com.tukan.api.dto;

import com.tukan.api.entity.Perfil;

public record PerfilCreatedResponse(
        PerfilResponse perfil,
        boolean onboardingRequired,
        String nextStep
) {

    public static PerfilCreatedResponse of(Perfil perfil, OnboardingStatus onboarding) {
        return new PerfilCreatedResponse(
                PerfilResponse.from(perfil),
                onboarding.onboardingRequired(),
                onboarding.nextStep()
        );
    }
}