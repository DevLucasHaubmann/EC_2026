package com.tukan.api.dto.ai.provider.claude;

public record ClaudeContentBlockRequest(
        String type,
        String text
) {

    public static ClaudeContentBlockRequest text(String text) {
        return new ClaudeContentBlockRequest("text", text);
    }
}
