package com.tukan.api.service.ai;

import com.tukan.api.config.AiProperties;
import com.tukan.api.dto.ai.provider.claude.ClaudeMessageRequest;
import com.tukan.api.dto.ai.provider.claude.ClaudeMessageResponse;
import com.tukan.api.exception.AiProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
@RequiredArgsConstructor
public class ClaudeAiProviderClient implements AiProviderClient {

    private static final String MESSAGES_ENDPOINT = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient claudeRestClient;
    private final AiProperties aiProperties;

    @Override
    public String send(String systemPrompt, String userPrompt) {
        AiProperties.Claude claude = aiProperties.getClaude();

        validateApiKey(claude.getApiKey());

        ClaudeMessageRequest request = ClaudeMessageRequest.of(
                claude.getModel(),
                claude.getMaxTokens(),
                claude.getTemperature(),
                systemPrompt,
                userPrompt
        );

        try {
            ClaudeMessageResponse response = claudeRestClient.post()
                    .uri(MESSAGES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", claude.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(request)
                    .retrieve()
                    .body(ClaudeMessageResponse.class);

            return extractText(response);
        } catch (RestClientException e) {
            throw new AiProviderException("Falha na comunicação com o provider Claude: " + e.getMessage(), e);
        }
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException(
                    "API key do Claude não configurada. Verifique a propriedade 'ai.claude.api-key'.");
        }
    }

    private String extractText(ClaudeMessageResponse response) {
        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new AiProviderException("O provider Claude retornou uma resposta sem conteúdo.");
        }

        return response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(block -> block.text())
                .findFirst()
                .orElseThrow(() -> new AiProviderException("O provider Claude retornou uma resposta sem bloco de texto."));
    }
}
