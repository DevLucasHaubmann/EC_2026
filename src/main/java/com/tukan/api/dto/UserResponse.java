package com.tukan.api.dto;

import com.tukan.api.entity.User;

public record UserResponse(
        Integer id,
        String nome,
        String email,
        User.UserType tipo,
        User.UserState status
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getTipo(),
                user.getStatus()
        );
    }
}
