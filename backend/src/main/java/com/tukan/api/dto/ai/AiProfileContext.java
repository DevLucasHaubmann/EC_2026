package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiProfileContext(
        @JsonProperty("gender")
        String gender,

        @JsonProperty("age")
        int age,

        @JsonProperty("weightKg")
        double weightKg,

        @JsonProperty("heightCm")
        double heightCm,

        @JsonProperty("activityLevel")
        String activityLevel
) {
}