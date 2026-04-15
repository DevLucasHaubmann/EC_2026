package com.tukan.api.service.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.*;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.service.ai.AiProviderClient;
import com.tukan.api.service.ai.AiProviderResult;
import com.tukan.api.service.ai.MealPlanPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanAiService {

    private final MealPlanEngine mealPlanEngine;
    private final MealPlanPromptBuilder promptBuilder;
    private final AiProviderClient aiProviderClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public MealPlanRecommendationResponse generate(String authenticatedEmail) {
        DailyMealPlan plan = mealPlanEngine.generatePlan(authenticatedEmail);
        MealPlanContext context = mealPlanEngine.buildContext(authenticatedEmail);

        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(plan, context);

            AiProviderResult providerResult = aiProviderClient.send(systemPrompt, userPrompt);

            MealPlanAiResponse aiResponse = parseResponse(providerResult.content());

            return new MealPlanRecommendationResponse(
                    "COMPLETE",
                    aiResponse.summary(),
                    plan,
                    aiResponse.mealExplanations(),
                    aiResponse.tips(),
                    aiResponse.alerts(),
                    providerResult.provider(),
                    providerResult.model(),
                    context
            );
        } catch (Exception e) {
            log.error("Falha ao enriquecer plano alimentar com IA. Retornando plano sem complemento da IA.", e);
            return buildFallbackResponse(plan, context);
        }
    }

    private MealPlanRecommendationResponse buildFallbackResponse(DailyMealPlan plan, MealPlanContext context) {
        return new MealPlanRecommendationResponse(
                "PARTIAL",
                "Plano alimentar gerado com sucesso. O complemento da IA está temporariamente indisponível.",
                plan,
                Map.of(),
                Collections.emptyList(),
                Collections.emptyList(),
                "fallback",
                "none",
                context
        );
    }

    private MealPlanAiResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AiProviderException("O provider de IA retornou uma resposta sem conteúdo.");
        }
        try {
            MealPlanAiResponse response = objectMapper.readValue(rawResponse, MealPlanAiResponse.class);
            validateResponse(response);
            return response;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao interpretar resposta da IA. Raw response: {}", rawResponse, e);
            throw new AiProviderException("A resposta da IA para o plano alimentar não pôde ser interpretada. Tente novamente.", e);
        }
    }

    private void validateResponse(MealPlanAiResponse response) {
        if (response.summary() == null || response.summary().isBlank()) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'summary' ausente.");
        }
        if (response.mealExplanations() == null || response.mealExplanations().isEmpty()) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'mealExplanations' ausente ou vazio.");
        }
        boolean hasBlankExplanation = response.mealExplanations().entrySet().stream()
                .anyMatch(e -> e.getValue() == null || e.getValue().isBlank());
        if (hasBlankExplanation) {
            throw new AiProviderException("Resposta da IA incompleta: 'mealExplanations' contém entradas sem texto.");
        }
        if (response.tips() == null) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'tips' ausente.");
        }
        if (response.alerts() == null) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'alerts' ausente.");
        }
    }
}