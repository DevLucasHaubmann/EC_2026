package com.tukan.api.dto.ai.provider.claude;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeMessageRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        double temperature,
        String system,
        List<ClaudeMessageEntry> messages
) {

    public static ClaudeMessageRequest of(String model, int maxTokens, double temperature,
                                           String systemPrompt, String userPrompt) {
        List<ClaudeMessageEntry> messages = List.of(ClaudeMessageEntry.user(userPrompt));
        return new ClaudeMessageRequest(model, maxTokens, temperature, systemPrompt, messages);
    }
}
