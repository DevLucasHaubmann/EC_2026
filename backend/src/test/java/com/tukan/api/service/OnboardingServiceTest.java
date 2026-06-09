package com.tukan.api.service;

import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.entity.Assessment;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private NutritionalProfileRepository nutritionalProfileRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Spy
    private AssessmentCompletenessPolicy completenessPolicy;

    @InjectMocks
    private OnboardingService onboardingService;

    private Assessment completeAssessment() {
        Assessment a = new Assessment();
        a.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        a.setDietType(Assessment.DietType.ONIVORA);
        a.setMealsPerDay(3);
        return a;
    }

    private Assessment incompleteAssessment() {
        Assessment a = new Assessment();
        a.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        // dietType e mealsPerDay ausentes
        return a;
    }

    @Nested
    class CheckOnboarding {

        @Test
        void shouldRequireOnboardingAndPointToProfileWhenNeitherExist() {
            // given
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(false);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when
            OnboardingStatus status = onboardingService.checkOnboarding(1);

            // then
            assertThat(status.onboardingRequired()).isTrue();
            assertThat(status.nextStep()).isEqualTo("/profiles/first-access");
        }

        @Test
        void shouldRequireOnboardingAndPointToAssessmentWhenOnlyProfileExists() {
            // given
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.empty());

            // when
            OnboardingStatus status = onboardingService.checkOnboarding(1);

            // then
            assertThat(status.onboardingRequired()).isTrue();
            assertThat(status.nextStep()).isEqualTo("/assessments");
        }

        @Test
        void shouldRequireOnboardingWhenAssessmentExistsButIsIncomplete() {
            // given — assessment com apenas goal preenchido (dietType e mealsPerDay nulos)
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(incompleteAssessment()));

            // when
            OnboardingStatus status = onboardingService.checkOnboarding(1);

            // then
            assertThat(status.onboardingRequired()).isTrue();
            assertThat(status.nextStep()).isEqualTo("/assessments");
        }

        @Test
        void shouldNotRequireOnboardingWhenBothProfileAndCompleteAssessmentExist() {
            // given
            when(nutritionalProfileRepository.existsByUserId(1)).thenReturn(true);
            when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(completeAssessment()));

            // when
            OnboardingStatus status = onboardingService.checkOnboarding(1);

            // then
            assertThat(status.onboardingRequired()).isFalse();
            assertThat(status.nextStep()).isEqualTo("/dashboard");
        }
    }
}
