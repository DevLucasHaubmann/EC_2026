package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Food;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodCurationServiceTest {

    private FoodCurationService curationService;

    @BeforeEach
    void setUp() {
        curationService = new FoodCurationService();
    }

    private Food createFood(int id, String name, String category) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCategory(category);
        food.setActive(true);
        return food;
    }

    private Food createFoodWithSubcategory(int id, String name, String category, String subcategory) {
        Food food = createFood(id, name, category);
        food.setSubcategory(subcategory);
        return food;
    }

    @Nested
    @DisplayName("Alimentos aceitáveis passam o filtro")
    class AlimentosAceitaveis {

        @Test
        @DisplayName("Alimentos comuns são mantidos")
        void alimentosComunsMantidos() {
            List<Food> foods = List.of(
                    createFood(1, "Arroz Integral", "CARBOHYDRATE"),
                    createFood(2, "Frango Grelhado", "PROTEIN"),
                    createFood(3, "Banana", "FRUIT"),
                    createFood(4, "Feijão Preto", "LEGUME")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Alimentos inadequados são removidos")
    class AlimentosInadequados {

        @Test
        @DisplayName("Aditivos culinários são removidos")
        void aditivosRemovidos() {
            List<Food> foods = List.of(
                    createFood(1, "Baking Soda", "OTHER"),
                    createFood(2, "Leavening agents, chemical", "OTHER"),
                    createFood(3, "Arroz Branco", "CARBOHYDRATE")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Arroz Branco");
        }

        @Test
        @DisplayName("Gorduras puras são removidas")
        void gordurasPurasRemovidas() {
            List<Food> foods = List.of(
                    createFood(1, "Lard, pork", "FAT"),
                    createFood(2, "Shortening industrial", "FAT"),
                    createFood(3, "Azeite de Oliva", "HEALTHY_FAT")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Azeite de Oliva");
        }

        @Test
        @DisplayName("Subprodutos e vísceras são removidos")
        void subprodutosRemovidos() {
            List<Food> foods = List.of(
                    createFood(1, "Chicken liver, cooked", "PROTEIN"),
                    createFood(2, "Beef by-products, mixed", "PROTEIN"),
                    createFood(3, "Chicken gizzard, fried", "PROTEIN"),
                    createFood(4, "Peito de Frango", "PROTEIN")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Peito de Frango");
        }

        @Test
        @DisplayName("Itens industriais são removidos")
        void itensIndustriaisRemovidos() {
            List<Food> foods = List.of(
                    createFood(1, "Bologna industrial, sliced", "PROTEIN"),
                    createFood(2, "Frango Grelhado", "PROTEIN")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Frango Grelhado");
        }

        @Test
        @DisplayName("Categorias bloqueadas são removidas")
        void categoriasBloqueadasRemovidas() {
            List<Food> foods = List.of(
                    createFood(1, "Sal Refinado", "CONDIMENT"),
                    createFood(2, "Pimenta do Reino", "SEASONING"),
                    createFood(3, "Feijão Carioca", "LEGUME")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Feijão Carioca");
        }

        @Test
        @DisplayName("Bicarbonato é removido")
        void bicarbonatorRemovido() {
            List<Food> foods = List.of(
                    createFood(1, "Bicarbonato de Sódio", "OTHER")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Suplementos e condimentos são removidos")
        void suplementosCondimentosRemovidos() {
            List<Food> foods = List.of(
                    createFood(1, "Whey Isolate Powder", "PROTEIN"),
                    createFood(2, "Fish oil capsule supplement", "FAT"),
                    createFood(3, "Vinegar, balsamic", "OTHER"),
                    createFood(4, "Iogurte Natural", "DAIRY")
            );

            List<Food> result = curationService.curate(foods);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Iogurte Natural");
        }
    }
}
