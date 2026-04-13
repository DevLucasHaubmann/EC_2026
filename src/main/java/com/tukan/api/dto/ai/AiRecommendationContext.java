package com.tukan.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiRecommendationContext(
        @JsonProperty("usuario")
        AiUsuarioContext user,

        @JsonProperty("perfil")
        AiPerfilContext profile,

        @JsonProperty("triagem")
        AiTriagemContext assessment
) {
}