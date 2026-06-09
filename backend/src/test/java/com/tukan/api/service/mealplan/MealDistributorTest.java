package com.tukan.api.service.mealplan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MealDistributor")
class MealDistributorTest {

    private final MealDistributor distributor = new MealDistributor();

    @Nested
    @DisplayName("3 refeições")
    class TresRefeicoes {

        @Test
        @DisplayName("Distribui calorias entre BREAKFAST, LUNCH e DINNER")
        void distribuiTresRefeicoes() {
            Map<String, Double> result = distributor.distribute(2000.0, 3);

            assertThat(result).containsOnlyKeys("BREAKFAST", "LUNCH", "DINNER");
            assertThat(result.get("BREAKFAST")).isEqualTo(500.0);
            assertThat(result.get("LUNCH")).isEqualTo(800.0);
            assertThat(result.get("DINNER")).isEqualTo(700.0);
        }

        @Test
        @DisplayName("Não inclui MORNING_SNACK nem AFTERNOON_SNACK")
        void naoInclueSnacks() {
            Map<String, Double> result = distributor.distribute(2000.0, 3);
            assertThat(result).doesNotContainKey("MORNING_SNACK");
            assertThat(result).doesNotContainKey("AFTERNOON_SNACK");
        }
    }

    @Nested
    @DisplayName("4 refeições")
    class QuatroRefeicoes {

        @Test
        @DisplayName("Distribui calorias entre BREAKFAST, LUNCH, AFTERNOON_SNACK e DINNER")
        void distribuiQuatroRefeicoes() {
            Map<String, Double> result = distributor.distribute(2000.0, 4);

            assertThat(result).containsOnlyKeys("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
            assertThat(result.get("BREAKFAST")).isEqualTo(400.0);
            assertThat(result.get("LUNCH")).isEqualTo(700.0);
            assertThat(result.get("AFTERNOON_SNACK")).isEqualTo(300.0);
            assertThat(result.get("DINNER")).isEqualTo(600.0);
        }

        @Test
        @DisplayName("Não inclui MORNING_SNACK")
        void naoInclueMorningSnack() {
            Map<String, Double> result = distributor.distribute(2000.0, 4);
            assertThat(result).doesNotContainKey("MORNING_SNACK");
        }
    }

    @Nested
    @DisplayName("5 refeições")
    class CincoRefeicoes {

        @Test
        @DisplayName("Distribui calorias entre BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK e DINNER")
        void distribuiCincoRefeicoes() {
            Map<String, Double> result = distributor.distribute(2000.0, 5);

            assertThat(result).containsOnlyKeys(
                    "BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER");
            assertThat(result.get("BREAKFAST")).isEqualTo(400.0);
            assertThat(result.get("MORNING_SNACK")).isEqualTo(200.0);
            assertThat(result.get("LUNCH")).isEqualTo(700.0);
            assertThat(result.get("AFTERNOON_SNACK")).isEqualTo(200.0);
            assertThat(result.get("DINNER")).isEqualTo(500.0);
        }

        @Test
        @DisplayName("Soma das parcelas não ultrapassa a meta calórica diária")
        void somaNaoUltrapassaMeta() {
            double daily = 2000.0;
            Map<String, Double> result = distributor.distribute(daily, 5);
            double sum = result.values().stream().mapToDouble(Double::doubleValue).sum();
            assertThat(sum).isLessThanOrEqualTo(daily);
        }
    }

    @Nested
    @DisplayName("Valor inválido de refeições usa padrão de 4")
    class ValorInvalido {

        @Test
        @DisplayName("mealsPerDay=2 cai no padrão de 4 refeições")
        void mealsPerDayAbaixoDeLimite() {
            Map<String, Double> result = distributor.distribute(2000.0, 2);
            assertThat(result).containsOnlyKeys("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }

        @Test
        @DisplayName("mealsPerDay=6 cai no padrão de 4 refeições")
        void mealsPerDayAcimaDeLimite() {
            Map<String, Double> result = distributor.distribute(2000.0, 6);
            assertThat(result).containsOnlyKeys("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }
    }
}
