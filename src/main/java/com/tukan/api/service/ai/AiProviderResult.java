package com.tukan.api.service.ai;

public record AiProviderResult(
        String content,
        String provider,
        String model
) {
}
