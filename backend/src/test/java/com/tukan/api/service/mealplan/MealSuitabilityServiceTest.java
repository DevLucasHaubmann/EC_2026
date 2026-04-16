package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Food;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MealSuitabilityServiceTest {

    private MealSuitabilityService suitabilityService;

    @BeforeEach
    void setUp() {
        suitabilityService = new MealSuitabilityService();
    }

    private Food createFood(int id, String name, String category) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCategory(category);
        food.setCaloriesPer100g(BigDecimal.valueOf(100));
        food.setReferencePortionGrams(BigDecimal.valueOf(100));
        food.setActive(true);
        return food;
    }

    @Nested
    @DisplayName("Café da manhã")
    class CafeDaManha {

        @Test
        @DisplayName("Remove itens absurdos do café da manhã")
        void removeItensAbsurdos() {
            List<Food> foods = new ArrayList<>(List.of(
                    createFood(1, "Ostra frita", "PROTEIN"),
                    createFood(2, "Pão Integral", "CARBOHYDRATE"),
                    createFood(3, "Feijoada completa", "PROTEIN"),
                    createFood(4, "Iogurte Natural", "DAIRY"),
                    createFood(5, "Steak grelhado", "PROTEIN")
            ));

            List<Food> result = suitabilityService.filterAndPrioritize(foods, "BREAKFAST");

            List<String> names = result.stream().map(Food::getName).toList();
            assertThat(names).contains("Pão Integral", "Iogurte Natural");
            assertThat(names).doesNotContain("Ostra frita", "Feijoada completa", "Steak grelhado");
        }

        @Test
        @DisplayName("Prioriza itens adequados para café da manhã")
        void priorizaItensAdequados() {
            List<Food> foods = new ArrayList<>(List.of(
                    createFood(1, "Tofu firme", "PROTEIN"),
                    createFood(2, "Pão com aveia integral", "CARBOHYDRATE"),
                    createFood(3, "Batata cozida", "CARBOHYDRATE")
            ));

            List<Food> result = suitabilityService.filterAndPrioritize(foods, "BREAKFAST");

            // Pão com aveia deve vir antes (tem "pao" e "aveia" como keywords)
            assertThat(result.get(0).getName()).isEqualTo("Pão com aveia integral");
        }
    }

    @Nested
    @DisplayName("Lanche da tarde")
    class LancheDaTarde {

        @Test
        @DisplayName("Remove itens pesados do lanche")
        void removeItensPesados() {
            List<Food> foods = new ArrayList<>(List.of(
                    createFood(1, "Pizza margherita", "CARBOHYDRATE"),
                    createFood(2, "Banana", "FRUIT"),
                    createFood(3, "Lasanha bolonhesa", "CARBOHYDRATE"),
                    createFood(4, "Castanha de caju", "HEALTHY_FAT")
            ));

            List<Food> result = suitabilityService.filterAndPrioritize(foods, "AFTERNOON_SNACK");

            List<String> names = result.stream().map(Food::getName).toList();
            assertThat(names).contains("Banana", "Castanha de caju");
            assertThat(names).doesNotContain("Pizza margherita", "Lasanha bolonhesa");
        }
    }

    @Nested
    @DisplayName("Almoço e janta")
    class AlmocoJanta {

        @Test
        @DisplayName("Almoço aceita pratos completos")
        void almocoAceitaPratosCompletos() {
            List<Food> foods = new ArrayList<>(List.of(
                    createFood(1, "Arroz integral", "CARBOHYDRATE"),
                    createFood(2, "Feijão preto", "LEGUME"),
                    createFood(3, "Frango grelhado", "PROTEIN"),
                    createFood(4, "Brócolis cozido", "VEGETABLE")
            ));

            List<Food> result = suitabilityService.filterAndPrioritize(foods, "LUNCH");

            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("Janta prioriza itens adequados")
        void jantaPriorizaAdequados() {
            List<Food> foods = new ArrayList<>(List.of(
                    createFood(1, "Cereal matinal açucarado", "CARBOHYDRATE"),
                    createFood(2, "Sopa de legumes", "VEGETABLE"),
                    createFood(3, "Arroz com feijão", "CARBOHYDRATE")
            ));

            List<Food> result = suitabilityService.filterAndPrioritize(foods, "DINNER");

            List<String> names = result.stream().map(Food::getName).toList();
            assertThat(names).doesNotContain("Cereal matinal açucarado");
            // Sopa deve ser priorizada
            assertThat(result.get(0).getName()).isEqualTo("Sopa de legumes");
        }
    }
}
