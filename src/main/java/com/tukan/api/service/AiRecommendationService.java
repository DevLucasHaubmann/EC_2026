package com.tukan.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.dto.ai.AiRecommendationResponse;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.service.ai.AiPromptBuilder;
import com.tukan.api.service.ai.AiProviderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final UserService userService;
    private final AiContextService aiContextService;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiProviderClient aiProviderClient;
    private final ObjectMapper objectMapper;

    public AiRecommendationResponse generateRecommendation(String authenticatedEmail) {
        Integer usuarioId = userService.findByEmail(authenticatedEmail).getId();

        AiRecommendationContext contexto = aiContextService.build(usuarioId);

        String systemPrompt = aiPromptBuilder.buildSystemPrompt();
        String userPrompt = aiPromptBuilder.buildUserPrompt(contexto);

        String rawResponse = aiProviderClient.send(systemPrompt, userPrompt);

        return parseResponse(rawResponse);
    }

    private AiRecommendationResponse parseResponse(String rawResponse) {
        try {
            AiRecommendationResponse response = objectMapper.readValue(rawResponse, AiRecommendationResponse.class);
            validateResponse(response);
            return response;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("A resposta da IA não pôde ser interpretada. Tente novamente.", e);
        }
    }

    private void validateResponse(AiRecommendationResponse response) {
        if (response.resumo() == null || response.resumo().isBlank()) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'resumo' ausente.");
        }
        if (response.recomendacoes() == null || response.recomendacoes().isEmpty()) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'recomendacoes' ausente.");
        }
    }
}
