package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.repository.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodFilterServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private FoodFilterService foodFilterService;

    private Food createFood(int id, String name, boolean lactose, boolean gluten, boolean egg,
                             boolean vegetarian, boolean vegan, double carbs100g) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCategory("PROTEIN");
        food.setContainsLactose(lactose);
        food.setContainsGluten(gluten);
        food.setContainsEgg(egg);
        food.setVegetarian(vegetarian);
        food.setVegan(vegan);
        food.setCarbsPer100g(BigDecimal.valueOf(carbs100g));
        food.setCaloriesPer100g(BigDecimal.valueOf(100));
        food.setReferencePortionGrams(BigDecimal.valueOf(100));
        food.setActive(true);
        food.setSuitableMeals("LUNCH,DINNER");
        return food;
    }

    private Assessment createAssessment(String allergies, String restrictions, String healthConditions) {
        Assessment a = new Assessment();
        a.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        a.setAllergies(allergies);
        a.setDietaryRestrictions(restrictions);
        a.setHealthConditions(healthConditions);
        return a;
    }

    @Nested
    @DisplayName("Filtro por alergias")
    class Alergias {

        @Test
        @DisplayName("Exclui alimentos com lactose quando alergia a lactose")
        void excluiLactose() {
            Food comLactose = createFood(1, "Queijo", true, false, false, true, false, 5);
            Food semLactose = createFood(2, "Frango", false, false, false, false, false, 0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(comLactose, semLactose));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment("lactose", null, null));

            assertThat(result).containsExactly(semLactose);
        }

        @Test
        @DisplayName("Exclui alimentos com glúten quando celíaco")
        void excluiGlutenCeliaco() {
            Food comGluten = createFood(1, "Pão", false, true, false, true, false, 50);
            Food semGluten = createFood(2, "Arroz", false, false, false, true, true, 28);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(comGluten, semGluten));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment("doença celíaca", null, null));

            assertThat(result).containsExactly(semGluten);
        }

        @Test
        @DisplayName("Exclui alimentos com ovo")
        void excluiOvo() {
            Food comOvo = createFood(1, "Bolo", false, false, true, true, false, 40);
            Food semOvo = createFood(2, "Salada", false, false, false, true, true, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(comOvo, semOvo));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment("ovos", null, null));

            assertThat(result).containsExactly(semOvo);
        }
    }

    @Nested
    @DisplayName("Filtro por dieta")
    class Dieta {

        @Test
        @DisplayName("Apenas veganos quando restrição vegana")
        void apenasVegano() {
            Food naoVegano = createFood(1, "Frango", false, false, false, false, false, 0);
            Food vegano = createFood(2, "Tofu", false, false, false, true, true, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(naoVegano, vegano));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, "vegano", null));

            assertThat(result).containsExactly(vegano);
        }

        @Test
        @DisplayName("Apenas vegetarianos quando restrição vegetariana")
        void apenasVegetariano() {
            Food carne = createFood(1, "Carne", false, false, false, false, false, 0);
            Food vegetariano = createFood(2, "Queijo", true, false, false, true, false, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(carne, vegetariano));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, "vegetariano", null));

            assertThat(result).containsExactly(vegetariano);
        }
    }

    @Nested
    @DisplayName("Filtro por condições de saúde")
    class CondicoesSaude {

        @Test
        @DisplayName("Exclui alimentos com mais de 60g carbs/100g para diabéticos")
        void excluiCarbsAltosDiabetico() {
            Food altoCarbsFood = createFood(1, "Açúcar", false, false, false, true, true, 99);
            Food baixoCarbsFood = createFood(2, "Frango", false, false, false, false, false, 0);
            Food moderadoFood = createFood(3, "Arroz", false, false, false, true, true, 28);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(altoCarbsFood, baixoCarbsFood, moderadoFood));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, null, "diabetes"));

            assertThat(result).containsExactlyInAnyOrder(baixoCarbsFood, moderadoFood);
        }

        @Test
        @DisplayName("Exclui bebidas para diabéticos")
        void excluiBebidasDiabetico() {
            Food bebida = createFood(1, "Suco", false, false, false, true, true, 12);
            bebida.setCategory("BEVERAGE");
            Food comida = createFood(2, "Salada", false, false, false, true, true, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(bebida, comida));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, null, "diabetes"));

            assertThat(result).containsExactly(comida);
        }
    }

    @Nested
    @DisplayName("Múltiplas restrições simultâneas")
    class MultiplasRestricoes {

        @Test
        @DisplayName("Vegano + sem glúten + diabético")
        void veganoSemGlutenDiabetico() {
            Food ok = createFood(1, "Tofu", false, false, false, true, true, 3);
            Food comGluten = createFood(2, "Seitan", false, true, false, true, true, 14);
            Food naoVegano = createFood(3, "Frango", false, false, false, false, false, 0);
            Food altoCarbs = createFood(4, "Mel vegano", false, false, false, true, true, 82);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(ok, comGluten, naoVegano, altoCarbs));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment("glúten", "vegano", "diabetes tipo 2"));

            assertThat(result).containsExactly(ok);
        }
    }

    @Nested
    @DisplayName("Normalização de entrada")
    class Normalizacao {

        @Test
        @DisplayName("Remove acentos e normaliza antes de comparar")
        void removeAcentos() {
            Set<String> result = foodFilterService.normalizeSet("Intolerância à lactose; Doença Celíaca");

            assertThat(result).contains("intolerancia a lactose", "doenca celiaca");
        }

        @Test
        @DisplayName("Trata campos vazios e nulos")
        void trataVaziosENulos() {
            assertThat(foodFilterService.normalizeSet(null)).isEmpty();
            assertThat(foodFilterService.normalizeSet("")).isEmpty();
            assertThat(foodFilterService.normalizeSet("   ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Agrupamento por refeição")
    class Agrupamento {

        @Test
        @DisplayName("Agrupa corretamente por refeicoes_indicadas")
        void agrupaCorreto() {
            Food cafe = createFood(1, "Pão", false, false, false, true, false, 50);
            cafe.setSuitableMeals("BREAKFAST,AFTERNOON_SNACK");
            Food almoco = createFood(2, "Arroz", false, false, false, true, true, 28);
            almoco.setSuitableMeals("LUNCH,DINNER");

            Map<String, List<Food>> grouped = foodFilterService.groupByMealType(List.of(cafe, almoco));

            assertThat(grouped).containsKeys("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
            assertThat(grouped.get("BREAKFAST")).containsExactly(cafe);
            assertThat(grouped.get("AFTERNOON_SNACK")).containsExactly(cafe);
            assertThat(grouped.get("LUNCH")).containsExactly(almoco);
            assertThat(grouped.get("DINNER")).containsExactly(almoco);
        }

        @Test
        @DisplayName("Usa tipo_refeicao_principal como fallback")
        void usaPrimaryComoFallback() {
            Food food = createFood(1, "Cereal", false, false, false, true, false, 70);
            food.setSuitableMeals(null);
            food.setPrimaryMealType("BREAKFAST");

            Map<String, List<Food>> grouped = foodFilterService.groupByMealType(List.of(food));

            assertThat(grouped.get("BREAKFAST")).containsExactly(food);
            assertThat(grouped.get("LUNCH")).isEmpty();
        }

        @Test
        @DisplayName("Sempre retorna 4 refeições mesmo sem alimentos")
        void sempreRetornaQuatroRefeicoes() {
            Map<String, List<Food>> grouped = foodFilterService.groupByMealType(List.of());

            assertThat(grouped).hasSize(4);
            assertThat(grouped).containsKeys("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }
    }
}
