package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.User;
import com.tukan.api.service.MeService.AuthenticatedUserData;

public record MeResponse(
        Integer id,

        @JsonProperty("name")
        String name,

        String email,

        @JsonProperty("type")
        User.UserType type,

        User.UserState status,

        @JsonProperty("profile")
        ProfileResponse profile,

        @JsonProperty("assessment")
        AssessmentResponse assessment
) {

    public static MeResponse from(AuthenticatedUserData data) {
        return new MeResponse(
                data.user().getId(),
                data.user().getName(),
                data.user().getEmail(),
                data.user().getType(),
                data.user().getStatus(),
                data.profile() != null ? ProfileResponse.from(data.profile()) : null,
                data.assessment() != null ? AssessmentResponse.from(data.assessment()) : null
        );
    }
}
