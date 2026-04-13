package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.Assessment;
import com.tukan.api.service.MeService.AuthenticatedUserData;

import java.time.LocalDate;

public record DashboardResponse(
        @JsonProperty("nome")
        String name,

        @JsonProperty("perfil")
        ProfileSummaryResponse profile,

        @JsonProperty("triagem")
        AssessmentSummaryResponse assessment,

        OnboardingStatus onboarding
) {

    public static DashboardResponse from(AuthenticatedUserData data, OnboardingStatus onboarding) {
        return new DashboardResponse(
                data.user().getName(),
                data.profile() != null ? ProfileSummaryResponse.from(data.profile()) : null,
                data.assessment() != null ? AssessmentSummaryResponse.from(data.assessment()) : null,
                onboarding
        );
    }

    public record ProfileSummaryResponse(
            Double pesoKg,
            Double alturaCm,
            Double imc,

            @JsonProperty("classificacaoImc")
            String bmiClassification,

            @JsonProperty("nivelAtividade")
            NutritionalProfile.ActivityLevel activityLevel,

            @JsonProperty("sexo")
            NutritionalProfile.Gender gender,

            @JsonProperty("idade")
            Integer age
    ) {

        public static ProfileSummaryResponse from(NutritionalProfile profile) {
            double bmi = calculateBmi(profile.getWeightKg(), profile.getHeightCm());
            int age = profile.calculateAge(LocalDate.now());

            return new ProfileSummaryResponse(
                    profile.getWeightKg(),
                    profile.getHeightCm(),
                    Math.round(bmi * 100.0) / 100.0,
                    classify(bmi),
                    profile.getActivityLevel(),
                    profile.getGender(),
                    age
            );
        }

        private static double calculateBmi(double weightKg, double heightCm) {
            double heightM = heightCm / 100.0;
            return weightKg / (heightM * heightM);
        }

        private static String classify(double bmi) {
            if (bmi < 18.5) return "Abaixo do peso";
            if (bmi < 25.0) return "Peso normal";
            if (bmi < 30.0) return "Sobrepeso";
            if (bmi < 35.0) return "Obesidade grau I";
            if (bmi < 40.0) return "Obesidade grau II";
            return "Obesidade grau III";
        }
    }

    public record AssessmentSummaryResponse(
            @JsonProperty("objetivo")
            Assessment.NutritionalGoal goal,

            @JsonProperty("possuiRestricoes")
            boolean hasRestrictions,

            @JsonProperty("possuiAlergias")
            boolean hasAllergies,

            @JsonProperty("possuiCondicoesSaude")
            boolean hasHealthConditions
    ) {

        public static AssessmentSummaryResponse from(Assessment assessment) {
            return new AssessmentSummaryResponse(
                    assessment.getGoal(),
                    hasContent(assessment.getDietaryRestrictions()),
                    hasContent(assessment.getAllergies()),
                    hasContent(assessment.getHealthConditions())
            );
        }

        private static boolean hasContent(String value) {
            return value != null && !value.isBlank();
        }
    }
}