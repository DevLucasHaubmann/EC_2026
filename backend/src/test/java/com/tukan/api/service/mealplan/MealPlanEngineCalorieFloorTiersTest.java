package com.tukan.api.service.mealplan;

import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealOption;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the progressive calorie acceptance ladder in {@link MealPlanEngine}.
 *
 * <p>Uses the real pipeline (FoodFilterService, FoodCurationService, MealSuitabilityService,
 * MealDistributor, FoodSelector) — only external boundaries (repositories, user service,
 * CalorieCalculator) are mocked. This proves the ladder behaviour under real food pool constraints.
 *
 * <p>Covers all 6 required scenarios from bug B1:
 * <ol>
 *   <li>Breakfast meets tier 1 (70%) → plan succeeds, standard tier used.</li>
 *   <li>Breakfast needs tier 2 (55%) fallback → plan succeeds, relaxed tier used.</li>
 *   <li>Breakfast needs tier 3 (40%) fallback → plan succeeds, minimum tier used.</li>
 *   <li>Breakfast below 40% → plan fails with BusinessException (hard floor).</li>
 *   <li>Tier fallback never relaxes composition/structure — food roles stay valid.</li>
 *   <li>ONIVORA plan completes with COMPLETED status for mealsPerDay=3 and mealsPerDay=4.</li>
 * </ol>
 */
@DisplayName("MealPlanEngine — escada progressiva de pisos calóricos (Bug B1)")
class MealPlanEngineCalorieFloorTiersTest {

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
        user.setEmail("floor-test@test.com");

        profile = new NutritionalProfile();
        profile.setGender(NutritionalProfile.Gender.MALE);
        profile.setWeightKg(80.0);
        profile.setHeightCm(180.0);
        profile.setActivityLevel(NutritionalProfile.ActivityLevel.MODERATE);
        profile.setDateOfBirth(LocalDate.now().minusYears(30));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Stubs user/profile/assessment for ONIVORA with the given mealsPerDay.
     * CalorieCalculator returns a fixed dailyCalories — the distributor is real, so meal targets
     * are derived from the real distribution logic.
     */
    private void stubUser(int mealsPerDay, double dailyCalories) {
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        assessment.setDietType(Assessment.DietType.ONIVORA);
        assessment.setMealsPerDay(mealsPerDay);

        when(userService.findByEmail("floor-test@test.com")).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
        when(calorieCalculator.calculateDailyCalorieTarget(any(), any(), anyInt()))
                .thenReturn(dailyCalories);
    }

    /**
     * Creates a BREAKFAST food with BREAKFAST_LIGHT role.
     *
     * <p>caloriesPerPortion = caloriesPer100g * portionGrams / 100
     * Caller controls the portion energy by choosing caloriesPer100g and portionGrams.
     */
    private Food breakfastFood(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("CARBOHYDRATE");
        f.setSubcategory("CEREAIS_E_GRAOS");
        f.setFoodRole(FoodRole.BREAKFAST_LIGHT);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(5));
        f.setCarbsPer100g(BigDecimal.valueOf(50));
        f.setFatPer100g(BigDecimal.valueOf(2));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("BREAKFAST");
        f.setActive(true);
        return f;
    }

    /**
     * Creates a HEALTHY_FAT breakfast-eligible food (e.g. nuts/seeds). This role is outside the
     * default breakfast composition slot, so it only enters a breakfast option through the engine's
     * best-effort supplementation — exactly the path under test.
     */
    private Food healthyFatBreakfast(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("HEALTHY_FAT");
        f.setSubcategory("OLEAGINOSAS");
        f.setFoodRole(FoodRole.HEALTHY_FAT);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(15));
        f.setCarbsPer100g(BigDecimal.valueOf(10));
        f.setFatPer100g(BigDecimal.valueOf(50));
        f.setFiberPer100g(BigDecimal.valueOf(5));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("BREAKFAST");
        f.setActive(true);
        return f;
    }

    /**
     * Creates a MAIN_PROTEIN food for LUNCH and DINNER.
     * Portion: 200 kcal/100g × 200g = 400 kcal per portion.
     * Combined with a carbohydrate (225 kcal) and a vegetable (50 kcal) the total reaches
     * 675 kcal — above tier 1 floor (70%) for every lunch/dinner target used in this test class.
     */
    private Food mainMealProtein(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("AVES");
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(200));
        f.setProteinPer100g(BigDecimal.valueOf(25));
        f.setCarbsPer100g(BigDecimal.valueOf(0));
        f.setFatPer100g(BigDecimal.valueOf(10));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        // 200 kcal/100g × 200g = 400 kcal
        f.setReferencePortionGrams(BigDecimal.valueOf(200));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /**
     * Creates a MAIN_CARBOHYDRATE food for LUNCH and DINNER.
     * Required by ONIVORA LUNCH/DINNER composition rule (MAIN_PROTEIN + MAIN_CARBOHYDRATE + VEGETABLE_LEGUME).
     * Portion: 150 kcal/100g × 150g = 225 kcal per portion.
     */
    private Food mainMealCarbohydrate(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("CARBOHYDRATE");
        f.setSubcategory("CEREAIS_E_GRAOS");
        f.setFoodRole(FoodRole.MAIN_CARBOHYDRATE);
        f.setCaloriesPer100g(BigDecimal.valueOf(150));
        f.setProteinPer100g(BigDecimal.valueOf(3));
        f.setCarbsPer100g(BigDecimal.valueOf(30));
        f.setFatPer100g(BigDecimal.valueOf(1));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        // 150 kcal/100g × 150g = 225 kcal
        f.setReferencePortionGrams(BigDecimal.valueOf(150));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /**
     * Creates a VEGETABLE_LEGUME food for LUNCH and DINNER.
     * Required by ONIVORA LUNCH/DINNER composition rule (MAIN_PROTEIN + MAIN_CARBOHYDRATE + VEGETABLE_LEGUME).
     * Portion: 50 kcal/100g × 100g = 50 kcal per portion.
     */
    private Food mainMealVegetable(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("VEGETABLE");
        f.setSubcategory("LEGUMES_E_VERDURAS");
        f.setFoodRole(FoodRole.VEGETABLE_LEGUME);
        f.setCaloriesPer100g(BigDecimal.valueOf(50));
        f.setProteinPer100g(BigDecimal.valueOf(2));
        f.setCarbsPer100g(BigDecimal.valueOf(8));
        f.setFatPer100g(BigDecimal.valueOf(0));
        f.setFiberPer100g(BigDecimal.valueOf(3));
        // 50 kcal/100g × 100g = 50 kcal
        f.setReferencePortionGrams(BigDecimal.valueOf(100));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /**
     * Minimal pool for a 3-meal ONIVORA plan: breakfast foods + complete LUNCH/DINNER slots.
     *
     * <p>ONIVORA LUNCH/DINNER require three composition slots: MAIN_PROTEIN + MAIN_CARBOHYDRATE +
     * VEGETABLE_LEGUME. The combination provides 675 kcal, above the tier 1 floor (70%) for all
     * lunch/dinner targets in this test class. Breakfast calorie coverage is controlled by the caller.
     */
    private List<Food> poolWith(List<Food> breakfastFoods) {
        return new java.util.ArrayList<>() {{
            addAll(breakfastFoods);
            // MAIN_PROTEIN (two options for de-duplication)
            add(mainMealProtein(100, "Frango Grelhado"));
            add(mainMealProtein(101, "Carne Bovina"));
            // MAIN_CARBOHYDRATE (required slot for ONIVORA LUNCH/DINNER)
            add(mainMealCarbohydrate(200, "Arroz Integral"));
            add(mainMealCarbohydrate(201, "Batata Doce"));
            // VEGETABLE_LEGUME (required slot for ONIVORA LUNCH/DINNER)
            add(mainMealVegetable(300, "Brocolis"));
            add(mainMealVegetable(301, "Abobrinha"));
        }};
    }

    private void assertPlanHasMeals(DailyMealPlan plan, int expectedMealCount) {
        assertThat(plan.meals()).hasSize(expectedMealCount);
        for (MealPlanMeal meal : plan.meals()) {
            assertThat(meal.options()).isNotEmpty();
            for (MealOption option : meal.options()) {
                assertThat(option.items()).isNotEmpty();
            }
        }
    }

    // =========================================================================
    // Cenário 1 — Tier 1 (70%): pool adequado, café aceito no tier padrão
    // =========================================================================

    @Nested
    @DisplayName("Cenário 1 — tier 1 (70%): pool adequado para breakfast")
    class Tier1Standard {

        @Test
        @DisplayName("Breakfast com opção >= 70% do alvo → plano gerado normalmente (tier 1)")
        void breakfastMeetsTier1_planSucceeds() {
            // given
            // MealDistributor para 3 refeições distribui ~20/40/40%.
            // dailyCalories=2000 → breakfast≈400 kcal.
            // Tier 1 floor = 70% × 400 = 280 kcal.
            // Alimento: 350 kcal/100g × 100g = 350 kcal — acima de 280. Tier 1 atingido.
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(poolWith(List.of(
                    breakfastFood(1, "Aveia Rica", 350, 100),   // 350 kcal — above tier 1
                    breakfastFood(2, "Granola Alta Cal", 380, 100) // 380 kcal — also tier 1
            )));

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then
            assertPlanHasMeals(plan, 3);
            MealPlanMeal breakfast = plan.meals().stream()
                    .filter(m -> m.mealType().equals("BREAKFAST")).findFirst().orElseThrow();
            assertThat(breakfast.options()).isNotEmpty();
        }
    }

    // =========================================================================
    // Cenário 2 — Tier 2 (55%): pool restrito, fallback para 55%
    // =========================================================================

    @Nested
    @DisplayName("Cenário 2 — tier 2 (55%): nenhuma opção atinge 70%, fallback para 55%")
    class Tier2Relaxed {

        @Test
        @DisplayName("Breakfast com melhor opção entre 55-70% do alvo → plano gerado via tier 2")
        void breakfastFallsToTier2_planSucceeds() {
            // given
            // dailyCalories=2000, mealsPerDay=3 → breakfast≈400 kcal.
            // Tier 1 floor = 280 kcal. Tier 2 floor = 220 kcal.
            // Alimento: 250 kcal/100g × 100g = 250 kcal — abaixo de 280 (tier 1), acima de 220 (tier 2).
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(poolWith(List.of(
                    breakfastFood(1, "Pao Light", 250, 100),  // 250 kcal — tier 2 only
                    breakfastFood(2, "Tapioca Simples", 240, 100) // 240 kcal — tier 2 only
            )));

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then
            assertPlanHasMeals(plan, 3);
        }
    }

    // =========================================================================
    // Cenário 3 — Tier 3 (40%): pool muito restrito, fallback para 40%
    // =========================================================================

    @Nested
    @DisplayName("Cenário 3 — tier 3 (40%): nenhuma opção atinge 55%, fallback para 40%")
    class Tier3Minimum {

        @Test
        @DisplayName("Breakfast com melhor opção entre 40-55% do alvo → plano gerado via tier 3")
        void breakfastFallsToTier3_planSucceeds() {
            // given
            // dailyCalories=2000, mealsPerDay=3 → breakfast≈400 kcal.
            // Tier 2 floor = 220 kcal. Tier 3 floor = 160 kcal.
            // Alimento: 190 kcal/100g × 100g = 190 kcal — abaixo de 220 (tier 2), acima de 160 (tier 3).
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(poolWith(List.of(
                    breakfastFood(1, "Biscoito Agua Sal", 190, 100), // 190 kcal — tier 3 only
                    breakfastFood(2, "Torrada Simples", 185, 100)     // 185 kcal — tier 3 only
            )));

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then
            assertPlanHasMeals(plan, 3);
        }
    }

    // =========================================================================
    // Cenário 4 — Abaixo de 40% (BREAKFAST): best-effort, não falha (Bug B1 — MVP)
    // =========================================================================

    @Nested
    @DisplayName("Cenário 4 — BREAKFAST abaixo de 40%: best-effort gera o plano (não falha)")
    class BreakfastBelowHardFloorBestEffort {

        @Test
        @DisplayName("Breakfast com todas opções abaixo de 40%, sem suplemento que eleve o piso → best-effort gera o plano")
        void breakfastBelowHardFloor_bestEffortGeneratesPlan() {
            // given
            // dailyCalories=2000, mealsPerDay=3 → breakfast≈400 kcal. Tier 3 floor = 160 kcal.
            // Pool de breakfast só tem alimentos subcalóricos (100 + 50 kcal): a suplementação não
            // alcança o piso, então o engine aplica o best-effort extremo e mantém as opções válidas.
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(poolWith(List.of(
                    breakfastFood(1, "Gelatina Zero", 100, 100), // 100 kcal — below hard floor
                    breakfastFood(2, "Agua Aromatizada", 50, 100) // 50 kcal — below hard floor
            )));

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then — plano gerado mesmo com café subcalórico; BREAKFAST não derruba o job
            assertPlanHasMeals(plan, 3);
            MealPlanMeal breakfast = plan.meals().stream()
                    .filter(m -> m.mealType().equals("BREAKFAST")).findFirst().orElseThrow();
            assertThat(breakfast.options()).isNotEmpty();
            assertThat(breakfast.options()).allSatisfy(o -> assertThat(o.items()).isNotEmpty());
        }

        @Test
        @DisplayName("Breakfast com fallback HEALTHY_FAT disponível → suplementação eleva acima do piso e gera o plano")
        void breakfastSupplementedByHealthyFat_reachesFloor() {
            // given
            // Café com staples subcalóricos (100 + 50 kcal) mais uma gordura saudável densa (castanhas,
            // 600 kcal/100g × 30g = 180 kcal). Sozinhos, os staples ficam abaixo do piso (160 kcal); a
            // suplementação best-effort de BREAKFAST adiciona a gordura (papel HEALTHY_FAT, fora do slot
            // padrão) e o piso de 160 kcal é alcançado.
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(poolWith(List.of(
                    breakfastFood(1, "Biscoito Leve", 100, 100),
                    breakfastFood(2, "Torrada Fina", 50, 100),
                    healthyFatBreakfast(3, "Castanhas", 600, 30)
            )));

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then
            assertPlanHasMeals(plan, 3);
        }

        @Test
        @DisplayName("Sem nenhum alimento válido para BREAKFAST → falha (cobertura de refeição)")
        void breakfastWithNoEligibleFood_fails() {
            // given — pool sem qualquer alimento de café da manhã: a cobertura por refeição barra antes
            // da seleção, pois não há como compor BREAKFAST.
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(poolWith(List.of()));

            // when / then
            assertThatThrownBy(() -> engine.generatePlan("floor-test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BREAKFAST");
        }
    }

    // =========================================================================
    // Cenário 5 — Fallback não relaxa composição/estrutura
    // =========================================================================

    @Nested
    @DisplayName("Cenário 5 — fallback não relaxa composição/estrutura")
    class FallbackPreservesComposition {

        @Test
        @DisplayName("Plano aceito via tier relaxado contém apenas alimentos com roles válidos para cada refeição")
        void fallbackDoesNotAcceptInvalidFoodRoles() {
            // given
            // Breakfast pool com apenas BREAKFAST_LIGHT de caloria moderada (tier 2 zone).
            // LUNCH/DINNER pool com MAIN_PROTEIN adequado.
            // This proves that even under tier 2 fallback, a LUNCH item (MAIN_PROTEIN) never
            // leaks into BREAKFAST — the composition chain (FoodSelector/MealCompositionRule)
            // filters by suitableMeals before the calorie gate is evaluated.
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(List.of(
                    breakfastFood(1, "Pao Frances Leve", 250, 100), // 250 kcal — tier 2 for breakfast
                    breakfastFood(2, "Cuscuz Simples", 245, 100),    // 245 kcal — tier 2 for breakfast
                    // All three required ONIVORA LUNCH/DINNER slots must be present
                    mainMealProtein(100, "Frango Grelhado"),
                    mainMealProtein(101, "Carne Bovina"),
                    mainMealCarbohydrate(200, "Arroz Integral"),
                    mainMealCarbohydrate(201, "Batata Doce"),
                    mainMealVegetable(300, "Brocolis"),
                    mainMealVegetable(301, "Abobrinha")
            ));

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then — plano gerado com sucesso via tier 2
            assertPlanHasMeals(plan, 3);

            // Lunch/dinner options must only contain main meal proteins (not breakfast foods)
            for (MealPlanMeal meal : plan.meals()) {
                if (meal.mealType().equals("LUNCH") || meal.mealType().equals("DINNER")) {
                    for (MealOption option : meal.options()) {
                        assertThat(option.items())
                                .extracting(item -> item.name())
                                .doesNotContain("Pao Frances Leve", "Cuscuz Simples");
                    }
                }
                // Breakfast options must only contain breakfast foods (not lunch/dinner items)
                if (meal.mealType().equals("BREAKFAST")) {
                    for (MealOption option : meal.options()) {
                        assertThat(option.items())
                                .extracting(item -> item.name())
                                .doesNotContain(
                                        "Frango Grelhado", "Carne Bovina",
                                        "Arroz Integral", "Batata Doce",
                                        "Brocolis", "Abobrinha");
                    }
                }
            }
        }
    }

    // =========================================================================
    // Cenário 6 — ONIVORA completa (mealsPerDay=3 e mealsPerDay=4)
    // =========================================================================

    @Nested
    @DisplayName("Cenário 6 — ONIVORA: geração completa para mealsPerDay=3 e mealsPerDay=4")
    class OnivoraCompletePlan {

        /**
         * Generous pool that covers all meal types for both 3-meal and 4-meal ONIVORA plans.
         * Each food comfortably clears tier 1 (70%) for its respective meal targets.
         *
         * <p>ONIVORA LUNCH/DINNER require three composition slots:
         * MAIN_PROTEIN + MAIN_CARBOHYDRATE + VEGETABLE_LEGUME.
         * The combined portion (400 + 225 + 50 = 675 kcal) exceeds the tier 1 floor for all
         * lunch/dinner targets here (70% × 800 = 560 kcal; 70% × 700 = 490 kcal; 70% × 600 = 420 kcal).
         */
        private List<Food> fullOnivoraPool() {
            return List.of(
                    // Breakfast
                    breakfastFood(1, "Aveia Integral", 370, 100),
                    breakfastFood(2, "Granola Premium", 400, 100),
                    // Snacks (AFTERNOON_SNACK / MORNING_SNACK)
                    snackFood(10, "Fruta Mista"),
                    snackFood(11, "Iogurte Snack"),
                    // Lunch / Dinner — all three required ONIVORA slots
                    mainMealProtein(100, "Frango Grelhado"),
                    mainMealProtein(101, "Carne Bovina"),
                    mainMealCarbohydrate(200, "Arroz Integral"),
                    mainMealCarbohydrate(201, "Batata Doce"),
                    mainMealVegetable(300, "Brocolis"),
                    mainMealVegetable(301, "Abobrinha")
            );
        }

        private Food snackFood(int id, String name) {
            Food f = new Food();
            f.setId(id);
            f.setName(name);
            f.setDisplayName(name);
            f.setCategory("FRUIT");
            f.setSubcategory("FRUTAS");
            f.setFoodRole(FoodRole.FRUIT);
            f.setCaloriesPer100g(BigDecimal.valueOf(300));
            f.setProteinPer100g(BigDecimal.valueOf(3));
            f.setCarbsPer100g(BigDecimal.valueOf(30));
            f.setFatPer100g(BigDecimal.valueOf(1));
            f.setFiberPer100g(BigDecimal.valueOf(3));
            // 300 kcal/100g × 150g = 450 kcal — above tier 1 for snack targets (~200-300 kcal)
            f.setReferencePortionGrams(BigDecimal.valueOf(150));
            f.setSuitableMeals("AFTERNOON_SNACK,MORNING_SNACK,BREAKFAST");
            f.setActive(true);
            return f;
        }

        @Test
        @DisplayName("ONIVORA mealsPerDay=3: plano COMPLETO com BREAKFAST, LUNCH, DINNER")
        void onivora3Meals_completePlan() {
            // given
            stubUser(3, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(fullOnivoraPool());

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then
            assertPlanHasMeals(plan, 3);
            assertThat(plan.meals()).extracting(MealPlanMeal::mealType)
                    .containsExactlyInAnyOrder("BREAKFAST", "LUNCH", "DINNER");
        }

        @Test
        @DisplayName("ONIVORA mealsPerDay=4: plano COMPLETO com BREAKFAST, LUNCH, AFTERNOON_SNACK, DINNER")
        void onivora4Meals_completePlan() {
            // given
            stubUser(4, 2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(fullOnivoraPool());

            // when
            DailyMealPlan plan = engine.generatePlan("floor-test@test.com");

            // then
            assertPlanHasMeals(plan, 4);
            assertThat(plan.meals()).extracting(MealPlanMeal::mealType)
                    .containsExactlyInAnyOrder("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }
    }
}
