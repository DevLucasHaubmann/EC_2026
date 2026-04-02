package com.tukan.api.dto;

public record AuthResponse(
        String token,
        String type,
        String message
) {
}
