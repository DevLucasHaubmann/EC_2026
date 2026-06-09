package com.tukan.api.service.mealplan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tukan.api.service.mealplan.MealCalorieFloorPolicy.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MealCalorieFloorPolicy}.
 *
 * Tests cover the progressive acceptance ladder (70 % → 55 % → 40 %) and the
 * hard failure case below 40 %. No Spring context — pure unit tests.
 */
@DisplayName("MealCalorieFloorPolicy — escada progressiva de pisos calóricos")
class MealCalorieFloorPolicyTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FoodSelector.MealOptionBuild option(int number, double calories) {
        return new FoodSelector.MealOptionBuild(number, List.of(), calories, 0.0, 0.0, 0.0);
    }

    private List<FoodSelector.MealOptionBuild> twoOptions(double calories) {
        return List.of(option(1, calories), option(2, calories));
    }

    // -------------------------------------------------------------------------
    // 1. Constantes nomeadas
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constantes: valores e relação entre tiers")
    class Constants {

        @Test
        @DisplayName("TIER_STANDARD é 0.70")
        void tierStandardIs70Percent() {
            assertThat(TIER_STANDARD).isEqualTo(0.70);
        }

        @Test
        @DisplayName("TIER_RELAXED é 0.55")
        void tierRelaxedIs55Percent() {
            assertThat(TIER_RELAXED).isEqualTo(0.55);
        }

        @Test
        @DisplayName("TIER_MINIMUM é 0.40")
        void tierMinimumIs40Percent() {
            assertThat(TIER_MINIMUM).isEqualTo(0.40);
        }

        @Test
        @DisplayName("TIER_MINIMUM < TIER_RELAXED < TIER_STANDARD (ordem correta da escada)")
        void tierOrderIsDescending() {
            assertThat(TIER_MINIMUM).isLessThan(TIER_RELAXED);
            assertThat(TIER_RELAXED).isLessThan(TIER_STANDARD);
        }
    }

    // -------------------------------------------------------------------------
    // 2. Tier 1 — standard (70%)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Tier 1 — standard (70%): opções aceitas quando atingem 70% do alvo")
    class Tier1Standard {

        @Test
        @DisplayName("Opções exatamente em 70% do alvo → aceitas via tier 1")
        void atExactly70Percent_accepted() {
            // given
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(700.0); // 70%

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then
            assertThat(result.accepted()).hasSize(2);
            assertThat(result.tierRatio()).isEqualTo(TIER_STANDARD);
        }

        @Test
        @DisplayName("Opções acima de 70% → aceitas via tier 1")
        void above70Percent_accepted() {
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(850.0); // 85%

            AcceptanceResult result = acceptOptions(target, options);

            assertThat(result.accepted()).hasSize(2);
            assertThat(result.tierRatio()).isEqualTo(TIER_STANDARD);
        }

        @Test
        @DisplayName("Uma opção acima de 70% e outra abaixo: apenas a que atinge é aceita no tier 1")
        void mixedOptions_onlyAbove70Accepted() {
            // given
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = List.of(
                    option(1, 750.0), // 75% — passes tier 1
                    option(2, 400.0)  // 40% — below tier 1
            );

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then
            assertThat(result.accepted()).hasSize(1);
            assertThat(result.accepted().get(0).totalCalories()).isEqualTo(750.0);
            assertThat(result.tierRatio()).isEqualTo(TIER_STANDARD);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Tier 2 — relaxed (55%)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Tier 2 — relaxed (55%): fallback quando nenhuma opção atinge 70%")
    class Tier2Relaxed {

        @Test
        @DisplayName("Nenhuma opção atinge 70%, mas atinge 55% → aceitas via tier 2")
        void below70ButAbove55_acceptedViaTier2() {
            // given
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(600.0); // 60% — abaixo de 70%, acima de 55%

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then
            assertThat(result.accepted()).hasSize(2);
            assertThat(result.tierRatio()).isEqualTo(TIER_RELAXED);
        }

        @Test
        @DisplayName("Opções exatamente em 55% → aceitas via tier 2")
        void atExactly55Percent_acceptedViaTier2() {
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(550.0); // exatamente 55%

            AcceptanceResult result = acceptOptions(target, options);

            assertThat(result.accepted()).hasSize(2);
            assertThat(result.tierRatio()).isEqualTo(TIER_RELAXED);
        }
    }

    // -------------------------------------------------------------------------
    // 4. Tier 3 — minimum (40%)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Tier 3 — minimum (40%): último fallback antes da falha")
    class Tier3Minimum {

        @Test
        @DisplayName("Nenhuma opção atinge 55%, mas atinge 40% → aceitas via tier 3")
        void below55ButAbove40_acceptedViaTier3() {
            // given
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(500.0); // 50% — abaixo de 55%, acima de 40%

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then
            assertThat(result.accepted()).hasSize(2);
            assertThat(result.tierRatio()).isEqualTo(TIER_MINIMUM);
        }

        @Test
        @DisplayName("Opções exatamente em 40% → aceitas via tier 3")
        void atExactly40Percent_acceptedViaTier3() {
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(400.0); // exatamente 40%

            AcceptanceResult result = acceptOptions(target, options);

            assertThat(result.accepted()).hasSize(2);
            assertThat(result.tierRatio()).isEqualTo(TIER_MINIMUM);
        }
    }

    // -------------------------------------------------------------------------
    // 5. Falha (abaixo de 40%)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Falha: abaixo de 40% → accepted vazio (caller deve lançar BusinessException)")
    class HardFailure {

        @Test
        @DisplayName("Opções abaixo de 40% → accepted vazio")
        void below40Percent_returnsEmpty() {
            // given
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(350.0); // 35%

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then
            assertThat(result.accepted()).isEmpty();
        }

        @Test
        @DisplayName("Opções em 0 kcal → accepted vazio")
        void zeroCalories_returnsEmpty() {
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = twoOptions(0.0);

            AcceptanceResult result = acceptOptions(target, options);

            assertThat(result.accepted()).isEmpty();
        }

        @Test
        @DisplayName("Lista de opções vazia → accepted vazio")
        void emptyOptions_returnsEmpty() {
            AcceptanceResult result = acceptOptions(1000.0, List.of());

            assertThat(result.accepted()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // 6. Lógica de seleção do tier mais alto viável
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Seleção do tier mais alto viável — semântica da escada")
    class TierSelection {

        @Test
        @DisplayName("Tier 1 tem precedência: se qualquer opção atinge 70%, o tier 2 e 3 não são usados")
        void tier1TakesPrecedence_whenAnyOptionReachesTier1() {
            // given — opção 1 em 75% (tier 1), opção 2 em 45% (tier 3)
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = List.of(
                    option(1, 750.0),
                    option(2, 450.0)
            );

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then — tier 1 é usado; apenas a opção de 750 é aceita
            assertThat(result.tierRatio()).isEqualTo(TIER_STANDARD);
            assertThat(result.accepted()).hasSize(1);
            assertThat(result.accepted().get(0).totalCalories()).isEqualTo(750.0);
        }

        @Test
        @DisplayName("Tier 2 tem precedência sobre tier 3: se qualquer opção atinge 55%, tier 3 não é usado")
        void tier2TakesPrecedenceOverTier3() {
            // given — opção 1 em 60% (tier 2), opção 2 em 45% (entre tier 2 e 3)
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = List.of(
                    option(1, 600.0), // tier 2
                    option(2, 450.0)  // apenas tier 3
            );

            // when
            AcceptanceResult result = acceptOptions(target, options);

            // then — tier 2; apenas a opção de 600 é aceita
            assertThat(result.tierRatio()).isEqualTo(TIER_RELAXED);
            assertThat(result.accepted()).hasSize(1);
            assertThat(result.accepted().get(0).totalCalories()).isEqualTo(600.0);
        }

        @Test
        @DisplayName("Quando todas as opções atingem tier 1, todas são aceitas")
        void allOptionsMeetTier1_allAccepted() {
            double target = 1000.0;
            List<FoodSelector.MealOptionBuild> options = List.of(
                    option(1, 800.0),
                    option(2, 750.0)
            );

            AcceptanceResult result = acceptOptions(target, options);

            assertThat(result.tierRatio()).isEqualTo(TIER_STANDARD);
            assertThat(result.accepted()).hasSize(2);
        }
    }
}
