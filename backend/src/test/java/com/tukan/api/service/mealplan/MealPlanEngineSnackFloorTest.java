package com.tukan.api.service.mealplan;

import com.tukan.api.dto.mealplan.DailyMealPlan;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Bug B4 — snack calorie floor supplementation.
 *
 * <p>Uses the real pipeline (FoodFilterService, FoodCurationService, MealSuitabilityService,
 * MealDistributor, FoodSelector); only external boundaries (repositories, user service,
 * CalorieCalculator) are mocked. This validates that the snack best-effort supplementation path
 * introduced in Bug B4 behaves correctly under real pool constraints.
 *
 * <p>Scenarios covered:
 * <ol>
 *   <li>Scenario A — ONIVORA + VEGANA: subcaloric snack supplemented to reach the 40% floor.</li>
 *   <li>Scenario B — pool insufficient → controlled BusinessException, plan not persisted.</li>
 *   <li>Scenario C — breakfast extreme-fallback still accepts subcaloric (no regression).</li>
 * </ol>
 *
 * <p>Calorie math reference (dailyCalories = 2000):
 * <pre>
 * mealsPerDay=4: BREAKFAST=20%(400), LUNCH=35%(700), AFTERNOON_SNACK=15%(300), DINNER=30%(600)
 *   AFTERNOON_SNACK 40% floor = 120 kcal
 * mealsPerDay=5: BREAKFAST=20%(400), MORNING_SNACK=10%(200), LUNCH=35%(700), AFTERNOON_SNACK=10%(200), DINNER=25%(500)
 *   MORNING_SNACK and AFTERNOON_SNACK 40% floor = 80 kcal
 * </pre>
 */
@DisplayName("MealPlanEngine — snack calorie floor supplementation (Bug B4)")
class MealPlanEngineSnackFloorTest {

    private static final double DAILY_CALORIES = 2000.0;
    private static final String TEST_EMAIL = "snack-test@test.com";

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
        user.setEmail(TEST_EMAIL);

        profile = new NutritionalProfile();
        profile.setGender(NutritionalProfile.Gender.FEMALE);
        profile.setWeightKg(65.0);
        profile.setHeightCm(165.0);
        profile.setActivityLevel(NutritionalProfile.ActivityLevel.MODERATE);
        profile.setDateOfBirth(LocalDate.now().minusYears(28));
    }

    // =========================================================================
    // Scenario A — subcaloric snack supplemented to floor
    // =========================================================================

    @Nested
    @DisplayName("Scenario A — subcaloric snack supplemented with HEALTHY_FAT to reach 40% floor")
    class SnackSupplementedToFloor {

        /**
         * Pool design — mealsPerDay=4, AFTERNOON_SNACK target=300 kcal, floor=120 kcal:
         *   snackFruit: 40 kcal/100g x 150g = 60 kcal (below floor alone)
         *   healthyFat: 600 kcal/100g x 15g = 90 kcal (supplement)
         *   total after supplement: 150 kcal above 120 kcal floor
         * Breakfast: two adequate options (370 kcal each — above tier 1 floor 280 kcal).
         * Lunch/Dinner: ONIVORA three-slot pool (protein+carb+vegetable, ~675 kcal total).
         */
        @Test
        @DisplayName("ONIVORA mealsPerDay=4: AFTERNOON_SNACK (60 kcal) supplemented to 150 kcal")
        void onivora_4meals_afternoonSnackSupplemented() {
            // given
            // mealsPerDay=4: AFTERNOON_SNACK target = 15% × 2000 = 300 kcal → floor = 40% × 300 = 120 kcal
            double snackTarget = 0.15 * DAILY_CALORIES; // 300
            double snackFloor  = 0.40 * snackTarget;    // 120
            stubUser(Assessment.DietType.ONIVORA, 4);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    pool4Meals(
                            snackFruit(10, "Maca", 40, 150),         // 60 kcal — below floor
                            healthyFatSnack(20, "Castanha", 600, 15) // 90 kcal — supplement → total 150
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).hasSize(4);
            MealPlanMeal snack = findMeal(plan, "AFTERNOON_SNACK");
            assertThat(snack.options()).isNotEmpty();
            snack.options().forEach(o -> {
                assertThat(o.items()).isNotEmpty();
                assertThat(o.totalCalories()).isGreaterThanOrEqualTo(snackFloor);
            });
        }

        /**
         * Same calorie design, but VEGANA diet type.
         * All foods must have vegan=true to pass FoodFilterService.passesDietFilter.
         * mealsPerDay=4: AFTERNOON_SNACK target=300 kcal → floor=120 kcal.
         * snackFruit: 40×150/100=60 kcal; healthyFat: 560×15/100=84 kcal → total 144 kcal >= 120.
         */
        @Test
        @DisplayName("VEGANA mealsPerDay=4: AFTERNOON_SNACK supplemented with HEALTHY_FAT above floor")
        void vegana_4meals_afternoonSnackSupplemented() {
            // given
            // mealsPerDay=4: AFTERNOON_SNACK target = 15% × 2000 = 300 kcal → floor = 40% × 300 = 120 kcal
            double snackTarget = 0.15 * DAILY_CALORIES; // 300
            double snackFloor  = 0.40 * snackTarget;    // 120
            stubUser(Assessment.DietType.VEGANA, 4);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    veganPool4Meals(
                            snackFruit(10, "Banana", 40, 150),
                            healthyFatSnack(20, "Amendoim", 560, 15)
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).hasSize(4);
            MealPlanMeal snack = findMeal(plan, "AFTERNOON_SNACK");
            assertThat(snack.options()).isNotEmpty();
            snack.options().forEach(o -> {
                assertThat(o.items()).isNotEmpty();
                assertThat(o.totalCalories()).isGreaterThanOrEqualTo(snackFloor);
            });
        }

        /**
         * Pool design — mealsPerDay=5, MORNING_SNACK target=200 kcal, floor=80 kcal:
         *   snackFruit: 40 kcal/100g x 50g = 20 kcal (below floor)
         *   healthyFat: 600 kcal/100g x 15g = 90 kcal (supplement)
         *   total after supplement: 110 kcal above 80 kcal floor
         */
        @Test
        @DisplayName("ONIVORA mealsPerDay=5: MORNING_SNACK (20 kcal) supplemented to 110 kcal")
        void onivora_5meals_morningSnackSupplemented() {
            // given
            // mealsPerDay=5: MORNING_SNACK target = 10% × 2000 = 200 kcal → floor = 40% × 200 = 80 kcal
            double snackTarget = 0.10 * DAILY_CALORIES; // 200
            double snackFloor  = 0.40 * snackTarget;    // 80
            stubUser(Assessment.DietType.ONIVORA, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    pool5Meals(
                            snackFruit(10, "Maca", 40, 50),          // 20 kcal — below floor
                            healthyFatSnack(20, "Castanha", 600, 15) // 90 kcal — supplement → total 110
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).hasSize(5);
            MealPlanMeal morning = findMeal(plan, "MORNING_SNACK");
            assertThat(morning.options()).isNotEmpty();
            morning.options().forEach(o -> {
                assertThat(o.items()).isNotEmpty();
                assertThat(o.totalCalories()).isGreaterThanOrEqualTo(snackFloor);
            });
        }

        /**
         * VEGANA + AFTERNOON_SNACK in 5-meal plan (target=200 kcal, floor=80 kcal).
         * snackFruit: 40×50/100=20 kcal; healthyFat: 650×15/100=97.5 kcal → total 117.5 >= 80.
         */
        @Test
        @DisplayName("VEGANA mealsPerDay=5: AFTERNOON_SNACK supplemented with HEALTHY_FAT above floor")
        void vegana_5meals_afternoonSnackSupplemented() {
            // given
            // mealsPerDay=5: AFTERNOON_SNACK target = 10% × 2000 = 200 kcal → floor = 40% × 200 = 80 kcal
            double snackTarget = 0.10 * DAILY_CALORIES; // 200
            double snackFloor  = 0.40 * snackTarget;    // 80
            stubUser(Assessment.DietType.VEGANA, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    veganPool5Meals(
                            snackFruit(10, "Manga", 40, 50),
                            healthyFatSnack(20, "Nozes", 650, 15)
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).hasSize(5);
            MealPlanMeal afternoon = findMeal(plan, "AFTERNOON_SNACK");
            assertThat(afternoon.options()).isNotEmpty();
            afternoon.options().forEach(o -> {
                assertThat(o.items()).isNotEmpty();
                assertThat(o.totalCalories()).isGreaterThanOrEqualTo(snackFloor);
            });
        }
    }

    // =========================================================================
    // Scenario B — pool insufficient -> BusinessException (no subcaloric persistence)
    // =========================================================================

    @Nested
    @DisplayName("Scenario B — pool too sparse for supplementation — BusinessException thrown")
    class InsufficientSnackPoolThrows {

        /**
         * snackFruit yields only 2.5 kcal, no supplement available.
         * Expected: BusinessException mentioning the snack slot and "minimo calorico".
         */
        @Test
        @DisplayName("ONIVORA mealsPerDay=4: AFTERNOON_SNACK pool too sparse — BusinessException")
        void onivora_4meals_insufficientAfternoonSnack_throws() {
            // given
            stubUser(Assessment.DietType.ONIVORA, 4);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    pool4Meals(
                            snackFruit(10, "Agua Aromatizada", 5, 50) // 2.5 kcal — far below 120 kcal floor
                    ));

            // when / then
            assertThatThrownBy(() -> engine.generatePlan(TEST_EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AFTERNOON_SNACK")
                    .hasMessageContaining("mínimo calórico");
        }

        /**
         * mealsPerDay=5, MORNING_SNACK target=200 kcal, floor=80 kcal.
         * snackFruit yields ~1.5 kcal, no supplement available.
         */
        @Test
        @DisplayName("VEGANA mealsPerDay=5: MORNING_SNACK pool too sparse — BusinessException")
        void vegana_5meals_insufficientMorningSnack_throws() {
            // given
            stubUser(Assessment.DietType.VEGANA, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    veganPool5Meals(
                            snackFruit(10, "Agua com Gas", 3, 50) // 1.5 kcal
                    ));

            // when / then
            assertThatThrownBy(() -> engine.generatePlan(TEST_EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mínimo calórico");
        }

        /**
         * mealsPerDay=5, AFTERNOON_SNACK target=200 kcal, floor=80 kcal. Same failure path.
         */
        @Test
        @DisplayName("ONIVORA mealsPerDay=5: AFTERNOON_SNACK pool too sparse — BusinessException")
        void onivora_5meals_insufficientAfternoonSnack_throws() {
            // given
            stubUser(Assessment.DietType.ONIVORA, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    pool5Meals(
                            snackFruit(10, "Gelatina Zero", 5, 50) // 2.5 kcal
                    ));

            // when / then
            assertThatThrownBy(() -> engine.generatePlan(TEST_EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mínimo calórico");
        }
    }

    // =========================================================================
    // Scenario C — breakfast extreme fallback (non-regression from B4)
    // =========================================================================

    @Nested
    @DisplayName("Scenario C — breakfast extreme fallback still accepts subcaloric (no regression)")
    class BreakfastExtremeFallbackRegression {

        /**
         * Breakfast food yields only 10 kcal — far below any floor. No supplement available.
         * Breakfast must NOT throw: it accepts the subcaloric result as-is.
         * Guard: B4 changes must not have altered the BREAKFAST code path.
         */
        @Test
        @DisplayName("ONIVORA mealsPerDay=3: breakfast below 40% — subcaloric accepted, no exception")
        void breakfast_extremeFallback_acceptsSubcaloric_noException() {
            // given
            stubUser(Assessment.DietType.ONIVORA, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    pool3Meals(
                            breakfastLight(1, "Biscoito Trace", 10, 100) // 10 kcal
                    ));

            // when — must NOT throw
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).hasSize(3);
            MealPlanMeal breakfast = findMeal(plan, "BREAKFAST");
            assertThat(breakfast.options()).isNotEmpty();
            breakfast.options().forEach(o -> assertThat(o.items()).isNotEmpty());
        }
    }

    // =========================================================================
    // Helpers — stubs
    // =========================================================================

    private void stubUser(Assessment.DietType dietType, int mealsPerDay) {
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        assessment.setDietType(dietType);
        assessment.setMealsPerDay(mealsPerDay);

        when(userService.findByEmail(TEST_EMAIL)).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
        when(calorieCalculator.calculateDailyCalorieTarget(any(), any(), anyInt()))
                .thenReturn(DAILY_CALORIES);
    }

    // =========================================================================
    // Helpers — food builders
    // =========================================================================

    /**
     * FRUIT-role snack food for MORNING_SNACK and AFTERNOON_SNACK.
     * Calories per portion = caloriesPer100g * portionGrams / 100.
     * Marked vegan/vegetarian since fruits are plant-based — safe for all diet types.
     */
    private Food snackFruit(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("FRUIT");
        f.setSubcategory("FRUTAS");
        f.setFoodRole(FoodRole.FRUIT);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(1));
        f.setCarbsPer100g(BigDecimal.valueOf(10));
        f.setFatPer100g(BigDecimal.valueOf(0));
        f.setFiberPer100g(BigDecimal.valueOf(1));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("MORNING_SNACK,AFTERNOON_SNACK");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /**
     * HEALTHY_FAT food for snack supplementation (nuts/seeds).
     * Calories per portion = caloriesPer100g * portionGrams / 100.
     * Marked vegan/vegetarian since nuts/seeds are plant-based.
     */
    private Food healthyFatSnack(int id, String name, double caloriesPer100g, double portionGrams) {
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
        f.setSuitableMeals("MORNING_SNACK,AFTERNOON_SNACK");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /** BREAKFAST_LIGHT food for the 3-meal regression test. */
    private Food breakfastLight(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("CARBOHYDRATE");
        f.setSubcategory("CEREAIS_E_GRAOS");
        f.setFoodRole(FoodRole.BREAKFAST_LIGHT);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(3));
        f.setCarbsPer100g(BigDecimal.valueOf(20));
        f.setFatPer100g(BigDecimal.valueOf(1));
        f.setFiberPer100g(BigDecimal.valueOf(1));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("BREAKFAST");
        f.setActive(true);
        return f;
    }

    /** Adequate ONIVORA breakfast food: 370 kcal/100g x 100g = 370 kcal, above tier-1 floor (280 kcal). */
    private Food adequateBreakfast(int id, String name) {
        return breakfastLight(id, name, 370, 100);
    }

    // =========================================================================
    // Helpers — main-meal foods
    // =========================================================================

    private Food mainProtein(int id, String name) {
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
        f.setReferencePortionGrams(BigDecimal.valueOf(200)); // 400 kcal per portion
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    private Food veganProtein(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("LEGUMINOSAS");
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(150));
        f.setProteinPer100g(BigDecimal.valueOf(16));
        f.setCarbsPer100g(BigDecimal.valueOf(10));
        f.setFatPer100g(BigDecimal.valueOf(8));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        f.setReferencePortionGrams(BigDecimal.valueOf(200)); // 300 kcal per portion
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    private Food mainCarbohydrate(int id, String name, boolean vegan) {
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
        f.setReferencePortionGrams(BigDecimal.valueOf(150)); // 225 kcal per portion
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(vegan);
        f.setVegetarian(vegan);
        f.setActive(true);
        return f;
    }

    private Food vegetable(int id, String name, boolean vegan) {
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
        f.setReferencePortionGrams(BigDecimal.valueOf(100)); // 50 kcal per portion
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(vegan);
        f.setVegetarian(vegan);
        f.setActive(true);
        return f;
    }

    // =========================================================================
    // Helpers — pool builders
    // =========================================================================

    /** mealsPerDay=3 ONIVORA: caller-provided breakfast foods + ONIVORA lunch/dinner. */
    private List<Food> pool3Meals(Food... breakfastFoods) {
        List<Food> pool = new ArrayList<>(List.of(breakfastFoods));
        pool.addAll(onivoraLunchDinner());
        return pool;
    }

    /** mealsPerDay=4 ONIVORA: adequate breakfast + caller-provided snack foods + ONIVORA lunch/dinner. */
    private List<Food> pool4Meals(Food... snackFoods) {
        List<Food> pool = new ArrayList<>();
        pool.add(adequateBreakfast(1, "Aveia"));
        pool.add(adequateBreakfast(2, "Granola"));
        pool.addAll(List.of(snackFoods));
        pool.addAll(onivoraLunchDinner());
        return pool;
    }

    /** mealsPerDay=5 ONIVORA: same structure as 4-meal pool; snack foods cover both snack slots. */
    private List<Food> pool5Meals(Food... snackFoods) {
        return pool4Meals(snackFoods);
    }

    /**
     * mealsPerDay=4 VEGANA: adequate vegan breakfast + caller-provided snack foods (must be vegan)
     * + vegan lunch/dinner. breakfastLight foods default to vegan=false so they need explicit override.
     */
    private List<Food> veganPool4Meals(Food... snackFoods) {
        List<Food> pool = new ArrayList<>();
        Food b1 = adequateBreakfast(1, "Aveia");
        b1.setVegan(true);
        b1.setVegetarian(true);
        Food b2 = adequateBreakfast(2, "Granola");
        b2.setVegan(true);
        b2.setVegetarian(true);
        pool.add(b1);
        pool.add(b2);
        pool.addAll(List.of(snackFoods));
        pool.addAll(veganLunchDinner());
        return pool;
    }

    /** mealsPerDay=5 VEGANA — same structure as 4-meal vegan pool. */
    private List<Food> veganPool5Meals(Food... snackFoods) {
        return veganPool4Meals(snackFoods);
    }

    private List<Food> onivoraLunchDinner() {
        return List.of(
                mainProtein(100, "Frango Grelhado"),
                mainProtein(101, "Carne Bovina"),
                mainCarbohydrate(200, "Arroz Integral", false),
                mainCarbohydrate(201, "Batata Doce", false),
                vegetable(300, "Brocolis", false),
                vegetable(301, "Abobrinha", false)
        );
    }

    private List<Food> veganLunchDinner() {
        return List.of(
                veganProtein(100, "Tofu"),
                veganProtein(101, "Grao de Bico"),
                mainCarbohydrate(200, "Arroz Integral", true),
                mainCarbohydrate(201, "Batata Doce", true),
                vegetable(300, "Brocolis", true),
                vegetable(301, "Abobrinha", true)
        );
    }

    // =========================================================================
    // Helpers — assertions
    // =========================================================================

    private MealPlanMeal findMeal(DailyMealPlan plan, String mealType) {
        return plan.meals().stream()
                .filter(m -> m.mealType().equals(mealType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Meal not found in plan: " + mealType));
    }
}
