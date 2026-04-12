package com.tukan.api.service;

import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final NutritionalProfileRepository nutritionalProfileRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    public OnboardingStatus checkOnboarding(Integer userId) {
        boolean hasProfile = nutritionalProfileRepository.existsByUserId(userId);
        boolean hasAssessment = assessmentRepository.existsByUserId(userId);

        boolean onboardingRequired = !hasProfile || !hasAssessment;
        String nextStep = resolveNextStep(hasProfile, hasAssessment);

        return new OnboardingStatus(onboardingRequired, nextStep);
    }

    private String resolveNextStep(boolean hasProfile, boolean hasAssessment) {
        if (!hasProfile) return "/perfil/primeiro-acesso";
        if (!hasAssessment) return "/triagem";
        return "/dashboard";
    }
}
