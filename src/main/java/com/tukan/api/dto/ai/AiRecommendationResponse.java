package com.tukan.api.dto.ai;

import java.util.List;

public record AiRecommendationResponse(
        String resumo,
        List<String> recomendacoes,
        List<String> alertas
) {
}
