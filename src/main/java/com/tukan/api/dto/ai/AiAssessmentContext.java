package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiAssessmentContext(
        @JsonProperty("goal")
        String goal,

        @JsonProperty("dietaryRestrictions")
        List<String> dietaryRestrictions,

        @JsonProperty("allergies")
        List<String> allergies,

        @JsonProperty("healthConditions")
        List<String> healthConditions
) {
}
