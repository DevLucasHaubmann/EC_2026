package com.tukan.api.dto.ai;

public record AiRecommendationContext(
        AiUsuarioContext usuario,
        AiPerfilContext perfil,
        AiTriagemContext triagem
) {
}
