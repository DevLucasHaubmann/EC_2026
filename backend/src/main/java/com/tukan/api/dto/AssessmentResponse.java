package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.Assessment;

import java.time.Instant;

public record AssessmentResponse(
        Integer id,

        @JsonProperty("userId")
        Integer userId,

        @JsonProperty("userName")
        String userName,

        @JsonProperty("goal")
        Assessment.NutritionalGoal goal,

        @JsonProperty("dietaryRestrictions")
        String dietaryRestrictions,

        @JsonProperty("allergies")
        String allergies,

        @JsonProperty("healthConditions")
        String healthConditions,

        @JsonProperty("mealsPerDay")
        Integer mealsPerDay,

        @JsonProperty("targetWeightKg")
        Double targetWeightKg,

        @JsonProperty("createdAt")
        Instant createdAt,

        @JsonProperty("updatedAt")
        Instant updatedAt
) {

    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getUser().getId(),
                assessment.getUser().getName(),
                assessment.getGoal(),
                assessment.getDietaryRestrictions(),
                assessment.getAllergies(),
                assessment.getHealthConditions(),
                assessment.getMealsPerDay(),
                assessment.getTargetWeightKg(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
        );
    }
}