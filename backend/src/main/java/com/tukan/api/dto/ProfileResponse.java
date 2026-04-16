package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.NutritionalProfile;

import java.time.Instant;
import java.time.LocalDate;

public record ProfileResponse(
        Integer id,

        @JsonProperty("userId")
        Integer userId,

        @JsonProperty("userName")
        String userName,

        @JsonProperty("dateOfBirth")
        LocalDate dateOfBirth,

        @JsonProperty("gender")
        NutritionalProfile.Gender gender,

        Double weightKg,
        Double heightCm,

        @JsonProperty("activityLevel")
        NutritionalProfile.ActivityLevel activityLevel,

        @JsonProperty("createdAt")
        Instant createdAt,

        @JsonProperty("updatedAt")
        Instant updatedAt
) {

    public static ProfileResponse from(NutritionalProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getWeightKg(),
                profile.getHeightCm(),
                profile.getActivityLevel(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}