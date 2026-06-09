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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private MealPlanPreparation samplePreparation() {
        return new MealPlanPreparation(samplePlan(), sampleContext());
    }

    @Nested
    class Preparacao {

        @Test
        void deveExecutarGeneratePlanEBuildContextExatamenteUmaVezPorPreparacao() {
            when(mealPlanEngine.generatePlan("user@test.com")).thenReturn(samplePlan());
            when(mealPlanEngine.buildContext("user@test.com")).thenReturn(sampleContext());

            MealPlanPreparation preparation = mealPlanAiService.prepare("user@test.com");

            assertThat(preparation.plan()).isEqualTo(samplePlan());
            assertThat(preparation.context()).isEqualTo(sampleContext());
            verify(mealPlanEngine, times(1)).generatePlan("user@test.com");
            verify(mealPlanEngine, times(1)).buildContext("user@test.com");
        }
    }

    @Nested
    class FluxoComIa {

        @Test
        void deveRetornarRespostaCompletaQuandoIaRespondeComSucesso() {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send("system", "user"))
                    .thenReturn(new AiProviderResult(VALID_AI_RESPONSE, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().status()).isEqualTo("COMPLETO");
            assertThat(result.response().plan()).isEqualTo(samplePlan());
            assertThat(result.response().summary()).isEqualTo("Plano equilibrado para o perfil do usuário.");
            assertThat(result.response().mealExplanations()).hasSize(4);
            assertThat(result.response().tips()).containsExactly("Beba água");
            assertThat(result.response().alerts()).containsExactly("Consulte um nutricionista");
            assertThat(result.response().provider()).isEqualTo("gemini");
            assertThat(result.response().model()).isEqualTo("gemini-2.0-flash");
        }

        @Test
        void enrichNaoDeveAcessarEngineDeBancoDeDados() {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send("system", "user"))
                    .thenReturn(new AiProviderResult(VALID_AI_RESPONSE, "gemini", "gemini-2.0-flash"));

            mealPlanAiService.enrich(samplePreparation());

            // Regression guard: the AI phase must run without touching transactional collaborators.
            verifyNoInteractions(mealPlanEngine);
        }
    }

    @Nested
    class Fallback {

        @Test
        void deveRetornarPlanoSemIaQuandoProviderFalha() {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenThrow(new AiProviderException("Timeout na API"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().plan()).isEqualTo(samplePlan());
            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().model()).isEqualTo("none");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
            assertThat(result.response().summary()).contains("indisponível");
        }

        @Test
        void deveRetornarPlanoSemIaQuandoRespostaInvalida() {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult("resposta inválida", "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().plan()).isEqualTo(samplePlan());
            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
        }

        @Test
        void deveRetornarFallbackQuandoProviderRetornaConteudoNulo() {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(null, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
        }

        @Test
        void deveRetornarFallbackQuandoProviderRetornaConteudoVazio() {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult("   ", "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
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
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(semResumo, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
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
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(semExplicacoes, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
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
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withEmptyValue, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
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
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withBlankValue, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
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
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withMixedValues, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().provider()).isEqualTo("fallback");
            assertThat(result.response().status()).isEqualTo("PARCIAL");
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
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(samplePlan(), sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenReturn(new AiProviderResult(withAllValidValues, "gemini", "gemini-2.0-flash"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(samplePreparation());

            assertThat(result.response().status()).isEqualTo("COMPLETO");
            assertThat(result.response().provider()).isEqualTo("gemini");
            assertThat(result.response().mealExplanations()).hasSize(2);
        }
    }

    /**
     * Sprint 2.4 regression: the engine is the source of truth for composition.
     * The AI enriches text only — it must never replace foods, change portions/macros,
     * add or remove meals. Since {@code MealPlanAiResponse} carries no composition fields,
     * the {@code plan} returned must always be the exact engine plan, both on success and
     * on fallback, for 3, 4 and 5 meals.
     */
    @Nested
    class RegressaoComposicaoDoEngine {

        @Test
        void iaNaoDeveAlterarComposicaoDoAlmocoNoFluxoComSucesso() {
            DailyMealPlan enginePlan = planWithMeals(3);
            MealPlanPreparation preparation = new MealPlanPreparation(enginePlan, sampleContext());
            stubAiSuccess(enginePlan, sampleContext());

            MealPlanGenerationResult result = mealPlanAiService.enrich(preparation);

            // The returned plan must be the very same engine instance: untouched composition.
            assertThat(result.response().plan()).isSameAs(enginePlan);
            assertThat(result.response().plan()).isEqualTo(enginePlan);
            assertThat(lunchOf(result.response().plan()).options().get(0).items())
                    .extracting(MealPlanFoodItem::category)
                    .containsExactly("PROTEIN", "CARB", "VEGETABLE");
        }

        @Test
        void fallbackDevePreservarComposicaoEstruturalDoEngine() {
            DailyMealPlan enginePlan = planWithMeals(3);
            MealPlanPreparation preparation = new MealPlanPreparation(enginePlan, sampleContext());
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(enginePlan, sampleContext())).thenReturn("user");
            when(aiProviderClient.send(anyString(), anyString()))
                    .thenThrow(new AiProviderException("Timeout na API"));

            MealPlanGenerationResult result = mealPlanAiService.enrich(preparation);

            assertThat(result.response().status()).isEqualTo("PARCIAL");
            assertThat(result.response().plan()).isSameAs(enginePlan);
            assertThat(lunchOf(result.response().plan()).options().get(0).items())
                    .extracting(MealPlanFoodItem::category)
                    .containsExactly("PROTEIN", "CARB", "VEGETABLE");
        }

        @Test
        void devePreservarPlanoComTresQuatroOuCincoRefeicoes() {
            for (int mealCount : new int[]{3, 4, 5}) {
                DailyMealPlan enginePlan = planWithMeals(mealCount);
                MealPlanPreparation preparation = new MealPlanPreparation(enginePlan, sampleContext());
                stubAiSuccess(enginePlan, sampleContext());

                MealPlanGenerationResult result = mealPlanAiService.enrich(preparation);

                assertThat(result.response().plan().meals())
                        .as("meal count must be preserved for %d meals", mealCount)
                        .hasSize(mealCount);
                assertThat(result.response().plan()).isSameAs(enginePlan);
                assertThat(lunchOf(result.response().plan()).options().get(0).items())
                        .as("lunch composition must be preserved for %d meals", mealCount)
                        .extracting(MealPlanFoodItem::category)
                        .containsExactly("PROTEIN", "CARB", "VEGETABLE");
            }
        }

        @Test
        void planoDevePermanecerCompativelComPlanJsonAposEnriquecimento() throws Exception {
            DailyMealPlan enginePlan = planWithMeals(4);
            MealPlanPreparation preparation = new MealPlanPreparation(enginePlan, sampleContext());
            stubAiSuccess(enginePlan, sampleContext());

            MealPlanGenerationResult result = mealPlanAiService.enrich(preparation);

            // plan_json round-trip: serializing then deserializing the plan yields an
            // equal object, proving the persisted contract shape is unchanged.
            String planJson = objectMapper.writeValueAsString(result.response().plan());
            DailyMealPlan roundTripped = objectMapper.readValue(planJson, DailyMealPlan.class);
            assertThat(roundTripped).isEqualTo(enginePlan);
            assertThat(planJson)
                    .contains("\"mealType\"")
                    .contains("\"portionGrams\"")
                    .contains("\"foodId\"");
        }

        private void stubAiSuccess(DailyMealPlan plan, MealPlanContext context) {
            when(promptBuilder.buildSystemPrompt()).thenReturn("system");
            when(promptBuilder.buildUserPrompt(plan, context)).thenReturn("user");
            when(aiProviderClient.send("system", "user"))
                    .thenReturn(new AiProviderResult(aiResponseFor(plan), "gemini", "gemini-2.0-flash"));
        }

        private MealPlanMeal lunchOf(DailyMealPlan plan) {
            return plan.meals().stream()
                    .filter(m -> "LUNCH".equals(m.mealType()))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static final List<String> MEAL_ORDER =
            List.of("BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER");

    private DailyMealPlan planWithMeals(int mealCount) {
        List<MealPlanMeal> meals = MEAL_ORDER.subList(0, mealCount).stream()
                .map(this::mealFor)
                .toList();
        return new DailyMealPlan(2000, "MANTER_PESO", meals);
    }

    private MealPlanMeal mealFor(String mealType) {
        boolean isMainMeal = "LUNCH".equals(mealType) || "DINNER".equals(mealType);
        List<MealPlanFoodItem> items = isMainMeal
                ? List.of(foodItem(1, "Chicken breast grilled", "PROTEIN"),
                          foodItem(2, "White rice cooked", "CARB"),
                          foodItem(3, "Broccoli cooked", "VEGETABLE"))
                : List.of(foodItem(4, "Banana", "FRUIT"));
        MealOption option = new MealOption(1, items, 500, 40, 50, 10);
        return new MealPlanMeal(mealType, 500, List.of(option));
    }

    private MealPlanFoodItem foodItem(int id, String name, String category) {
        return new MealPlanFoodItem(id, name, name, category,
                BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                BigDecimal.valueOf(20), BigDecimal.valueOf(15),
                BigDecimal.valueOf(3), BigDecimal.valueOf(2));
    }

    private String aiResponseFor(DailyMealPlan plan) {
        String explanations = plan.meals().stream()
                .map(meal -> "\"" + meal.mealType() + "\": \"Explicação da refeição.\"")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "summary": "Plano equilibrado para o perfil do usuário.",
                  "mealExplanations": {%s},
                  "tips": ["Beba água"],
                  "alerts": []
                }
                """.formatted(explanations);
    }
}
