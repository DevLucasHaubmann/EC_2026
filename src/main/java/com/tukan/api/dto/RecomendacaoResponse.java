package com.tukan.api.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.entity.Recomendacao;

import java.time.Instant;
import java.util.List;

public record RecomendacaoResponse(
        Integer id,
        Integer usuarioId,
        String resumo,
        List<String> recomendacoes,
        List<String> alertas,
        String provider,
        String model,
        Recomendacao.StatusRecomendacao status,
        Instant criadoEm
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RecomendacaoResponse from(Recomendacao recomendacao) {
        return new RecomendacaoResponse(
                recomendacao.getId(),
                recomendacao.getUsuario().getId(),
                recomendacao.getResumo(),
                parseJsonList(recomendacao.getRecomendacoes()),
                parseJsonList(recomendacao.getAlertas()),
                recomendacao.getProvider(),
                recomendacao.getModel(),
                recomendacao.getStatus(),
                recomendacao.getCriadoEm()
        );
    }

    private static List<String> parseJsonList(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
