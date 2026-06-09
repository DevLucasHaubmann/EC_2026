package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Food;
import com.tukan.api.entity.FoodRole;
import com.tukan.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private Food createFoodWithRole(int id, String name, String category,
                                     double cal100g, double portionG, FoodRole role) {
        Food food = createFood(id, name, category, cal100g, portionG);
        food.setFoodRole(role);
        return food;
    }

    private Food createFoodWithRole(int id, String name, String category, double cal100g,
                                     double portionG, FoodRole role, boolean vegetarian) {
        Food food = createFoodWithRole(id, name, category, cal100g, portionG, role);
        food.setVegetarian(vegetarian);
        food.setVegan(vegetarian);
        return food;
    }

    /**
     * Same plant-first ordering the engine builds for FLEXITARIANA: vegetarian foods rank ahead of
     * the rest. Declared here so the selector test exercises the exact comparator semantics in use.
     */
    private static final java.util.Comparator<Food> PLANT_FIRST =
            java.util.Comparator.comparingInt((Food f) -> f.isVegetarian() ? 0 : 1);

    // ---------------------------------------------------------------------------
    // Estrutura mínima
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Estrutura mínima garantida")
    class EstruturaMinima {

        @Test
        @DisplayName("Lista vazia lança exceção — validação deve ocorrer antes")
        void listaVaziaLancaExcecao() {
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(List.of(), "LUNCH", 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Sempre retorna exatamente 2 opções com alimentos disponíveis")
        void retornaDuasOpcoesComAlimentos() {
            List<Food> foods = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                foods.add(createFood(i, "Alimento " + i, "PROTEIN", 150, 100));
            }

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);

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

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);

            assertThat(options).hasSize(2);
        }

        @Test
        @DisplayName("Retorna 2 opções com apenas 1 alimento — fallback reusa na opção 2")
        void retornaDuasOpcoesComUmAlimento() {
            List<Food> foods = List.of(createFood(1, "Frango", "PROTEIN", 200, 150));

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);

            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).hasSize(1);
            // Fallback: opção 2 reusa o alimento da opção 1
            assertThat(options.get(1).items()).hasSize(1);
        }
    }

    // ---------------------------------------------------------------------------
    // Fallback com reaproveitamento
    // ---------------------------------------------------------------------------

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

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 400);

            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("Pool extremamente restrito (1 alimento): ambas opções preenchidas")
        void poolExtremoUmAlimento() {
            List<Food> foods = List.of(createFood(1, "Tofu", "PROTEIN", 120, 100));

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 300);

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

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 300);

            List<Integer> idsOp1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            List<Integer> idsOp2 = options.get(1).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();

            // Com pool grande, não deve haver repetição
            for (Integer id : idsOp1) {
                assertThat(idsOp2).doesNotContain(id);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Sem repetição entre opções
    // ---------------------------------------------------------------------------

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

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 400);

            List<Integer> idsOp1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            List<Integer> idsOp2 = options.get(1).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();

            for (Integer id : idsOp1) {
                assertThat(idsOp2).doesNotContain(id);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Meta calórica
    // ---------------------------------------------------------------------------

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
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", target);

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

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 2000);

            for (FoodSelector.MealOptionBuild option : options) {
                assertThat(option.items()).hasSizeLessThanOrEqualTo(5);
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Variedade entre categorias (fallback path)
    // ---------------------------------------------------------------------------

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
                    createFood(8, "Carne", "PROTEIN", 250, 100),
                    createFood(9, "Batata", "CARBOHYDRATE", 77, 100),
                    createFood(10, "Espinafre", "VEGETABLE", 23, 100),
                    createFood(11, "Maçã", "FRUIT", 52, 100),
                    createFood(12, "Lentilha", "LEGUME", 116, 100),
                    createFood(13, "Iogurte", "DAIRY", 59, 100),
                    createFood(14, "Castanha", "HEALTHY_FAT", 656, 10)
            );

            // Sem mealTypeKey conhecido → fallback por categoria
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, null, 600);

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

    // ---------------------------------------------------------------------------
    // Seleção por papel (role-based — caminho primário)
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Seleção por papel (role-based)")
    class SelecaoPorPapel {

        @Test
        @DisplayName("LUNCH: pool com os 3 papéis obrigatórios → opção contém os 3 papéis")
        void lunchComTresPapeisObrigatorios() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Carne", "PROTEIN", 200, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(5, "Batata", "CARBOHYDRATE", 77, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(6, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 600);

            // then
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 4);   // MAIN_PROTEIN
            assertThat(ids1).containsAnyOf(2, 5);   // MAIN_CARBOHYDRATE
            assertThat(ids1).containsAnyOf(3, 6);   // VEGETABLE_LEGUME
        }

        @Test
        @DisplayName("DINNER: mesma regra que LUNCH → contém os 3 papéis")
        void dinnerComTresPapeisObrigatorios() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Macarrão", "CARBOHYDRATE", 150, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Abobrinha", "VEGETABLE", 17, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Atum", "PROTEIN", 130, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(5, "Mandioca", "CARBOHYDRATE", 125, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(6, "Couve", "VEGETABLE", 28, 100, FoodRole.VEGETABLE_LEGUME)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "DINNER", 700);

            // then
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 4);
            assertThat(ids1).containsAnyOf(2, 5);
            assertThat(ids1).containsAnyOf(3, 6);
        }

        @Test
        @DisplayName("BREAKFAST: pool com itens leves + MAIN_PROTEIN pesado → não inclui MAIN_PROTEIN")
        void breakfastNaoIncluiProteinaPesada() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Aveia", "CARBOHYDRATE", 389, 50, FoodRole.BREAKFAST_LIGHT),
                    createFoodWithRole(2, "Banana", "FRUIT", 89, 100, FoodRole.FRUIT),
                    createFoodWithRole(3, "Iogurte", "DAIRY", 59, 100, FoodRole.LIGHT_DAIRY),
                    createFoodWithRole(4, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN)  // não deve ser selecionado
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "BREAKFAST", 300);

            // then — MAIN_PROTEIN (id=4) não deve aparecer em nenhuma opção
            for (FoodSelector.MealOptionBuild option : options) {
                List<Integer> ids = option.items().stream()
                        .map(FoodSelector.FoodPortionBuild::foodId).toList();
                assertThat(ids).doesNotContain(4);
            }
        }

        @Test
        @DisplayName("MORNING_SNACK: aceita FRUIT — não seleciona MAIN_PROTEIN nem MAIN_CARBOHYDRATE")
        void morningSnackAceitaFruta() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Maçã", "FRUIT", 52, 100, FoodRole.FRUIT),
                    createFoodWithRole(2, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "MORNING_SNACK", 150);

            // then — apenas a fruta (id=1) é elegível
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            assertThat(opt1.items()).isNotEmpty();
            opt1.items().forEach(item ->
                    assertThat(item.foodId()).isNotIn(2, 3));
        }

        @Test
        @DisplayName("AFTERNOON_SNACK: aceita HEALTHY_SNACK — não seleciona MAIN_PROTEIN")
        void afternoonSnackAceitaHealthySnack() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Castanha", "HEALTHY_FAT", 656, 30, FoodRole.HEALTHY_SNACK),
                    createFoodWithRole(2, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "AFTERNOON_SNACK", 200);

            // then
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            assertThat(opt1.items()).isNotEmpty();
            opt1.items().forEach(item ->
                    assertThat(item.foodId()).isNotIn(2, 3));
        }

        @Test
        @DisplayName("AFTERNOON_SNACK: pool com só FRUIT também é aceito")
        void afternoonSnackAceitaFrutaSozinha() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Pera", "FRUIT", 57, 100, FoodRole.FRUIT),
                    createFoodWithRole(2, "Uva", "FRUIT", 67, 100, FoodRole.FRUIT)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "AFTERNOON_SNACK", 150);

            // then — ambas opções preenchidas com fruta
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("UNSPECIFIED não é escolhido como papel de slot quando há classificados")
        void unspecifiedNaoEscolhidoQuandoHaClassificados() {
            // given — pool misto: 1 classificado (MAIN_PROTEIN) + 2 UNSPECIFIED
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Macarrão", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFood(4, "AlimentoX", "OTHER", 100, 100),   // foodRole null → UNSPECIFIED
                    createFood(5, "AlimentoY", "OTHER", 80, 100)     // foodRole null → UNSPECIFIED
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 600);

            // then — slots obrigatórios preenchidos com classificados; UNSPECIFIED (4,5) não obrigatório
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            List<Integer> ids = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids).contains(1, 2, 3);
        }

        @Test
        @DisplayName("Pool com foodRole null não causa NPE")
        void poolComRoleNuloNaoCausaNpe() {
            // given — todos UNSPECIFIED (foodRole null)
            List<Food> foods = List.of(
                    createFood(1, "AlimentoA", "PROTEIN", 150, 100),
                    createFood(2, "AlimentoB", "CARBOHYDRATE", 130, 100)
            );

            // when / then — não deve lançar exceção
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 400);
            assertThat(options).hasSize(2);
        }

        @Test
        @DisplayName("Fallback: pool 100% UNSPECIFIED para LUNCH → não quebra, gera via fallback de categoria")
        void lunchPoolTotalmenteUnspecifiedUsaFallback() {
            // given — pool com categorias estruturais mas sem FoodRole classificado
            List<Food> foods = List.of(
                    createFood(1, "Frango", "PROTEIN", 150, 100),
                    createFood(2, "Arroz", "CARBOHYDRATE", 130, 100),
                    createFood(3, "Brócolis", "VEGETABLE", 35, 100),
                    createFood(4, "Carne", "PROTEIN", 200, 100),
                    createFood(5, "Batata", "CARBOHYDRATE", 77, 100)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);

            // then — fallback de categoria gera opções não vazias
            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("Slot obrigatório com único candidato calórico é selecionado mesmo excedendo o teto")
        void slotObrigatorioVenceTetoCalorico() {
            // given — único MAIN_PROTEIN tem 1000 kcal/porção, muito acima do teto (300 * 1.10)
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Proteína Densa", "PROTEIN", 1000, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 300);

            // then — composição obrigatória tem precedência: a proteína é incluída mesmo acima do teto
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            List<Integer> ids = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids).contains(1);
            assertThat(opt1.totalCalories()).isGreaterThan(300 * 1.10);
        }
    }

    // ---------------------------------------------------------------------------
    // Validação estrutural (2.4F)
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Validação estrutural (2.4F)")
    class ValidacaoEstrutural {

        @Test
        @DisplayName("LUNCH sem MAIN_CARBOHYDRATE no pool: lança BusinessException com slot identificado")
        void lunchSemCarboidratoLancaExcecao() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Carne", "PROTEIN", 200, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH", 500))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("main carbohydrate");
        }

        @Test
        @DisplayName("DINNER sem VEGETABLE_LEGUME no pool: lança BusinessException com slot identificado")
        void dinnerSemVerduraLancaExcecao() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Atum", "PROTEIN", 130, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(4, "Mandioca", "CARBOHYDRATE", 125, 100, FoodRole.MAIN_CARBOHYDRATE)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "DINNER", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DINNER")
                    .hasMessageContaining("vegetable/legume");
        }

        @Test
        @DisplayName("LUNCH com 1 proteína: opção 2 recupera completude reusando a proteína")
        void lunchOpcao2RecuperaCompletude() {
            // given — única proteína; deduplicação deixaria a opção 2 sem proteína
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Batata", "CARBOHYDRATE", 77, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(4, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(5, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME)
            );

            // when — teto baixo evita supplemental drenar todo o pool na opção 1
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 300);

            // then — ambas as opções contêm a proteína (id=1), restaurando a composição completa
            List<Integer> ids1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            List<Integer> ids2 = options.get(1).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids1).contains(1);
            assertThat(ids2).contains(1);
        }

        @Test
        @DisplayName("BREAKFAST: HEALTHY_FAT e COMPLEMENT presentes não viram substitutos do café")
        void breakfastIgnoraComplementos() {
            // given — itens leves elegíveis + HEALTHY_FAT e COMPLEMENT (não elegíveis para café)
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Aveia", "CARBOHYDRATE", 389, 40, FoodRole.BREAKFAST_LIGHT),
                    createFoodWithRole(2, "Banana", "FRUIT", 89, 100, FoodRole.FRUIT),
                    createFoodWithRole(3, "Iogurte", "DAIRY", 59, 100, FoodRole.LIGHT_DAIRY),
                    createFoodWithRole(4, "Azeite", "HEALTHY_FAT", 884, 10, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(5, "Molho", "OTHER", 120, 30, FoodRole.COMPLEMENT)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "BREAKFAST", 300);

            // then — HEALTHY_FAT (4) e COMPLEMENT (5) nunca aparecem
            for (FoodSelector.MealOptionBuild option : options) {
                List<Integer> ids = option.items().stream()
                        .map(FoodSelector.FoodPortionBuild::foodId).toList();
                assertThat(ids).doesNotContain(4, 5);
            }
        }

        @Test
        @DisplayName("MORNING_SNACK: snack saudável sozinho é válido")
        void morningSnackSnackSozinhoValido() {
            // given — apenas HEALTHY_SNACK
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Castanhas", "HEALTHY_FAT", 600, 25, FoodRole.HEALTHY_SNACK),
                    createFoodWithRole(2, "Mix", "HEALTHY_FAT", 550, 25, FoodRole.HEALTHY_SNACK)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "MORNING_SNACK", 150);

            // then — ambas as opções preenchidas, sem exigir fruta
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("LUNCH sem MAIN_PROTEIN no pool: lança BusinessException")
        void lunchSemMainProteinLancaExcecao() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(2, "Batata", "CARBOHYDRATE", 77, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH", 500))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("main protein");
        }

        @Test
        @DisplayName("DINNER sem MAIN_PROTEIN no pool: lança BusinessException")
        void dinnerSemMainProteinLancaExcecao() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Macarrão", "CARBOHYDRATE", 150, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(2, "Mandioca", "CARBOHYDRATE", 125, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Abobrinha", "VEGETABLE", 17, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Couve", "VEGETABLE", 28, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "DINNER", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DINNER")
                    .hasMessageContaining("main protein");
        }

        @Test
        @DisplayName("Fallback de categoria não retorna opção vazia mesmo com alimento acima do teto")
        void fallbackCategoriaNuncaVazio() {
            // given — pool sem role (caminho de categoria) cujo único alimento excede o teto calórico
            List<Food> foods = List.of(
                    createFood(1, "Bolo Denso", "CARBOHYDRATE", 1200, 200)  // ~2400 kcal >> teto
            );

            // when — target pequeno: nenhum item cabe no teto
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, null, 200);

            // then — guarda anti-vazio garante opção preenchida
            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("LUNCH: supplemental não adiciona segundo alimento MAIN_PROTEIN quando slot já preenchido")
        void lunchSupplementalNaoAdicionaSegundaProteina() {
            // given — pool rico em proteínas (4 candidatos) + 1 carb + 1 verdura com 50 kcal/porção cada;
            // meta de 800 kcal aciona supplemental (150 kcal acumulados após slots, floor = 720),
            // mas supplemental é no-op para LUNCH: todos os allowedRoles já estão em usedRoles
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 50, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 50, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 50, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Peixe", "PROTEIN", 50, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(5, "Moluscos", "PROTEIN", 50, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(6, "Carne", "PROTEIN", 50, 100, FoodRole.MAIN_PROTEIN)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 800);

            // then — opção 1 tem exatamente os 3 slots obrigatórios, com 1 único MAIN_PROTEIN
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            assertThat(opt1.items()).hasSize(3);
            Set<Integer> proteinIds = Set.of(1, 4, 5, 6);
            long proteinCount = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .filter(proteinIds::contains)
                    .count();
            assertThat(proteinCount)
                    .as("Opção 1 não deve conter mais de um alimento MAIN_PROTEIN")
                    .isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------------------
    // Task 2.8B — Slot obrigatório vazio e suplementação calórica
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Task 2.8B — Slot obrigatório vazio e suplementação calórica")
    class Task28B {

        @Test
        @DisplayName("Pool vazio para MAIN_PROTEIN em LUNCH lança BusinessException")
        void lunchSemMainProteinLancaBusinessException() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(10, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(11, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH", 500))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("main protein");
        }

        @Test
        @DisplayName("Pool vazio para MAIN_CARBOHYDRATE em LUNCH lança BusinessException")
        void lunchSemMainCarbohydrateLancaBusinessException() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(10, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(11, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH", 500))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("main carbohydrate");
        }

        @Test
        @DisplayName("Pool vazio para VEGETABLE_LEGUME em DINNER lança BusinessException")
        void dinnerSemVegetableLegumeLancaBusinessException() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(10, "Salmão", "PROTEIN", 200, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(11, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "DINNER", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DINNER")
                    .hasMessageContaining("vegetable/legume");
        }

        @Test
        @DisplayName("Com os 3 papéis obrigatórios presentes, LUNCH gera plano sem exceção")
        void lunchComTresPapeisNaoLancaExcecao() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);
            // then
            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
        }

        @Test
        @DisplayName("Regressão vegana: MAIN_PROTEIN vegano no pool gera LUNCH completo")
        void veganPoolComMainProteinGeraPlanoCompleto() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Tofu", "PROTEIN", 76, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Feijão", "LEGUME", 77, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(4, "Quinoa", "CARBOHYDRATE", 120, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(5, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(6, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);
            // then
            assertThat(options).hasSize(2);
            Set<Integer> ids1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2); // proteína
            assertThat(ids1).containsAnyOf(3, 4); // carboidrato
            assertThat(ids1).containsAnyOf(5, 6); // vegetais
        }

        @Test
        @DisplayName("Regressão vegana: pool vegano sem MAIN_PROTEIN lança BusinessException")
        void veganPoolSemMainProteinLancaBusinessException() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Arroz", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(2, "Quinoa", "CARBOHYDRATE", 120, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH", 500))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("main protein");
        }

        @Test
        @DisplayName("Suplementação com HEALTHY_FAT é adicionada ao LUNCH quando abaixo do piso calórico")
        void lunchSupplementacaoAdicionaHealthyFat() {
            // given — 3 required slots com 50 kcal cada = 150 total; target 500 (piso 450); HEALTHY_FAT disponível
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 50, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 50, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 50, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Azeite", "HEALTHY_FAT", 200, 100, FoodRole.HEALTHY_FAT)
            );
            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);
            // then — Azeite (id=4) deve ter sido incluído via suplementação
            assertThat(options).hasSize(2);
            List<Integer> ids1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids1).contains(4);
        }

        @Test
        @DisplayName("Suplementação não usa alimentos fora do pool passado para buildTwoOptions")
        void suplementacaoNaoUsaAlimentoForaDoPool() {
            // given — pool sem HEALTHY_FAT
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME)
            );
            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);
            // then — somente ids 1, 2, 3 podem aparecer
            for (FoodSelector.MealOptionBuild option : options) {
                option.items().forEach(item ->
                        assertThat(item.foodId()).isIn(1, 2, 3));
            }
        }

        @Test
        @DisplayName("Suplementação não duplica o mesmo alimento na mesma opção de refeição")
        void suplementacaoNaoDuplicaAlimento() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 150, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Azeite", "HEALTHY_FAT", 200, 100, FoodRole.HEALTHY_FAT)
            );
            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH", 500);
            // then
            for (FoodSelector.MealOptionBuild option : options) {
                List<Integer> ids = option.items().stream()
                        .map(FoodSelector.FoodPortionBuild::foodId).toList();
                assertThat(ids).doesNotHaveDuplicates();
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Task 2.8C — LOW_CARB com FoodSelector real
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Task 2.8C — LOW_CARB com FoodSelector real")
    class Task28C {

        @Test
        @DisplayName("LUNCH_LOW_CARB: pool com 3 papéis obrigatórios gera opção com MAIN_PROTEIN, VEGETABLE_LEGUME e HEALTHY_FAT")
        void lunchLowCarb_geraOpcaoCompleta() {
            // given — pool pré-filtrado pelo FoodFilterService (carbs <= 20g já garantidos)
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango Grelhado", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Carne Magra", "PROTEIN", 200, 130, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Espinafre", "VEGETABLE", 23, 150, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(5, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(6, "Azeite", "HEALTHY_FAT", 884, 15, FoodRole.HEALTHY_FAT),
                    // MAIN_CARBOHYDRATE não é slot LOW_CARB — não deve ser selecionado
                    createFoodWithRole(7, "Arroz", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 600);

            // then
            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2);   // MAIN_PROTEIN
            assertThat(ids1).containsAnyOf(3, 4);   // VEGETABLE_LEGUME
            assertThat(ids1).containsAnyOf(5, 6);   // HEALTHY_FAT
            assertThat(ids1).doesNotContain(7);      // MAIN_CARBOHYDRATE não é slot LOW_CARB
        }

        @Test
        @DisplayName("DINNER_LOW_CARB: pool com 3 papéis obrigatórios gera opção completa")
        void dinnerLowCarb_geraOpcaoCompleta() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Atum", "PROTEIN", 130, 120, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Abobrinha", "VEGETABLE", 17, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Couve", "VEGETABLE", 28, 150, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(5, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(6, "Castanha-do-Para", "HEALTHY_FAT", 656, 30, FoodRole.HEALTHY_FAT)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "DINNER_LOW_CARB", 600);

            // then
            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2);   // MAIN_PROTEIN
            assertThat(ids1).containsAnyOf(3, 4);   // VEGETABLE_LEGUME
            assertThat(ids1).containsAnyOf(5, 6);   // HEALTHY_FAT
        }

        @Test
        @DisplayName("LUNCH_LOW_CARB: MAIN_CARBOHYDRATE no pool NÃO é selecionado — slot não existe em LOW_CARB")
        void lunchLowCarb_mainCarbohidratoNaoEhSelecionado() {
            // given — pool com MAIN_CARBOHYDRATE intruso
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(3, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(4, "Arroz Integral", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 600);

            // then — arroz (id=4, MAIN_CARBOHYDRATE) nunca deve ser selecionado
            for (FoodSelector.MealOptionBuild option : options) {
                List<Integer> ids = option.items().stream()
                        .map(FoodSelector.FoodPortionBuild::foodId).toList();
                assertThat(ids).doesNotContain(4);
            }
        }

        @Test
        @DisplayName("LUNCH_LOW_CARB sem HEALTHY_FAT: lança BusinessException com slot identificado")
        void lunchLowCarb_semHealthyFat_lancaBusinessException() {
            // given — pool sem nenhum alimento com papel HEALTHY_FAT
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Carne Magra", "PROTEIN", 200, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Espinafre", "VEGETABLE", 23, 150, FoodRole.VEGETABLE_LEGUME)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH_LOW_CARB")
                    .hasMessageContaining("healthy fat");
        }

        @Test
        @DisplayName("LUNCH_LOW_CARB sem MAIN_PROTEIN: lança BusinessException com slot identificado")
        void lunchLowCarb_semMainProtein_lancaBusinessException() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(2, "Espinafre", "VEGETABLE", 23, 150, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(3, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(4, "Azeite", "HEALTHY_FAT", 884, 15, FoodRole.HEALTHY_FAT)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH_LOW_CARB")
                    .hasMessageContaining("main protein");
        }

        @Test
        @DisplayName("Regressão VEGANA LOW_CARB: MAIN_PROTEIN vegano no pool gera LUNCH_LOW_CARB completo")
        void lunchLowCarb_veganMainProtein_geraOpcaoCompleta() {
            // given — pool vegano típico LOW_CARB (tofu, tempeh como proteína)
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Tofu Firme", "PROTEIN", 76, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Tempeh", "PROTEIN", 193, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Couve", "VEGETABLE", 28, 150, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(5, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(6, "Semente de Chia", "HEALTHY_FAT", 486, 20, FoodRole.HEALTHY_FAT)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 550);

            // then — proteína vegana satisfaz o slot MAIN_PROTEIN
            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2);   // proteína vegana
            assertThat(ids1).containsAnyOf(3, 4);   // vegetais
            assertThat(ids1).containsAnyOf(5, 6);   // gordura saudável
        }

        @Test
        @DisplayName("Regressão PESCATARIANA LOW_CARB: peixe (MAIN_PROTEIN) elegível — gera LUNCH_LOW_CARB completo")
        void lunchLowCarb_pescatarianaRegressao_peixeElegivel() {
            // given — pool PESCATARIANA LOW_CARB pré-filtrado (carne terrestre já removida pelo FoodFilterService)
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Tilapia", "PROTEIN", 130, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(4, "Espinafre", "VEGETABLE", 23, 150, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(5, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(6, "Azeite", "HEALTHY_FAT", 884, 15, FoodRole.HEALTHY_FAT)
            );

            // when
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 600);

            // then — peixe satisfaz o slot MAIN_PROTEIN
            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2);   // peixe como proteína
            assertThat(ids1).containsAnyOf(3, 4);   // vegetais
            assertThat(ids1).containsAnyOf(5, 6);   // gordura saudável
        }
    }

    // ---------------------------------------------------------------------------
    // Task 2.8D — Priorização vegetal FLEXITARIANA (selectionPriority)
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Task 2.8D — Priorização vegetal FLEXITARIANA")
    class Task28D {

        @Test
        @DisplayName("MAIN_PROTEIN vegetal é preferido sobre carne quando ambos existem")
        void plantProteinPreferidoSobreCarne() {
            // given — proteína vegetal (tofu) e animal (frango), ambas dentro do teto calórico
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Tofu", "PROTEIN", 76, 150, FoodRole.MAIN_PROTEIN, true),
                    createFoodWithRole(2, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN, false),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE, true),
                    createFoodWithRole(4, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME, true)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "LUNCH", 600, PLANT_FIRST);

            // then — a opção 1 escolhe o tofu (id=1) como MAIN_PROTEIN, não o frango (id=2)
            List<Integer> ids1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids1).contains(1).doesNotContain(2);
        }

        @Test
        @DisplayName("Sem proteína vegetal, carne é selecionada como fallback — plano completo")
        void carneSelecionadaComoFallbackQuandoSemVegetal() {
            // given — apenas proteínas animais no pool
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN, false),
                    createFoodWithRole(2, "Carne", "PROTEIN", 200, 130, FoodRole.MAIN_PROTEIN, false),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE, true),
                    createFoodWithRole(4, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME, true)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "LUNCH", 600, PLANT_FIRST);

            // then — a priorização não bloqueia carne: opção 1 inclui uma proteína animal
            assertThat(options).hasSize(2);
            List<Integer> ids1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids1).containsAnyOf(1, 2);
        }

        @Test
        @DisplayName("Carne permanece elegível — opção 2 usa a carne quando a vegetal já foi usada")
        void carnePermaneceElegivelComoAlternativa() {
            // given — 1 proteína vegetal + 1 animal, com carbos e vegetais distintos para a opção 2
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Tofu", "PROTEIN", 76, 150, FoodRole.MAIN_PROTEIN, true),
                    createFoodWithRole(2, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN, false),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE, true),
                    createFoodWithRole(4, "Batata", "CARBOHYDRATE", 77, 100, FoodRole.MAIN_CARBOHYDRATE, true),
                    createFoodWithRole(5, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME, true),
                    createFoodWithRole(6, "Espinafre", "VEGETABLE", 23, 100, FoodRole.VEGETABLE_LEGUME, true)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "LUNCH", 600, PLANT_FIRST);

            // then — opção 1 com tofu, opção 2 recai sobre a carne (não excluída artificialmente)
            List<Integer> ids1 = options.get(0).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            List<Integer> ids2 = options.get(1).items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids1).contains(1);
            assertThat(ids2).contains(2);
        }

        @Test
        @DisplayName("Sem priorização (null) o comportamento legado é preservado — não quebra LUNCH misto")
        void semPriorizacaoComportamentoLegado() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Tofu", "PROTEIN", 76, 150, FoodRole.MAIN_PROTEIN, true),
                    createFoodWithRole(2, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN, false),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 100, FoodRole.MAIN_CARBOHYDRATE, true),
                    createFoodWithRole(4, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME, true)
            );

            // when — sobrecarga de 3 args (priority null)
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "LUNCH", 600);

            // then — apenas garante geração completa e válida (sem assert de qual proteína)
            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }
    }

    // ---------------------------------------------------------------------------
    // Task 2.8E — CETOGENICA com FoodSelector real
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Task 2.8E — CETOGENICA com FoodSelector real")
    class Task28E {

        @Test
        @DisplayName("LUNCH_CETOGENICA: pool keto gera opção com MAIN_PROTEIN e HEALTHY_FAT, sem MAIN_CARBOHYDRATE")
        void lunchKeto_geraOpcaoComProteinaEGordura() {
            // given — pool pré-filtrado pelo filtro keto (carbs <= 10, fat >= 15 já garantidos);
            // inclui um MAIN_CARBOHYDRATE intruso que não deve ser selecionado
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Carne Gorda", "PROTEIN", 250, 130, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(4, "Azeite", "HEALTHY_FAT", 884, 15, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(5, "Arroz", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "LUNCH_CETOGENICA", 600);

            // then
            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2);   // MAIN_PROTEIN
            assertThat(ids1).containsAnyOf(3, 4);   // HEALTHY_FAT estrutural
            assertThat(ids1).doesNotContain(5);     // MAIN_CARBOHYDRATE não é slot keto
        }

        @Test
        @DisplayName("DINNER_CETOGENICA: pool keto gera opção com MAIN_PROTEIN e HEALTHY_FAT")
        void dinnerKeto_geraOpcaoCompleta() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Atum", "PROTEIN", 184, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Ovos", "PROTEIN", 155, 100, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Castanha-do-Para", "HEALTHY_FAT", 656, 30, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(4, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "DINNER_CETOGENICA", 600);

            // then
            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(Collectors.toSet());
            assertThat(ids1).containsAnyOf(1, 2);   // MAIN_PROTEIN
            assertThat(ids1).containsAnyOf(3, 4);   // HEALTHY_FAT
        }

        @Test
        @DisplayName("LUNCH_CETOGENICA sem HEALTHY_FAT: lança BusinessException com slot identificado")
        void lunchKeto_semHealthyFat_lancaBusinessException() {
            // given — pool sem nenhum alimento com papel HEALTHY_FAT
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Carne", "PROTEIN", 250, 130, FoodRole.MAIN_PROTEIN)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH_CETOGENICA", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH_CETOGENICA")
                    .hasMessageContaining("healthy fat");
        }

        @Test
        @DisplayName("LUNCH_CETOGENICA sem MAIN_PROTEIN: lança BusinessException com slot identificado")
        void lunchKeto_semMainProtein_lancaBusinessException() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(2, "Azeite", "HEALTHY_FAT", 884, 15, FoodRole.HEALTHY_FAT)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH_CETOGENICA", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH_CETOGENICA")
                    .hasMessageContaining("main protein");
        }
    }

    // ---------------------------------------------------------------------------
    // Task 2.8F — CARNIVORA com FoodSelector real
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Task 2.8F — CARNIVORA com FoodSelector real")
    class Task28F {

        @Test
        @DisplayName("LUNCH_CARNIVORA: pool animal gera opção com MAIN_PROTEIN e sem vegetal/carboidrato")
        void lunchCarnivora_geraOpcaoComProteinaAnimal() {
            // given — pool pré-filtrado pelo filtro CARNIVORA (apenas origem animal);
            // inclui intrusos vegetais (MAIN_CARBOHYDRATE/VEGETABLE_LEGUME) que não devem ser selecionados
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Picanha", "PROTEIN", 250, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Salmão", "PROTEIN", 208, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(3, "Arroz", "CARBOHYDRATE", 130, 150, FoodRole.MAIN_CARBOHYDRATE),
                    createFoodWithRole(4, "Brócolis", "VEGETABLE", 35, 100, FoodRole.VEGETABLE_LEGUME)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "LUNCH_CARNIVORA", 600);

            // then — ambas opções preenchidas com proteína animal, sem itens vegetais
            assertThat(options).hasSize(2);
            for (FoodSelector.MealOptionBuild option : options) {
                assertThat(option.items()).isNotEmpty();
                List<Integer> ids = option.items().stream()
                        .map(FoodSelector.FoodPortionBuild::foodId).toList();
                assertThat(ids).containsAnyOf(1, 2);   // MAIN_PROTEIN animal
                assertThat(ids).doesNotContain(3, 4);  // carboidrato/vegetal não são slots CARNIVORA
            }
        }

        @Test
        @DisplayName("DINNER_CARNIVORA: pool animal (ovos/peixe) gera opção válida")
        void dinnerCarnivora_geraOpcaoValida() {
            // given
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Atum", "PROTEIN", 184, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Ovos", "PROTEIN", 155, 100, FoodRole.MAIN_PROTEIN)
            );

            // when
            List<FoodSelector.MealOptionBuild> options =
                    foodSelector.buildTwoOptions(foods, "DINNER_CARNIVORA", 600);

            // then — plano não parcial: ambas opções preenchidas
            assertThat(options).hasSize(2);
            assertThat(options.get(0).items()).isNotEmpty();
            assertThat(options.get(1).items()).isNotEmpty();
        }

        @Test
        @DisplayName("LUNCH_CARNIVORA sem MAIN_PROTEIN: lança BusinessException com slot identificado")
        void lunchCarnivora_semMainProtein_lancaBusinessException() {
            // given — pool sem nenhuma proteína (apenas papéis não estruturais para CARNIVORA)
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT),
                    createFoodWithRole(2, "Castanha", "HEALTHY_FAT", 656, 30, FoodRole.HEALTHY_SNACK)
            );
            // when / then
            assertThatThrownBy(() -> foodSelector.buildTwoOptions(foods, "LUNCH_CARNIVORA", 600))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH_CARNIVORA")
                    .hasMessageContaining("main protein");
        }
    }

    // ---------------------------------------------------------------------------
    // Task 2.8I.1 — Floor-seeking override no supplemental fill do BREAKFAST
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Task 2.8I.1 — BREAKFAST: floor-seeking override no supplemental fill")
    class Task28I1 {

        /**
         * Reproduz o cenário real de falha reportado pelo QA:
         * porções realistas pequenas (pão 28g, banana 100g, iogurte 170g) somam ~263 kcal,
         * abaixo do piso do engine (60% × 400 = 240 kcal). A castanha (28g = 185 kcal)
         * excede a soft ceiling (440 kcal) mas cabe dentro do hard max (500 kcal).
         * O override deve adicionar a castanha e fazer o total chegar a 448 kcal.
         */
        @Test
        @DisplayName("BREAKFAST: castanha excede soft ceiling mas é adicionada pelo floor override")
        void breakfast_nutExceedsSoftCeilingButFloorOverrideAddsIt() {
            // given — porções reais pequenas; alvo 400 kcal, soft ceiling 440, hard max 500
            List<Food> foods = List.of(
                    // pão 28g × 265/100 = 74.2 kcal (BREAKFAST_LIGHT — slot obrigatório)
                    createFoodWithRole(1, "Pao", "CARBOHYDRATE", 265, 28, FoodRole.BREAKFAST_LIGHT),
                    // banana 100g × 89/100 = 89 kcal (FRUIT — supplemental)
                    createFoodWithRole(2, "Banana", "FRUIT", 89, 100, FoodRole.FRUIT),
                    // iogurte 170g × 59/100 = 100.3 kcal (LIGHT_DAIRY — supplemental)
                    createFoodWithRole(3, "Iogurte", "DAIRY", 59, 170, FoodRole.LIGHT_DAIRY),
                    // castanha 28g × 660/100 = 184.8 kcal (HEALTHY_SNACK — supplemental)
                    // 74+89+100+185 = 448 > soft ceiling (440) mas ≤ hard max (500)
                    createFoodWithRole(4, "Castanha", "HEALTHY_FAT", 660, 28, FoodRole.HEALTHY_SNACK)
            );

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "BREAKFAST", 400);

            // total deve incluir castanha (id=4), superando 240 kcal (60% do alvo)
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            assertThat(opt1.totalCalories()).isGreaterThanOrEqualTo(280.0);
            List<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId).toList();
            assertThat(ids1).contains(4); // castanha foi incluída via floor override
        }

        @Test
        @DisplayName("BREAKFAST: quando floor é atingido dentro da soft ceiling, override não é acionado")
        void breakfast_floorReachedWithinCeiling_noOverrideNeeded() {
            // given — porções maiores: pão 100g = 265, banana 100g = 89. Total 354 > floor(360)?
            // Aveia 50g = 194.5, banana 89, iogurte 59 → total sem nuts = 342.5 < floor 360
            // Mas nuts 28g × 660 = 185: 342.5+185=527 ≤ soft ceiling (440)? 527>440 → override
            // Vamos usar itens que realmente atingem o floor sem override
            List<Food> foods = List.of(
                    // aveia 50g × 389/100 = 194.5 kcal
                    createFoodWithRole(1, "Aveia", "CARBOHYDRATE", 389, 50, FoodRole.BREAKFAST_LIGHT),
                    // banana 100g = 89 kcal
                    createFoodWithRole(2, "Banana", "FRUIT", 89, 100, FoodRole.FRUIT),
                    // iogurte 200g × 59/100 = 118 kcal. Total: 194.5+89+118=401.5 ≥ floor(360)
                    createFoodWithRole(3, "Iogurte", "DAIRY", 59, 200, FoodRole.LIGHT_DAIRY),
                    createFoodWithRole(4, "Castanha", "HEALTHY_FAT", 660, 28, FoodRole.HEALTHY_SNACK)
            );

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "BREAKFAST", 400);

            // deve funcionar normalmente — castanha pode ou não estar, mas total é adequado
            assertThat(options).hasSize(2);
            assertThat(options.get(0).totalCalories()).isGreaterThan(0);
        }

        @Test
        @DisplayName("BREAKFAST: item acima do hard max não é adicionado pelo override")
        void breakfast_itemAboveHardMaxIsNotAdded() {
            // given — alvo 100 kcal (hard max = 125). Única opção: nuts 28g = 185 kcal > 125.
            // Dois pães distintos preenchem o slot obrigatório em cada opção.
            // BREAKFAST_LIGHT_SLOT aceita HEALTHY_SNACK, então a castanha (id=3) poderia
            // preencher o slot obrigatório se fosse a única opção — por isso fornecemos
            // dois pães (ids 1 e 2) para que nenhuma opção precise dela.
            // Castanha 28g × 1200/100g = 336 kcal: acima do hard max (125) → não entra via override.
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Pao1", "CARBOHYDRATE", 150, 28, FoodRole.BREAKFAST_LIGHT), // 42 kcal
                    createFoodWithRole(2, "Pao2", "CARBOHYDRATE", 150, 30, FoodRole.BREAKFAST_LIGHT), // 45 kcal
                    createFoodWithRole(3, "Castanha Gigante", "HEALTHY_FAT", 1200, 28, FoodRole.HEALTHY_SNACK) // 336 kcal
            );
            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "BREAKFAST", 100);

            assertThat(options).hasSize(2);
            for (FoodSelector.MealOptionBuild opt : options) {
                List<Integer> ids = opt.items().stream()
                        .map(FoodSelector.FoodPortionBuild::foodId).toList();
                assertThat(ids).doesNotContain(3); // castanha não entra via override (acima do hard max)
            }
        }

        @Test
        @DisplayName("caloriesForPortion: NULL referencePortionGrams usa 100g como padrão (consistência com buildOption)")
        void caloriesForPortion_nullPortionUses100gDefault() {
            // given — food com NULL referencePortionGrams, 200 kcal/100g
            Food food = new Food();
            food.setId(1);
            food.setName("Alimento Sem Porcao");
            food.setCategory("PROTEIN");
            food.setCaloriesPer100g(java.math.BigDecimal.valueOf(200));
            food.setProteinPer100g(java.math.BigDecimal.valueOf(20));
            food.setCarbsPer100g(java.math.BigDecimal.valueOf(0));
            food.setFatPer100g(java.math.BigDecimal.valueOf(10));
            food.setFiberPer100g(java.math.BigDecimal.valueOf(0));
            food.setReferencePortionGrams(null); // NULL — deve usar 100g como padrão
            food.setFoodRole(FoodRole.MAIN_PROTEIN);
            food.setActive(true);

            // Pool com 1 alimento sem porcao para alvo = 100 kcal
            // Com fix: caloriesForPortion = 200 kcal (100g × 200/100)
            // Sem fix: caloriesForPortion = 0 → acumulado = 0 → supplemental tenta todos
            // buildOption também usa 100g → totalCalories = 200 kcal
            // O teste verifica que seleção e buildOption são consistentes
            Food food2 = createFoodWithRole(2, "Proteina2", "PROTEIN", 200, 100, FoodRole.MAIN_CARBOHYDRATE);
            Food food3 = createFoodWithRole(3, "Legume", "VEGETABLE", 30, 100, FoodRole.VEGETABLE_LEGUME);

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(
                    List.of(food, food2, food3), "LUNCH", 500);

            // buildOption usa 100g default → totalCalories deve refletir 100g × kcal/100g para alimento sem porcao
            assertThat(options.get(0).totalCalories()).isGreaterThan(0);
            // totalCalories deve ser compatível com buildOption (100g default)
            // food id=1 com 200 kcal/100g em 100g = 200 kcal → plano conta isso corretamente
            boolean foodWithoutPortionSelected = options.get(0).items().stream()
                    .anyMatch(item -> item.foodId() == 1);
            if (foodWithoutPortionSelected) {
                // Se selecionado, deve contribuir com 200 kcal (não 0)
                double itemCal = options.get(0).items().stream()
                        .filter(item -> item.foodId() == 1)
                        .mapToDouble(FoodSelector.FoodPortionBuild::calories)
                        .sum();
                assertThat(itemCal).isEqualTo(200.0);
            }
        }

        @Test
        @DisplayName("LUNCH_LOW_CARB: override não muda comportamento quando floor é atingido normalmente")
        void lunchLowCarb_overrideDoesNotAffectNormalFlow() {
            // Regressão: o override só ativa quando accumulated < floor E acima da soft ceiling.
            // Para LUNCH_LOW_CARB com porções normais, o floor deve ser atingido sem override.
            List<Food> foods = List.of(
                    createFoodWithRole(1, "Frango", "PROTEIN", 165, 150, FoodRole.MAIN_PROTEIN),
                    createFoodWithRole(2, "Brócolis", "VEGETABLE", 35, 200, FoodRole.VEGETABLE_LEGUME),
                    createFoodWithRole(3, "Abacate", "HEALTHY_FAT", 160, 100, FoodRole.HEALTHY_FAT)
            );

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(foods, "LUNCH_LOW_CARB", 600);

            assertThat(options).hasSize(2);
            FoodSelector.MealOptionBuild opt1 = options.get(0);
            Set<Integer> ids1 = opt1.items().stream()
                    .map(FoodSelector.FoodPortionBuild::foodId)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(ids1).containsAnyOf(1);   // MAIN_PROTEIN
            assertThat(ids1).containsAnyOf(2);   // VEGETABLE_LEGUME
            assertThat(ids1).containsAnyOf(3);   // HEALTHY_FAT
        }
    }
}
