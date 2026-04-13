package com.tukan.api.dto.ai.provider.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaudeMessageResponse(
        List<ClaudeContentBlockResponse> content
) {
}
