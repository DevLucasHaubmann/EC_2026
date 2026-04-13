package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiUsuarioContext(
        @JsonProperty("nome")
        String name
) {
}