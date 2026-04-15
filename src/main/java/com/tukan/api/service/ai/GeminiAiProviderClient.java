package com.tukan.api.service.ai;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.tukan.api.config.AiProperties;
import com.tukan.api.exception.AiProviderException;
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

    private final Client geminiClient;
    private final AiProperties aiProperties;

    @Override
    public AiProviderResult send(String systemPrompt, String userPrompt) {
        AiProperties.Gemini gemini = aiProperties.getGemini();

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
            return new AiProviderResult(content, "gemini", gemini.getModel());
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na comunicação com o provider Gemini.", e);
            throw new AiProviderException("Falha na comunicação com o provider de IA. Tente novamente em instantes.", e);
        }
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.text() == null || response.text().isBlank()) {
            throw new AiProviderException("O provider Gemini retornou uma resposta sem conteúdo.");
        }
        return response.text();
    }
}
