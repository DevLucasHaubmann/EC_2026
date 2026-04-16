package com.tukan.api.dto.ai.provider.claude;

import java.util.List;

public record ClaudeMessageEntry(
        String role,
        List<ClaudeContentBlockRequest> content
) {

    public static ClaudeMessageEntry user(String text) {
        return new ClaudeMessageEntry("user", List.of(ClaudeContentBlockRequest.text(text)));
    }
}