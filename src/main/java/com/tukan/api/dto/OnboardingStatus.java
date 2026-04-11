package com.tukan.api.dto;

public record OnboardingStatus(
        boolean onboardingRequired,
        String nextStep
) {
}
