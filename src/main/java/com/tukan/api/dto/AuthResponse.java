package com.tukan.api.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String type,
        long expiresIn,
        String message
) {
}
