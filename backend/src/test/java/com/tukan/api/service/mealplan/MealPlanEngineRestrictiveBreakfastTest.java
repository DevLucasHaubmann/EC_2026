package com.tukan.api.service.mealplan;

import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealOption;
import com.tukan.api.dto.mealplan.MealPlanFoodItem;
import com.tukan.api.dto.mealplan.MealPlanMeal;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.entity.FoodRole;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.FoodRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Teste integrado do café da manhã em dietas restritivas (Task 2.8H.1 — Blocker 1 do QA).
 *
 * <p>O slot padrão de café ({@code BREAKFAST_LIGHT}) só aceita papéis breakfast-eligible
 * ({@code BREAKFAST_LIGHT}, {@code FRUIT}, {@code LIGHT_DAIRY}, {@code HEALTHY_SNACK}), todos
 * removidos pelos filtros keto/carnívora — deixando o slot sem candidatos e abortando a geração.
 * Estes testes exercitam o pipeline real ({@link MealPlanEngine} + filtro + agrupamento +
 * composição + selector) e provam que as composições específicas {@code BREAKFAST_CETOGENICA}
 * e {@code BREAKFAST_CARNIVORA} eliminam a falha, aceitando {@code MAIN_PROTEIN}/{@code HEALTHY_FAT}
 * (keto) e {@code MAIN_PROTEIN}/{@code LIGHT_DAIRY} (carnívora).
 */
@DisplayName("MealPlanEngine — café da manhã CETOGENICA/CARNIVORA (Task 2.8H.1)")
class MealPlanEngineRestrictiveBreakfastTest {

    private MealPlanEngine engine;
    private FoodRepository foodRepository;
    private UserService userService;
    private NutritionalProfileRepository profileRepository;
    private AssessmentRepository assessmentRepository;
    private CalorieCalculator calorieCalculator;

    private User user;
    private NutritionalProfile profile;

    @BeforeEach
    void setUp() {
        foodRepository = mock(FoodRepository.class);
        userService = mock(UserService.class);
        profileRepository = mock(NutritionalProfileRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        calorieCalculator = mock(CalorieCalculator.class);

        // Pipeline real: somente as fronteiras externas são mockadas.
        engine = new MealPlanEngine(
                userService,
                profileRepository,
                assessmentRepository,
                calorieCalculator,
                new FoodFilterService(foodRepository),
                new FoodCurationService(),
                new MealSuitabilityService(),
                new MealDistributor(),
                new FoodSelector());

        user = new User();
        user.setId(1);
        user.setEmail("restritiva@test.com");

        profile = new NutritionalProfile();
        profile.setGender(NutritionalProfile.Gender.MALE);
        profile.setWeightKg(80.0);
        profile.setHeightCm(180.0);
        profile.setActivityLevel(NutritionalProfile.ActivityLevel.MODERATE);
        profile.setDateOfBirth(LocalDate.now().minusYears(30));
    }

    // =========================================================================
    // CETOGENICA
    // =========================================================================

    @Test
    @DisplayName("CETOGENICA: gera café da manhã válido sem BREAKFAST_LIGHT e sem carboidrato")
    void cetogenica_generatesValidKetoBreakfast() {
        // given — café keto: ovos (MAIN_PROTEIN) e abacate (HEALTHY_FAT); pão é removido pelo filtro keto
        Set<String> ketoBreakfast = Set.of("Ovos Mexidos", "Abacate");
        stubUser(Assessment.DietType.CETOGENICA);
        when(foodRepository.findByActiveTrue()).thenReturn(ketoPool());

        // when — fluxo real ponta a ponta
        DailyMealPlan plan = engine.generatePlan("restritiva@test.com");

        // then — plano completo, nenhuma opção vazia
        assertNoEmptyOptions(plan);

        // pão (BREAKFAST_LIGHT, carb alto) não vaza em nenhuma refeição
        assertThat(allItemNames(plan)).doesNotContain("Pao Frances");

        // café usa apenas papéis keto-compatíveis (proteína/gordura), nunca o item leve
        for (MealOption option : mealByType(plan, "BREAKFAST").options()) {
            assertThat(itemNames(option)).isSubsetOf(ketoBreakfast);
        }
    }

    @Test
    @DisplayName("CETOGENICA: pool de café sem papel keto-compatível falha claramente, sem plano parcial")
    void cetogenica_insufficientBreakfastPoolFailsClearly() {
        // given — único alimento de café passa no filtro keto mas tem papel COMPLEMENT (fora do slot)
        stubUser(Assessment.DietType.CETOGENICA);
        when(foodRepository.findByActiveTrue()).thenReturn(ketoPoolWithUnusableBreakfast());

        // when / then — falha de negócio clara, nenhum plano retornado
        assertThatThrownBy(() -> engine.generatePlan("restritiva@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BREAKFAST_CETOGENICA")
                .hasMessageContaining("keto breakfast item");
    }

    // =========================================================================
    // CARNIVORA
    // =========================================================================

    @Test
    @DisplayName("CARNIVORA: gera café da manhã válido apenas com MAIN_PROTEIN animal (cenário real do blocker)")
    void carnivora_generatesValidBreakfastFromProteinOnly() {
        // given — café carnívoro só tem ovos (MAIN_PROTEIN); fruta é removida pelo filtro carnívoro
        stubUser(Assessment.DietType.CARNIVORA);
        when(foodRepository.findByActiveTrue()).thenReturn(carnivoraProteinOnlyPool());

        // when
        DailyMealPlan plan = engine.generatePlan("restritiva@test.com");

        // then — café não fica vazio; o slot padrão teria rejeitado MAIN_PROTEIN
        assertNoEmptyOptions(plan);
        assertThat(allItemNames(plan)).doesNotContain("Banana");
        for (MealOption option : mealByType(plan, "BREAKFAST").options()) {
            assertThat(itemNames(option)).containsOnly("Ovos Cozidos");
        }
    }

    // =========================================================================
    // Helpers de asserção
    // =========================================================================

    private void assertNoEmptyOptions(DailyMealPlan plan) {
        assertThat(plan.meals()).isNotEmpty();
        for (MealPlanMeal meal : plan.meals()) {
            assertThat(meal.options()).hasSize(2);
            for (MealOption option : meal.options()) {
                assertThat(option.items()).isNotEmpty();
            }
        }
    }

    private MealPlanMeal mealByType(DailyMealPlan plan, String mealType) {
        return plan.meals().stream()
                .filter(m -> m.mealType().equals(mealType))
                .findFirst().orElseThrow();
    }

    private Set<String> itemNames(MealOption option) {
        return option.items().stream()
                .map(MealPlanFoodItem::name)
                .collect(Collectors.toSet());
    }

    private Set<String> allItemNames(DailyMealPlan plan) {
        return plan.meals().stream()
                .flatMap(m -> m.options().stream())
                .flatMap(o -> o.items().stream())
                .map(MealPlanFoodItem::name)
                .collect(Collectors.toSet());
    }

    // =========================================================================
    // Stubs e pools
    // =========================================================================

    private void stubUser(Assessment.DietType dietType) {
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        assessment.setDietType(dietType);
        assessment.setMealsPerDay(3);
        when(userService.findByEmail("restritiva@test.com")).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
        when(calorieCalculator.calculateDailyCalorieTarget(any(), any(), anyInt())).thenReturn(2000.0);
    }

    /** Pool keto: café (ovos + abacate), almoço/jantar (proteína + gordura) e um intruso carb (pão). */
    private List<Food> ketoPool() {
        return List.of(
                // café keto
                food(1, "Ovos Mexidos", "PROTEIN", "LATICINIOS_E_OVOS", FoodRole.MAIN_PROTEIN, 150, 1, 20, "BREAKFAST"),
                food(2, "Abacate", "HEALTHY_FAT", "OLEAGINOSAS", FoodRole.HEALTHY_FAT, 160, 8, 15, "BREAKFAST"),
                // almoço/jantar keto (MAIN_PROTEIN + HEALTHY_FAT são estruturais)
                food(3, "Picanha", "PROTEIN", "CARNES_BOVINAS", FoodRole.MAIN_PROTEIN, 280, 0, 25, "LUNCH,DINNER"),
                food(4, "Azeite de Oliva", "HEALTHY_FAT", "GORDURAS_E_OLEOS", FoodRole.HEALTHY_FAT, 200, 0, 30, "LUNCH,DINNER"),
                // intruso: alto carboidrato — removido pelo filtro keto antes da seleção
                food(5, "Pao Frances", "CARBOHYDRATE", "CEREAIS_E_GRAOS", FoodRole.BREAKFAST_LIGHT, 270, 50, 3, "BREAKFAST"));
    }

    /**
     * Pool keto cujo único alimento de café passa no filtro (carbs ≤ 10, fat ≥ 15) mas tem papel
     * COMPLEMENT, fora do slot {@code keto breakfast item}. Almoço/jantar permanecem viáveis, de modo
     * que a falha venha da validação de slot do café — não da cobertura de refeição.
     */
    private List<Food> ketoPoolWithUnusableBreakfast() {
        return List.of(
                food(1, "Creme Keto", "BEVERAGE", "OUTROS", FoodRole.COMPLEMENT, 90, 2, 80, "BREAKFAST"),
                food(3, "Picanha", "PROTEIN", "CARNES_BOVINAS", FoodRole.MAIN_PROTEIN, 280, 0, 25, "LUNCH,DINNER"),
                food(4, "Azeite de Oliva", "HEALTHY_FAT", "GORDURAS_E_OLEOS", FoodRole.HEALTHY_FAT, 200, 0, 30, "LUNCH,DINNER"));
    }

    /** Pool carnívoro: café só com ovos (MAIN_PROTEIN), proteínas animais e um intruso fruta. */
    private List<Food> carnivoraProteinOnlyPool() {
        return List.of(
                food(1, "Ovos Cozidos", "PROTEIN", "LATICINIOS_E_OVOS", FoodRole.MAIN_PROTEIN, 150, 1, 11, "BREAKFAST"),
                food(2, "Carne Bovina", "PROTEIN", "CARNES_BOVINAS", FoodRole.MAIN_PROTEIN, 250, 0, 15, "LUNCH,DINNER"),
                food(3, "Frango Grelhado", "PROTEIN", "AVES", FoodRole.MAIN_PROTEIN, 165, 0, 8, "LUNCH,DINNER"),
                // intruso: fruta — removida pelo filtro carnívoro
                food(4, "Banana", "FRUIT", "FRUTAS", FoodRole.FRUIT, 90, 22, 0, "BREAKFAST"));
    }

    private Food food(int id, String name, String category, String subcategory, FoodRole role,
                      double caloriesPer100g, double carbsPer100g, double fatPer100g, String suitableMeals) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory(category);
        f.setSubcategory(subcategory);
        f.setFoodRole(role);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(15));
        f.setCarbsPer100g(BigDecimal.valueOf(carbsPer100g));
        f.setFatPer100g(BigDecimal.valueOf(fatPer100g));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        // Portion sized so a single MAIN_PROTEIN clears the lunch calorie floor (0.60 × 800 = 480 kcal):
        // the leanest protein here (Frango/Ovos, 150-165 kcal/100g) yields ~525-577 kcal at 350 g.
        f.setReferencePortionGrams(BigDecimal.valueOf(350));
        f.setSuitableMeals(suitableMeals);
        f.setActive(true);
        return f;
    }
}
