package com.tukan.api.service.mealplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.*;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.service.ai.AiProviderClient;
import com.tukan.api.service.ai.AiProviderResult;
import com.tukan.api.service.ai.MealPlanPromptBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanAiServiceTest {

    @Mock
    private MealPlanEngine mealPlanEngine;

    @Mock
    private MealPlanPromptBuilder promptBuilder;

    @Mock
    private AiProviderClient aiProviderClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MealPlanAiService mealPlanAiService;

    private static final String VALID_AI_RESPONSE = """
            {
              "summary": "Plano equilibrado para o perfil do usuário.",
              "mealExplanations": {
                "BREAKFAST": "Café da manhã energético.",
                "LUNCH": "Almoço rico em proteínas.",
                "AFTERNOON_SNACK": "Lanche leve.",
                "DINNER": "Jantar completo."
              },
              "tips": ["Beba água"],
              "alerts": ["Consulte um nutricionista"]
            }
            """;

    private DailyMealPlan samplePlan() {
        return new DailyMealPlan(2000, "MANTER_PESO", Collections.emptyList());
    }

    private MealPlanContext sampleContext() {
        return new MealPlanContext(null, null, 2000, Map.of(), Map.of());
    }

    @Nested
    class FluxoComIa {

        @Test
        void deveRetornarRespostaCompletaQuandoIaRespondeComSucesso() {
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send("system", "user"))
                    .thenReturn(new AiProviderResult(VALID_AI_RESPONSE, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.status()).isEqualTo("COMPLETO");
            assertThat(response.plan()).isEqualTo(samplePlan());
            assertThat(response.summary()).isEqualTo("Plano equilibrado para o perfil do usuário.");
            assertThat(response.mealExplanations()).hasSize(4);
            assertThat(response.tips()).containsExactly("Beba água");
            assertThat(response.alerts()).containsExactly("Consulte um nutricionista");
            assertThat(response.provider()).isEqualTo("gemini");
            assertThat(response.model()).isEqualTo("gemini-2.0-flash");
        }
    }

    @Nested
    class Fallback {

        @Test
        void deveRetornarPlanoSemIaQuandoProviderFalha() {
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenThrow(new AiProviderException("Timeout na API"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.plan()).isEqualTo(samplePlan());
            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.model()).isEqualTo("none");
            assertThat(response.status()).isEqualTo("PARCIAL");
            assertThat(response.summary()).contains("indisponível");
        }

        @Test
        void deveRetornarPlanoSemIaQuandoRespostaInvalida() {
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult("resposta inválida", "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.plan()).isEqualTo(samplePlan());
            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void deveRetornarFallbackQuandoProviderRetornaConteudoNulo() {
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(null, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void deveRetornarFallbackQuandoProviderRetornaConteudoVazio() {
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult("   ", "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }
    }

    @Nested
    class ValidacaoResposta {

        @Test
        void deveAcionarFallbackQuandoResumoAusente() {
            String semResumo = """
                    {
                      "summary": "",
                      "mealExplanations": {"BREAKFAST": "ok"},
                      "tips": [],
                      "alerts": []
                    }
                    """;
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(semResumo, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void deveAcionarFallbackQuandoMealExplanationsVazio() {
            String semExplicacoes = """
                    {
                      "summary": "Plano válido.",
                      "mealExplanations": {},
                      "tips": [],
                      "alerts": []
                    }
                    """;
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(semExplicacoes, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void shouldTriggerFallbackWhenMealExplanationsHasEmptyStringValue() {
            String withEmptyValue = """
                    {
                      "summary": "Plano válido.",
                      "mealExplanations": {"BREAKFAST": ""},
                      "tips": [],
                      "alerts": []
                    }
                    """;
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withEmptyValue, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void shouldTriggerFallbackWhenMealExplanationsHasBlankStringValue() {
            String withBlankValue = """
                    {
                      "summary": "Plano válido.",
                      "mealExplanations": {"BREAKFAST": "   "},
                      "tips": [],
                      "alerts": []
                    }
                    """;
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withBlankValue, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void shouldTriggerFallbackWhenMealExplanationsHasMixOfValidAndBlankValues() {
            String withMixedValues = """
                    {
                      "summary": "Plano válido.",
                      "mealExplanations": {
                        "BREAKFAST": "Café da manhã nutritivo.",
                        "LUNCH": ""
                      },
                      "tips": [],
                      "alerts": []
                    }
                    """;
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withMixedValues, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.provider()).isEqualTo("fallback");
            assertThat(response.status()).isEqualTo("PARCIAL");
        }

        @Test
        void shouldAcceptMealExplanationsWhenAllValuesAreValid() {
            String withAllValidValues = """
                    {
                      "summary": "Plano válido.",
                      "mealExplanations": {
                        "BREAKFAST": "Café da manhã nutritivo.",
                        "LUNCH": "Almoço equilibrado."
                      },
                      "tips": [],
                      "alerts": []
                    }
                    """;
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withAllValidValues, "gemini", "gemini-2.0-flash"));

            MealPlanRecommendationResponse response = mealPlanAiService.generate("user@test.com");

            assertThat(response.status()).isEqualTo("COMPLETO");
            assertThat(response.provider()).isEqualTo("gemini");
            assertThat(response.mealExplanations()).hasSize(2);
        }
    }
}
