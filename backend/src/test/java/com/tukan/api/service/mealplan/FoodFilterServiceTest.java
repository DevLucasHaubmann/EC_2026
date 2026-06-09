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

    private Assessment createAssessmentWithDietType(Assessment.DietType dietType) {
        Assessment a = new Assessment();
        a.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        a.setDietType(dietType);
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
    @DisplayName("Filtro por dieta pescatariana")
    class Pescatariana {

        @Test
        @DisplayName("Permite peixes e frutos do mar para pescatariano")
        void permitePeixe() {
            Food peixe = createFood(1, "Salmão", false, false, false, false, false, 0);
            peixe.setSubcategory("PEIXES_E_FRUTOS_DO_MAR");
            Food frango = createFood(2, "Frango", false, false, false, false, false, 0);
            frango.setSubcategory("AVES");
            Food tofu = createFood(3, "Tofu", false, false, false, true, true, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(peixe, frango, tofu));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, "PESCATARIANA", null));

            assertThat(result).containsExactlyInAnyOrder(peixe, tofu);
        }

        @Test
        @DisplayName("Bloqueia carnes terrestres para pescatariano")
        void bloqueaCarneTerrestre() {
            Food carne = createFood(1, "Carne Bovina", false, false, false, false, false, 0);
            carne.setSubcategory("CARNES_BOVINAS");
            Food porco = createFood(2, "Pernil", false, false, false, false, false, 0);
            porco.setSubcategory("CARNES_SUINAS");
            Food peixe = createFood(3, "Tilápia", false, false, false, false, false, 0);
            peixe.setSubcategory("PEIXES_E_FRUTOS_DO_MAR");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(carne, porco, peixe));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, "PESCATARIANA", null));

            assertThat(result).containsExactly(peixe);
        }

        @Test
        @DisplayName("Permite alimentos vegetarianos para pescatariano")
        void permiteVegetariano() {
            Food queijo = createFood(1, "Queijo", true, false, false, true, false, 5);
            Food carne = createFood(2, "Picanha", false, false, false, false, false, 0);
            carne.setSubcategory("CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(queijo, carne));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, "PESCATARIANA", null));

            assertThat(result).containsExactly(queijo);
        }
    }

    @Nested
    @DisplayName("Filtro por dietType (fonte primária)")
    class DietTypePrimario {

        @Test
        @DisplayName("PESCATARIANA via dietType permite peixes e vegetarianos")
        void pescatarianaViaDietType() {
            Food peixe = createFood(1, "Salmão", false, false, false, false, false, 0);
            peixe.setSubcategory("PEIXES_E_FRUTOS_DO_MAR");
            Food frango = createFood(2, "Frango", false, false, false, false, false, 0);
            frango.setSubcategory("AVES");
            Food tofu = createFood(3, "Tofu", false, false, false, true, true, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(peixe, frango, tofu));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.PESCATARIANA));

            assertThat(result).containsExactlyInAnyOrder(peixe, tofu);
        }

        @Test
        @DisplayName("VEGETARIANA via dietType exclui carnes")
        void vegetarianaViaDietType() {
            Food carne = createFood(1, "Carne", false, false, false, false, false, 0);
            Food queijo = createFood(2, "Queijo", true, false, false, true, false, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(carne, queijo));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.VEGETARIANA));

            assertThat(result).containsExactly(queijo);
        }

        @Test
        @DisplayName("VEGANA via dietType exclui não-veganos")
        void veganaViaDietType() {
            Food naoVegano = createFood(1, "Frango", false, false, false, false, false, 0);
            Food vegano = createFood(2, "Tofu", false, false, false, true, true, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(naoVegano, vegano));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.VEGANA));

            assertThat(result).containsExactly(vegano);
        }

        @Test
        @DisplayName("ONIVORA via dietType não aplica filtro de dieta")
        void onivoraViaDietType() {
            Food carne = createFood(1, "Carne", false, false, false, false, false, 0);
            Food vegano = createFood(2, "Tofu", false, false, false, true, true, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(carne, vegano));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.ONIVORA));

            assertThat(result).containsExactlyInAnyOrder(carne, vegano);
        }

        @Test
        @DisplayName("Fallback: dietType null usa keyword em dietaryRestrictions")
        void fallbackKeywordQuandoDietTypeNulo() {
            Food peixe = createFood(1, "Salmão", false, false, false, false, false, 0);
            peixe.setSubcategory("PEIXES_E_FRUTOS_DO_MAR");
            Food frango = createFood(2, "Frango", false, false, false, false, false, 0);
            frango.setSubcategory("AVES");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(peixe, frango));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessment(null, "PESCATARIANA", null));

            assertThat(result).containsExactly(peixe);
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
    @DisplayName("Filtro LOW_CARB")
    class LowCarbFilter {

        @Test
        @DisplayName("Exclui alimento com carbsPer100g = 25.0 quando dietType=LOW_CARB")
        void lowCarb_excludesFoodWithHighCarbs() {
            Food alto = createFood(1, "Batata", false, false, false, true, true, 25.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(alto));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.LOW_CARB));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Mantém alimento com carbsPer100g = 15.0 quando dietType=LOW_CARB")
        void lowCarb_keepsFoodWithLowCarbs() {
            Food baixo = createFood(1, "Frango", false, false, false, false, false, 15.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(baixo));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.LOW_CARB));

            assertThat(result).containsExactly(baixo);
        }

        @Test
        @DisplayName("Exclui alimento com carbsPer100g = 20.1 (acima do threshold)")
        void lowCarb_excludesFoodAtExactThresholdPlusOne() {
            Food food = createFood(1, "Cenoura", false, false, false, true, true, 20.1);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.LOW_CARB));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Mantém alimento com carbsPer100g = 20.0 (exatamente no threshold)")
        void lowCarb_keepsFoodAtExactThreshold() {
            Food food = createFood(1, "Pepino", false, false, false, true, true, 20.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.LOW_CARB));

            assertThat(result).containsExactly(food);
        }

        @Test
        @DisplayName("Exclui alimento com carbsPer100g = null quando dietType=LOW_CARB")
        void lowCarb_excludesFoodWithNullCarbs() {
            Food food = new Food();
            food.setId(1);
            food.setName("AlimentoSemDado");
            food.setCategory("PROTEIN");
            food.setContainsLactose(false);
            food.setContainsGluten(false);
            food.setContainsEgg(false);
            food.setVegetarian(true);
            food.setVegan(true);
            food.setCarbsPer100g(null);
            food.setCaloriesPer100g(java.math.BigDecimal.valueOf(100));
            food.setReferencePortionGrams(java.math.BigDecimal.valueOf(100));
            food.setActive(true);
            food.setSuitableMeals("LUNCH,DINNER");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.LOW_CARB));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ONIVORA não aplica filtro de carboidrato — alimento com 50g carbs é incluído")
        void lowCarb_doesNotAffectOnivoraFilter() {
            Food food = createFood(1, "Arroz", false, false, false, true, true, 50.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.ONIVORA));

            assertThat(result).containsExactly(food);
        }

        @Test
        @DisplayName("LOW_CARB preserva filtro de alergia — alimento com lactose é excluído mesmo com carbs ok")
        void lowCarb_preservesAllergyFilter() {
            Food food = createFood(1, "Queijo", true, false, false, true, false, 3.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            Assessment assessment = new Assessment();
            assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
            assessment.setDietType(Assessment.DietType.LOW_CARB);
            assessment.setAllergies("lactose");

            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filtro FLEXITARIANA (Task 2.8D)")
    class FlexitarianFilter {

        @Test
        @DisplayName("FLEXITARIANA não exclui carnes — carne e vegetal permanecem elegíveis")
        void flexitariana_doesNotExcludeMeat() {
            Food carne = createFood(1, "Carne", false, false, false, false, false, 0);
            Food tofu = createFood(2, "Tofu", false, false, false, true, true, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(carne, tofu));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.FLEXITARIANA));

            assertThat(result).containsExactlyInAnyOrder(carne, tofu);
        }

        @Test
        @DisplayName("FLEXITARIANA não ativa filtro vegetariano/vegano/pescatariano — peixe e carne terrestre passam")
        void flexitariana_doesNotActivateDietFilters() {
            Food peixe = createFood(1, "Salmão", false, false, false, false, false, 0);
            peixe.setSubcategory("PEIXES_E_FRUTOS_DO_MAR");
            Food porco = createFood(2, "Pernil", false, false, false, false, false, 0);
            porco.setSubcategory("CARNES_SUINAS");
            Food queijo = createFood(3, "Queijo", true, false, false, true, false, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(peixe, porco, queijo));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.FLEXITARIANA));

            assertThat(result).containsExactlyInAnyOrder(peixe, porco, queijo);
        }

        @Test
        @DisplayName("FLEXITARIANA preserva filtro de alergia — alimento com lactose é excluído")
        void flexitariana_preservesAllergyFilter() {
            Food queijo = createFood(1, "Queijo", true, false, false, true, false, 5);
            Food carne = createFood(2, "Carne", false, false, false, false, false, 0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(queijo, carne));

            Assessment assessment = createAssessmentWithDietType(Assessment.DietType.FLEXITARIANA);
            assessment.setAllergies("lactose");

            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("FLEXITARIANA preserva filtro de diabetes — carboidrato alto é excluído")
        void flexitariana_preservesDiabetesFilter() {
            Food acucar = createFood(1, "Açúcar", false, false, false, true, true, 99);
            Food carne = createFood(2, "Carne", false, false, false, false, false, 0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(acucar, carne));

            Assessment assessment = createAssessmentWithDietType(Assessment.DietType.FLEXITARIANA);
            assessment.setHealthConditions("diabetes");

            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("FLEXITARIANA preserva bloqueio de categoria por condição de saúde — bebida low-carb é removida via blockedCategories")
        void flexitariana_preservesHealthConditionBlockedCategory() {
            // given — bebida (BEVERAGE) com carbs baixos (5g): passa na regra de carboidrato do
            // diabetes (limite 60g), então só pode ser removida pelo bloqueio de categoria
            // (blockedCategories: diabetes -> BEVERAGE). Isso isola o mecanismo de categoria da
            // regra de carboidrato. Diabetes é a única condição de saúde mapeada para
            // blockedCategories no sistema atual (FoodFilterService.HEALTH_CONDITION_BLOCKED_CATEGORIES).
            Food bebida = createFood(1, "Refrigerante zero", false, false, false, true, true, 5);
            bebida.setCategory("BEVERAGE");
            Food permitido = createFood(2, "Carne", false, false, false, false, false, 0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(bebida, permitido));

            Assessment assessment = createAssessmentWithDietType(Assessment.DietType.FLEXITARIANA);
            assessment.setHealthConditions("diabetes");

            // when
            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            // then — bebida removida pela categoria bloqueada; alimento permitido permanece
            assertThat(result).containsExactly(permitido);
        }

        @Test
        @DisplayName("FLEXITARIANA não aplica filtro low carb — alimento com 50g carbs permanece")
        void flexitariana_doesNotApplyLowCarbFilter() {
            Food arroz = createFood(1, "Arroz", false, false, false, true, true, 50.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(arroz));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.FLEXITARIANA));

            assertThat(result).containsExactly(arroz);
        }
    }

    @Nested
    @DisplayName("Filtro CETOGENICA (Task 2.8E)")
    class KetoFilter {

        private Food createKetoFood(int id, String name, Double carbs100g, Double fat100g) {
            Food food = new Food();
            food.setId(id);
            food.setName(name);
            food.setCategory("PROTEIN");
            food.setContainsLactose(false);
            food.setContainsGluten(false);
            food.setContainsEgg(false);
            food.setVegetarian(false);
            food.setVegan(false);
            food.setCarbsPer100g(carbs100g == null ? null : BigDecimal.valueOf(carbs100g));
            food.setFatPer100g(fat100g == null ? null : BigDecimal.valueOf(fat100g));
            food.setCaloriesPer100g(BigDecimal.valueOf(200));
            food.setReferencePortionGrams(BigDecimal.valueOf(100));
            food.setActive(true);
            food.setSuitableMeals("LUNCH,DINNER");
            return food;
        }

        @Test
        @DisplayName("Exclui alimento com carbsPer100g = 11 (acima do teto keto de 10)")
        void keto_excludesFoodWithCarbsAboveThreshold() {
            Food food = createKetoFood(1, "Castanha doce", 11.0, 50.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CETOGENICA));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Exclui alimento com fatPer100g = 14.9 (abaixo do piso keto de 15)")
        void keto_excludesFoodWithFatBelowThreshold() {
            Food food = createKetoFood(1, "Peito de frango", 0.0, 14.9);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CETOGENICA));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Exclui alimento com carbsPer100g null")
        void keto_excludesFoodWithNullCarbs() {
            Food food = createKetoFood(1, "Sem dado carb", null, 30.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CETOGENICA));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Exclui alimento com fatPer100g null")
        void keto_excludesFoodWithNullFat() {
            Food food = createKetoFood(1, "Sem dado fat", 2.0, null);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CETOGENICA));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Mantém alimento com carbs = 10 e fat = 15 (exatamente nos limites)")
        void keto_keepsFoodAtExactThresholds() {
            Food food = createKetoFood(1, "Abacate", 10.0, 15.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CETOGENICA));

            assertThat(result).containsExactly(food);
        }

        @Test
        @DisplayName("CETOGENICA preserva filtro de alergia — alimento keto com lactose é excluído")
        void keto_preservesAllergyFilter() {
            Food food = createKetoFood(1, "Queijo gordo", 2.0, 30.0);
            food.setContainsLactose(true);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            Assessment assessment = createAssessmentWithDietType(Assessment.DietType.CETOGENICA);
            assessment.setAllergies("lactose");

            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("LOW_CARB continua usando teto <= 20 e não exige gordura — alimento carb=15 fat=0 permanece")
        void lowCarb_doesNotRequireFatFloor() {
            Food food = createKetoFood(1, "Vegetal low carb", 15.0, 0.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.LOW_CARB));

            assertThat(result).containsExactly(food);
        }

        @Test
        @DisplayName("ONIVORA não regride — alimento alto carb e baixa gordura permanece elegível")
        void onivora_notAffectedByKetoFilter() {
            Food food = createKetoFood(1, "Arroz", 50.0, 1.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(food));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.ONIVORA));

            assertThat(result).containsExactly(food);
        }
    }

    @Nested
    @DisplayName("Filtro CARNIVORA (Task 2.8F)")
    class CarnivoraFilter {

        private Food animalFood(int id, String name, String category, String subcategory) {
            Food food = createFood(id, name, false, false, false, false, false, 0);
            food.setCategory(category);
            food.setSubcategory(subcategory);
            return food;
        }

        @Test
        @DisplayName("Mantém carne bovina, suína, ave e peixe elegíveis")
        void carnivora_keepsMeatPoultryFish() {
            Food bovina = animalFood(1, "Picanha", "PROTEIN", "CARNES_BOVINAS");
            Food suina = animalFood(2, "Pernil", "PROTEIN", "CARNES_SUINAS");
            Food ave = animalFood(3, "Frango", "PROTEIN", "AVES");
            Food peixe = animalFood(4, "Salmão", "PROTEIN", "PEIXES_E_FRUTOS_DO_MAR");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(bovina, suina, ave, peixe));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactlyInAnyOrder(bovina, suina, ave, peixe);
        }

        @Test
        @DisplayName("Mantém ovos/laticínios elegíveis (LATICINIOS_E_OVOS e categoria DAIRY)")
        void carnivora_keepsEggsAndDairy() {
            Food ovos = animalFood(1, "Ovo", "PROTEIN", "LATICINIOS_E_OVOS");
            Food laticinio = animalFood(2, "Iogurte", "DAIRY", null);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(ovos, laticinio));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactlyInAnyOrder(ovos, laticinio);
        }

        @Test
        @DisplayName("Remove vegetais")
        void carnivora_removesVegetables() {
            Food vegetal = animalFood(1, "Brócolis", "VEGETABLE", null);
            Food carne = animalFood(2, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(vegetal, carne));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("Remove frutas")
        void carnivora_removesFruits() {
            Food fruta = animalFood(1, "Banana", "FRUIT", null);
            Food carne = animalFood(2, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(fruta, carne));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("Remove grãos/carboidratos vegetais")
        void carnivora_removesGrains() {
            Food arroz = animalFood(1, "Arroz", "CARBOHYDRATE", "GRAOS_E_MASSAS");
            Food carne = animalFood(2, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(arroz, carne));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("Remove leguminosas")
        void carnivora_removesLegumes() {
            Food feijao = animalFood(1, "Feijão", "LEGUME", null);
            Food carne = animalFood(2, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(feijao, carne));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("Remove embutidos (PROTEIN industrializado) e óleos/gorduras de origem mista")
        void carnivora_removesProcessedMeatAndMixedFats() {
            Food embutido = animalFood(1, "Salame", "PROTEIN", "EMBUTIDOS");
            Food oleo = animalFood(2, "Azeite", "PROTEIN", "GORDURAS_E_OLEOS");
            Food carne = animalFood(3, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(embutido, oleo, carne));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.CARNIVORA));

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("Preserva filtro de alergia — laticínio com lactose é excluído mesmo sendo animal")
        void carnivora_preservesAllergyFilter() {
            Food laticinio = createFood(1, "Queijo", true, false, false, false, false, 0);
            laticinio.setCategory("DAIRY");
            Food carne = animalFood(2, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(laticinio, carne));

            Assessment assessment = createAssessmentWithDietType(Assessment.DietType.CARNIVORA);
            assessment.setAllergies("lactose");

            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("Preserva filtro de diabetes — bloqueio de categoria continua ativo")
        void carnivora_preservesDiabetesFilter() {
            Food bebida = animalFood(1, "Bebida láctea", "BEVERAGE", null);
            Food carne = animalFood(2, "Carne", "PROTEIN", "CARNES_BOVINAS");
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(bebida, carne));

            Assessment assessment = createAssessmentWithDietType(Assessment.DietType.CARNIVORA);
            assessment.setHealthConditions("diabetes");

            List<Food> result = foodFilterService.findEligibleFoods(assessment);

            assertThat(result).containsExactly(carne);
        }

        @Test
        @DisplayName("ONIVORA não regride — carne e vegetal permanecem elegíveis")
        void carnivora_doesNotAffectOnivora() {
            Food carne = animalFood(1, "Carne", "PROTEIN", "CARNES_BOVINAS");
            Food vegetal = animalFood(2, "Brócolis", "VEGETABLE", null);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(carne, vegetal));

            List<Food> result = foodFilterService.findEligibleFoods(
                    createAssessmentWithDietType(Assessment.DietType.ONIVORA));

            assertThat(result).containsExactlyInAnyOrder(carne, vegetal);
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
        @DisplayName("Sempre retorna 5 refeições mesmo sem alimentos")
        void sempreRetornaCincoRefeicoes() {
            Map<String, List<Food>> grouped = foodFilterService.groupByMealType(List.of());

            assertThat(grouped).hasSize(5);
            assertThat(grouped).containsKeys("BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }
    }
}
