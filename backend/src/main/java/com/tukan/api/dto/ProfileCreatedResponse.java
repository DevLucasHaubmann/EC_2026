package com.tukan.api.dto;

import com.tukan.api.entity.NutritionalProfile;

public record ProfileCreatedResponse(
        ProfileResponse profile,
        boolean onboardingRequired,
        String nextStep
) {

    public static ProfileCreatedResponse of(NutritionalProfile profile, OnboardingStatus onboarding) {
        return new ProfileCreatedResponse(
                ProfileResponse.from(profile),
                onboarding.onboardingRequired(),
                onboarding.nextStep()
        );
    }
}