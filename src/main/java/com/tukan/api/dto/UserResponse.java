package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.User;

public record UserResponse(
        Integer id,

        @JsonProperty("name")
        String name,

        String email,

        @JsonProperty("type")
        User.UserType type,

        User.UserState status
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getType(),
                user.getStatus()
        );
    }
}
