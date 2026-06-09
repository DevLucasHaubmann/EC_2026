package com.tukan.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealOption;
import com.tukan.api.dto.mealplan.MealPlanContext;
import com.tukan.api.dto.mealplan.MealPlanFoodItem;
import com.tukan.api.dto.mealplan.MealPlanMeal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the AI contract for Sprint 2.4: the prompt must instruct the model to PRESERVE
 * the engine-generated composition and limit itself to textual enrichment. The prompt is
 * the only soft boundary; these tests ensure it never silently loosens to allow the AI to
 * swap, invent, or recalculate foods.
 */
class MealPlanPromptBuilderTest {

    private MealPlanPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new MealPlanPromptBuilder(new ObjectMapper());
    }

    @Nested
    class SystemPrompt {

        @Test
        void deveProibirAlteracaoEstruturalDaComposicao() {
            // when
            String prompt = promptBuilder.buildSystemPrompt();

            // then — the AI must not change the foods chosen by the engine
            assertThat(prompt)
                    .contains("NÃO substitua, remova ou invente alimentos")
                    .contains("NÃO altere gramas, calorias, proteínas, carboidratos ou gorduras")
                    .contains("NÃO crie refeições ou opções além das recebidas")
                    .contains("NÃO recalcule nenhum valor nutricional");
        }

        @Test
        void deveLimitarPapelDaIaAEnriquecimentoTextual() {
            // when
            String prompt = promptBuilder.buildSystemPrompt();

            // then — role is restricted to summary, explanations, tips and alerts
            assertThat(prompt)
                    .contains("apresentação textual do plano alimentar")
                    .contains("resumo")
                    .contains("Explicar brevemente, por refeição")
                    .contains("dicas")
                    .contains("Alertar sobre restrições");
        }

        @Test
        void deveOrdenarPreservacaoDaQuantidadeEOrdemDasRefeicoes() {
            // when
            String prompt = promptBuilder.buildSystemPrompt();

            // then — 3/4/5 meals must be respected exactly, no invented meal types
            assertThat(prompt)
                    .contains("Respeite exatamente as refeições e a ordem recebidas no plano")
                    .contains("Não crie mealTypes inexistentes")
                    .contains("3, 4 ou 5 refeições");
        }

        @Test
        void deveExigirRespostaJsonSemCamposDeComposicao() {
            // when
            String prompt = promptBuilder.buildSystemPrompt();

            // then — the response schema carries only text fields; it has no slot to
            // return foods, portions or macros, so the AI cannot rewrite composition.
            assertThat(prompt)
                    .contains("\"summary\"")
                    .contains("\"mealExplanations\"")
                    .contains("\"tips\"")
                    .contains("\"alerts\"")
                    .doesNotContain("\"items\"")
                    .doesNotContain("\"portionGrams\"");
        }
    }

    @Nested
    class UserPrompt {

        @Test
        void deveEnviarPlanoCompletoDoEngineComItensEMacros() {
            // given
            DailyMealPlan plan = structuredPlan();
            MealPlanContext context = emptyContext();

            // when
            String userPrompt = promptBuilder.buildUserPrompt(plan, context);

            // then — the engine plan is serialized in full so the AI receives the
            // source-of-truth composition it must describe (not regenerate).
            assertThat(userPrompt)
                    .contains("Plano alimentar gerado pelo sistema")
                    .contains("LUNCH")
                    .contains("Chicken breast grilled")
                    .contains("White rice cooked")
                    .contains("portionGrams")
                    .contains("Apresente esse plano de forma humanizada");
        }

        @Test
        void deveIncluirContextoNutricionalDoUsuario() {
            // given
            DailyMealPlan plan = structuredPlan();
            MealPlanContext context = emptyContext();

            // when
            String userPrompt = promptBuilder.buildUserPrompt(plan, context);

            // then
            assertThat(userPrompt).contains("Contexto nutricional do usuário");
        }
    }

    private DailyMealPlan structuredPlan() {
        MealPlanFoodItem protein = item(1, "Chicken breast grilled", "PROTEIN");
        MealPlanFoodItem carb = item(2, "White rice cooked", "CARB");
        MealPlanFoodItem veggie = item(3, "Broccoli cooked", "VEGETABLE");
        MealOption option = new MealOption(1, List.of(protein, carb, veggie),
                500, 40, 50, 10);
        MealPlanMeal lunch = new MealPlanMeal("LUNCH", 500, List.of(option));
        return new DailyMealPlan(2000, "MANTER_PESO", List.of(lunch));
    }

    private MealPlanFoodItem item(int id, String name, String category) {
        return new MealPlanFoodItem(id, name, name, category,
                BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                BigDecimal.valueOf(20), BigDecimal.valueOf(15),
                BigDecimal.valueOf(3), BigDecimal.valueOf(2));
    }

    private MealPlanContext emptyContext() {
        return new MealPlanContext(null, null, 2000, Map.of(), Map.of());
    }
}
