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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodSelectorTest {

    private FoodSelector foodSelector;

    @BeforeEach
    void setUp() {
        foodSelector = new FoodSelector();
    }

    private Food createFood(int id, String name, String category, double cal100g, double portionG) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCategory(category);
        food.setCaloriesPer100g(BigDecimal.valueOf(cal100g));
        food.setProteinPer100g(BigDecimal.valueOf(10));
        food.setCarbsPer100g(BigDecimal.valueOf(20));
        food.setFatPer100g(BigDecimal.valueOf(5));
        food.setFiberPer100g(BigDecimal.valueOf(2));
        food.setReferencePortionGrams(BigDecimal.valueOf(portionG));
        food.setPrimaryMealType("LUNCH");
        food.setActive(true);
        return food;
    }

    @Nested
    @DisplayName("Estrutura mínima garantida")
    class EstruturaMinima {

        @Test
        @DisplayName("Lista vazia lança exceção — validação deve ocorrer antes")
        void listaVaziaLancaExcecao() {
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(List.of(), 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Sempre retorna exatamente 2 opções com alimentos disponíveis")
        void retornaDuasOpcoesComAlimentos() {
            List<Food> foods = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                foods.add(createFood(i, "Alimento " + i, "PROTEIN", 150, 100));
            }

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 500);

            assertThat(options).hasSize(2);
            assertThat(options.get(0).optionNumber()).isEqualTo(1);
            assertThat(options.get(1).optionNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("Retorna 2 opções mesmo com poucos alimentos")
        void retornaDuasOpcoesComPoucosAlimentos() {
            List<Food> foods = List.of(
                    createFood(1, "Frango", "PROTEIN", 200, 150),
                    createFood(2, "Arroz", "CARBOHYDRATE", 130, 150)
            );

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 500);

            assertThat(options).hasSize(2);
        }

        @Test
        @DisplayName("Retorna 2 opções com apenas 1 alimento — fallback reusa na opção 2")
        void retornaDuasOpcoesComUmAlimento() {
            List<Food> foods = List.of(createFood(1, "Frango", "PROTEIN", 200, 150));

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 500);

            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).hasSize(1);
            // Fallback: opção 2 reusa o alimento da opção 1
            assertThat(options.get(1).items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Fallback com reaproveitamento")
    class FallbackReaproveitamento {

        @Test
        @DisplayName("Pool muito pequeno: opção 2 nunca fica vazia")
        void poolMuitoPequenoNuncaVazio() {
            List<Food> foods = List.of(
                    createFood(1, "Frango", "PROTEIN", 150, 100),
                    createFood(2, "Arroz", "CARBOHYDRATE", 130, 150)
            );

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 400);

            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("Pool extremamente restrito (1 alimento): ambas opções preenchidas")
        void poolExtremoUmAlimento() {
            List<Food> foods = List.of(createFood(1, "Tofu", "PROTEIN", 120, 100));

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 300);

            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).hasSize(1);
            assertThat(options.get(1).items()).hasSize(1);
            // Mesmo alimento reusado
            assertThat(options.get(0).items().get(0).foodId())
                    .isEqualTo(options.get(1).items().get(0).foodId());
        }

        @Test
        @DisplayName("Opção 2 sem repetição quando pool é suficiente")
        void semRepeticaoQuandoPoolSuficiente() {
            List<Food> foods = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                foods.add(createFood(i, "Alimento " + i, "PROTEIN", 100, 100));
            }

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 300);

            List<Integer> idsOp1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            List<Integer> idsOp2 = options.get(1).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();

            // Com pool grande, não deve haver repetição
            for (Integer id : idsOp1) {
                assertThat(idsOp2).doesNotContain(id);
            }
        }

        @Test
        @DisplayName("Lista vazia lança exceção")
        void listaVaziaLancaExcecao() {
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(List.of(), 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Sem repetição entre opções")
    class SemRepeticao {

        @Test
        @DisplayName("Alimentos não se repetem entre opção 1 e opção 2")
        void alimentosNaoRepetem() {
            List<Food> foods = new ArrayList<>();
            for (int i = 1; i <= 30; i++) {
                String cat = i % 3 == 0 ? "PROTEIN" : i % 3 == 1 ? "CARBOHYDRATE" : "VEGETABLE";
                foods.add(createFood(i, "Alimento " + i, cat, 100, 100));
            }

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 400);

            List<Integer> idsOp1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            List<Integer> idsOp2 = options.get(1).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();

            for (Integer id : idsOp1) {
                assertThat(idsOp2).doesNotContain(id);
            }
        }
    }

    @Nested
    @DisplayName("Respeito à meta calórica")
    class MetaCalorica {

        @Test
        @DisplayName("Total de calorias da opção não excede meta + 10%")
        void naoExcedeMeta() {
            List<Food> foods = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                foods.add(createFood(i, "Alimento " + i, "PROTEIN", 150, 100));
            }

            double target = 500;
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, target);

            for (FoodSelector.MealOptionBuild option : options) {
                if (!option.items().isEmpty()) {
                    assertThat(option.totalCalories()).isLessThanOrEqualTo(target * 1.10 + 1);
                }
            }
        }

        @Test
        @DisplayName("Máximo de 5 itens por opção")
        void maximoCincoItens() {
            List<Food> foods = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                foods.add(createFood(i, "Alimento " + i, "PROTEIN", 20, 50));
            }

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 2000);

            for (FoodSelector.MealOptionBuild option : options) {
                assertThat(option.items()).hasSizeLessThanOrEqualTo(5);
            }
        }
    }

    @Nested
    @DisplayName("Variedade entre categorias")
    class VariedadeCategorias {

        @Test
        @DisplayName("Seleciona alimentos de categorias diferentes quando disponíveis")
        void selecionaCategoriasDiferentes() {
            List<Food> foods = List.of(
                    createFood(1, "Frango", "PROTEIN", 150, 100),
                    createFood(2, "Arroz", "CARBOHYDRATE", 130, 100),
                    createFood(3, "Brócolis", "VEGETABLE", 35, 100),
                    createFood(4, "Banana", "FRUIT", 89, 100),
                    createFood(5, "Feijão", "LEGUME", 77, 100),
                    createFood(6, "Queijo", "DAIRY", 300, 30),
                    createFood(7, "Azeite", "HEALTHY_FAT", 884, 10),
                    // Duplicatas para opção 2
                    createFood(8, "Carne", "PROTEIN", 250, 100),
                    createFood(9, "Batata", "CARBOHYDRATE", 77, 100),
                    createFood(10, "Espinafre", "VEGETABLE", 23, 100),
                    createFood(11, "Maçã", "FRUIT", 52, 100),
                    createFood(12, "Lentilha", "LEGUME", 116, 100),
                    createFood(13, "Iogurte", "DAIRY", 59, 100),
                    createFood(14, "Castanha", "HEALTHY_FAT", 656, 10)
            );

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, 600);

            // Cada opção deve ter alimentos de mais de uma categoria
            for (FoodSelector.MealOptionBuild option : options) {
                if (option.items().size() > 1) {
                    long distinctCategories = option.items().stream()
                            .map(FoodSelector.FoodPortionBuild::category)
                            .distinct()
                            .count();
                    assertThat(distinctCategories).isGreaterThan(1);
                }
            }
        }
    }
}
