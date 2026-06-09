package com.tukan.api.service.ai;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.tukan.api.config.AiProperties;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiAiProviderClient implements AiProviderClient {

    private static final String PROVIDER_NAME = "gemini";

    private final Client geminiClient;
    private final AiProperties aiProperties;

    @Override
    public AiProviderResult send(String systemPrompt, String userPrompt) {
        AiProperties.Gemini gemini = aiProperties.getGemini();

        // Logs config posture without ever exposing the key value.
        log.info("Calling Gemini provider: model={}, apiKeyConfigured={}, maxTokens={}",
                gemini.getModel(), isApiKeyConfigured(gemini), gemini.getMaxTokens());

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .temperature((float) gemini.getTemperature())
                .maxOutputTokens(gemini.getMaxTokens())
                .responseMimeType("application/json")
                .build();

        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    gemini.getModel(),
                    List.of(Content.fromParts(Part.fromText(userPrompt))),
                    config
            );

            String content = extractText(response);
            log.debug("Gemini provider responded: model={}, contentLength={}",
                    gemini.getModel(), content.length());
            return new AiProviderResult(content, PROVIDER_NAME, gemini.getModel());
        } catch (AiProviderException e) {
            // Empty/blank-content case from extractText: already a sanitized domain failure.
            log.error("Gemini returned no usable content: model={}, reason={}",
                    gemini.getModel(), LogSanitizer.sanitize(e.getMessage()));
            throw e;
        } catch (Exception e) {
            // Real provider failure (auth, quota, network, timeout). The underlying cause is logged
            // here — where it is available — sanitized and truncated, before being wrapped.
            log.error("Gemini provider call failed: model={}, causeType={}, cause={}",
                    gemini.getModel(), e.getClass().getSimpleName(), LogSanitizer.sanitize(e.getMessage()));
            throw new AiProviderException("Falha na comunicação com o provider Gemini.", e);
        }
    }

    private boolean isApiKeyConfigured(AiProperties.Gemini gemini) {
        return gemini.getApiKey() != null && !gemini.getApiKey().isBlank();
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.text() == null || response.text().isBlank()) {
            throw new AiProviderException("O provider Gemini retornou uma resposta sem conteúdo.");
        }
        return response.text();
    }
}
