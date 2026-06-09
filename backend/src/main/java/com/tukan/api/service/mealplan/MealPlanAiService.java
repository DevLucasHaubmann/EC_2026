package com.tukan.api.service.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.*;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.service.ai.AiProviderClient;
import com.tukan.api.service.ai.AiProviderResult;
import com.tukan.api.service.ai.MealPlanPromptBuilder;
import com.tukan.api.util.LogSanitizer;
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

    /**
     * Phase 1 — Preparation. Loads user data and builds the deterministic plan and
     * context inside a short read-only transaction. The connection is released before
     * the external AI call happens.
     */
    @Transactional(readOnly = true)
    public MealPlanPreparation prepare(String authenticatedEmail) {
        DailyMealPlan plan = mealPlanEngine.generatePlan(authenticatedEmail);
        MealPlanContext context = mealPlanEngine.buildContext(authenticatedEmail);
        return new MealPlanPreparation(plan, context);
    }

    /**
     * Phase 2 — AI enrichment. Calls the external provider, parses and validates the
     * response, and falls back to a partial plan on failure. Intentionally NOT
     * transactional: it must not hold a database connection during the long external call.
     */
    public MealPlanGenerationResult enrich(MealPlanPreparation preparation) {
        DailyMealPlan plan = preparation.plan();
        MealPlanContext context = preparation.context();

        log.info("AI meal plan enrichment started: meals={}", plan.meals().size());

        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(plan, context);
            // Prompt content is never logged — only a non-reversible size+hash fingerprint.
            log.debug("AI prompt built: system[{}], user[{}]",
                    LogSanitizer.fingerprint(systemPrompt), LogSanitizer.fingerprint(userPrompt));

            AiProviderResult providerResult = aiProviderClient.send(systemPrompt, userPrompt);

            MealPlanAiResponse aiResponse = parseResponse(providerResult.content());

            MealPlanRecommendationResponse response = new MealPlanRecommendationResponse(
                    "COMPLETO",
                    aiResponse.summary(),
                    plan,
                    aiResponse.mealExplanations(),
                    aiResponse.tips(),
                    aiResponse.alerts(),
                    providerResult.provider(),
                    providerResult.model()
            );
            log.info("AI meal plan enrichment succeeded: provider={}, model={}, status=COMPLETO",
                    providerResult.provider(), providerResult.model());
            return new MealPlanGenerationResult(response, context);
        } catch (AiProviderException e) {
            // Expected AI degradation: provider/parse/validation failure. PARTIAL fallback is the
            // designed response, so this is WARN. The real provider cause (when any) was already
            // logged at ERROR inside the provider client.
            log.warn("AI meal plan enrichment failed, applying PARTIAL fallback: "
                            + "errorType={}, rootCauseType={}, reason={}",
                    e.getClass().getSimpleName(), rootCauseType(e), LogSanitizer.sanitize(e.getMessage()));
            return buildFallbackResult(plan, context);
        } catch (Exception e) {
            // Unexpected failure (not an AI degradation): a real bug must surface at ERROR. The
            // fallback is still applied so the user keeps the deterministic plan.
            log.error("AI meal plan enrichment failed unexpectedly, applying PARTIAL fallback: "
                            + "errorType={}, rootCauseType={}, reason={}",
                    e.getClass().getSimpleName(), rootCauseType(e), LogSanitizer.sanitize(e.getMessage()));
            return buildFallbackResult(plan, context);
        }
    }

    /** Simple name of the deepest cause in the chain — exposes the real failure behind the wrapper. */
    private String rootCauseType(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName();
    }

    private MealPlanGenerationResult buildFallbackResult(DailyMealPlan plan, MealPlanContext context) {
        MealPlanRecommendationResponse response = new MealPlanRecommendationResponse(
                "PARCIAL",
                "Plano alimentar gerado com sucesso. O complemento da IA está temporariamente indisponível.",
                plan,
                Map.of(),
                Collections.emptyList(),
                Collections.emptyList(),
                "fallback",
                "none"
        );
        return new MealPlanGenerationResult(response, context);
    }

    private MealPlanAiResponse parseResponse(String rawResponse) {
        try {
            MealPlanAiResponse response = objectMapper.readValue(rawResponse, MealPlanAiResponse.class);
            validateResponse(response);
            return response;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("A resposta da IA para o plano alimentar não pôde ser interpretada. Tente novamente.", e);
        }
    }

    private void validateResponse(MealPlanAiResponse response) {
        if (response.summary() == null || response.summary().isBlank()) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'resumo' ausente.");
        }
        if (response.mealExplanations() == null || response.mealExplanations().isEmpty()) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'explicacaoRefeicoes' ausente.");
        }
        boolean hasBlankExplanation = response.mealExplanations().values().stream()
                .anyMatch(v -> v == null || v.isBlank());
        if (hasBlankExplanation) {
            throw new AiProviderException("Resposta da IA incompleta: 'explicacaoRefeicoes' contém valores em branco.");
        }
        if (response.tips() == null) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'dicas' ausente.");
        }
        if (response.alerts() == null) {
            throw new AiProviderException("Resposta da IA incompleta: campo 'alertas' ausente.");
        }
    }
}