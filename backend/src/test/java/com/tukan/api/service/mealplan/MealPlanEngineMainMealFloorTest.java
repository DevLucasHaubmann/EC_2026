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
 * Integration tests for Bug B4 (extension) — lunch/dinner supplementation respects the effective
 * {@code MealCompositionRule} of the meal variant.
 *
 * <p>Uses the real pipeline (FoodFilterService, FoodCurationService, MealSuitabilityService,
 * MealDistributor, FoodSelector); only external boundaries (repositories, user service,
 * CalorieCalculator) are mocked.
 *
 * <p>Key invariant validated: {@code supplementMainMeal} must NOT add roles outside
 * {@code rule.allowedRoles()} regardless of caloric density. The discriminating test for
 * LOW_CARB uses a food with {@code FoodRole.MAIN_CARBOHYDRATE} and {@code carbsPer100g <= 20}
 * (passes the low-carb diet filter) to confirm the new code rejects it during supplementation
 * because {@code LUNCH_LOW_CARB.allowedRoles()} does NOT include {@code MAIN_CARBOHYDRATE}.
 *
 * <p>Calorie math reference (dailyCalories = 2000):
 * <pre>
 * mealsPerDay=4: LUNCH=35%(700) → floor=280 kcal; DINNER=30%(600) → floor=240 kcal
 * mealsPerDay=5: DINNER=25%(500) → floor=200 kcal
 * mealsPerDay=3: LUNCH=40%(800) → floor=320 kcal
 * </pre>
 */
@DisplayName("MealPlanEngine — main meal (LUNCH/DINNER) calorie floor supplementation (Bug B4)")
class MealPlanEngineMainMealFloorTest {

    private static final double DAILY_CALORIES = 2000.0;
    private static final String TEST_EMAIL = "main-meal-floor@test.com";

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
    // Scenario 1 — LOW_CARB discriminating test
    // =========================================================================

    @Nested
    @DisplayName("Scenario 1 — LOW_CARB: supplementation must not add MAIN_CARBOHYDRATE")
    class LowCarbSupplementationRespectesComposition {

        /**
         * Pool design — mealsPerDay=4, LUNCH_LOW_CARB target=700 kcal, floor=280 kcal:
         *
         * Allowed roles for LUNCH_LOW_CARB: {MAIN_PROTEIN, VEGETABLE_LEGUME, HEALTHY_FAT}.
         *
         * Initial composition (passes low-carb filter, carbs <= 20g/100g per food):
         *   protein:   180 kcal/100g × 100g = 180 kcal  [MAIN_PROTEIN, carbs=0]
         *   vegetable:  30 kcal/100g × 100g =  30 kcal  [VEGETABLE_LEGUME, carbs=5]
         *   total initial = 210 kcal < floor 280 kcal
         *
         * Discriminating food: carbohydrateDecoy
         *   role=MAIN_CARBOHYDRATE, carbs=18g/100g (passes low-carb filter!), 350 kcal/100g × 100g = 350 kcal
         *   After supplement: 210 + 350 = 560 kcal >= 280 kcal BUT must NOT be added.
         *
         * Correct supplement: healthyFat
         *   role=HEALTHY_FAT, carbs=5g/100g, 600 kcal/100g × 20g = 120 kcal
         *   After supplement: 210 + 120 = 330 kcal >= 280 kcal, and HEALTHY_FAT is allowed.
         *
         * Assertion: totalCalories >= 280 AND no item has role MAIN_CARBOHYDRATE.
         */
        @Test
        @DisplayName("LUNCH_LOW_CARB mealsPerDay=4: supplement uses HEALTHY_FAT, never MAIN_CARBOHYDRATE")
        void lowCarb_lunch_4meals_supplementDoesNotAddCarbohydrate() {
            // given
            // mealsPerDay=4: LUNCH target = 35% × 2000 = 700 kcal → floor = 40% × 700 = 280 kcal
            double lunchTarget = 0.35 * DAILY_CALORIES; // 700
            double lunchFloor  = 0.40 * lunchTarget;    // 280

            stubUser(Assessment.DietType.LOW_CARB, 4);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    lowCarbPool4Meals(
                            // initial option below floor: 180 + 30 = 210 kcal
                            lowCarbProtein(10, "Frango Low Carb", 180, 100),   // 180 kcal
                            lowCarbVegetable(20, "Brocolis", 30, 100),          // 30 kcal
                            // discriminating decoy: passes low-carb filter (carbs=18 <= 20) but MUST be rejected
                            lowCarbCarbohydrateDecoy(30, "Batata Doce Pequena", 350, 100, 18),
                            // correct supplement: HEALTHY_FAT — allowed by LUNCH_LOW_CARB
                            lowCarbHealthyFat(40, "Azeite Oliva", 600, 20)     // 120 kcal → total 330
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).isNotEmpty();
            MealPlanMeal lunch = findMeal(plan, "LUNCH");
            assertThat(lunch.options()).isNotEmpty();

            lunch.options().forEach(option -> {
                // floor reached
                assertThat(option.totalCalories())
                        .as("LUNCH option totalCalories must be >= floor %.0f", lunchFloor)
                        .isGreaterThanOrEqualTo(lunchFloor);

                // discriminating assertion: MAIN_CARBOHYDRATE must never appear in LOW_CARB lunch
                boolean hasCarbohydrateItem = option.items().stream()
                        .anyMatch(item -> {
                            // look up the food role by matching food id from the pool
                            return "Batata Doce Pequena".equals(item.name());
                        });
                assertThat(hasCarbohydrateItem)
                        .as("MAIN_CARBOHYDRATE decoy must NOT appear in LOW_CARB supplemented lunch")
                        .isFalse();
            });
        }
    }

    // =========================================================================
    // Scenario 2 — VEGANA DINNER supplement reaches floor
    // =========================================================================

    @Nested
    @DisplayName("Scenario 2 — VEGANA DINNER: supplement adds VEGETABLE_LEGUME to reach floor")
    class VeganaDinnerSupplementToFloor {

        /**
         * Pool design — mealsPerDay=5, DINNER target=500 kcal, floor=200 kcal:
         *
         * Allowed roles for DINNER (VEGANA treated as standard composition):
         *   {MAIN_PROTEIN, MAIN_CARBOHYDRATE, VEGETABLE_LEGUME}
         *
         * Initial option after buildTwoOptions:
         *   veganProtein: 150 kcal/100g × 100g = 150 kcal  [MAIN_PROTEIN]
         *   — no carbohydrate or vegetable to fill remaining slots (excluded from pool)
         *   total initial = 150 kcal < floor 200 kcal
         *
         * Supplement: veganVegetable dense
         *   VEGETABLE_LEGUME, 80 kcal/100g × 150g = 120 kcal → total 270 kcal >= 200.
         */
        @Test
        @DisplayName("VEGANA DINNER mealsPerDay=5: supplemented to floor with VEGETABLE_LEGUME")
        void vegana_dinner_5meals_supplementedToFloor() {
            // given
            // mealsPerDay=5: DINNER target = 25% × 2000 = 500 kcal → floor = 40% × 500 = 200 kcal
            double dinnerTarget = 0.25 * DAILY_CALORIES; // 500
            double dinnerFloor  = 0.40 * dinnerTarget;   // 200

            stubUser(Assessment.DietType.VEGANA, 5);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    veganPool5Meals(
                            // only protein in initial pool — starts below floor
                            veganProtein(10, "Tofu Firme", 150, 100),          // 150 kcal
                            // supplement candidate: VEGETABLE_LEGUME dense enough
                            veganVegetableDense(20, "Lentilha Cozida", 80, 150) // 120 kcal → total 270
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).isNotEmpty();
            MealPlanMeal dinner = findMeal(plan, "DINNER");
            assertThat(dinner.options()).isNotEmpty();
            dinner.options().forEach(option ->
                    assertThat(option.totalCalories())
                            .as("DINNER option totalCalories must be >= floor %.0f", dinnerFloor)
                            .isGreaterThanOrEqualTo(dinnerFloor));
        }
    }

    // =========================================================================
    // Scenario 3 — CARNIVORA LUNCH: supplement adds only MAIN_PROTEIN
    // =========================================================================

    @Nested
    @DisplayName("Scenario 3 — CARNIVORA LUNCH: supplement adds MAIN_PROTEIN only")
    class CarnivoraLunchSupplementProteinOnly {

        /**
         * Pool design — mealsPerDay=3, LUNCH_CARNIVORA target=800 kcal, floor=320 kcal:
         *
         * Allowed roles for LUNCH_CARNIVORA: {MAIN_PROTEIN} only.
         *
         * Initial option: one small protein portion = 200 kcal < 320 kcal floor.
         * Supplement adds a second MAIN_PROTEIN from pool → total >= 320 kcal.
         * No vegetable/carbohydrate in pool (CARNIVORA filter blocks them anyway).
         *
         * Assertion: totalCalories >= 320 AND all items originate from MAIN_PROTEIN foods.
         */
        @Test
        @DisplayName("CARNIVORA LUNCH mealsPerDay=3: supplement adds MAIN_PROTEIN to reach floor")
        void carnivora_lunch_3meals_supplementAddsMainProteinOnly() {
            // given
            // mealsPerDay=3: LUNCH target = 40% × 2000 = 800 kcal → floor = 40% × 800 = 320 kcal
            double lunchTarget = 0.40 * DAILY_CALORIES; // 800
            double lunchFloor  = 0.40 * lunchTarget;    // 320

            stubUser(Assessment.DietType.CARNIVORA, 3);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    carnivoraPool3Meals(
                            // initial protein: 200 kcal/100g × 100g = 200 kcal (below floor 320)
                            carnivoraProtein(10, "Frango Grelhado", 200, 100),
                            // supplement protein: 220 kcal/100g × 150g = 330 kcal → total >= 320
                            carnivoraProtein(11, "Carne Bovina Magra", 220, 150)
                    ));

            // when
            DailyMealPlan plan = engine.generatePlan(TEST_EMAIL);

            // then
            assertThat(plan.meals()).isNotEmpty();
            MealPlanMeal lunch = findMeal(plan, "LUNCH");
            assertThat(lunch.options()).isNotEmpty();

            lunch.options().forEach(option -> {
                assertThat(option.totalCalories())
                        .as("CARNIVORA LUNCH option totalCalories must be >= floor %.0f", lunchFloor)
                        .isGreaterThanOrEqualTo(lunchFloor);

                // All supplement items must be MAIN_PROTEIN foods — no vegetable/carb/fat allowed
                boolean allProtein = option.items().stream()
                        .allMatch(item ->
                                "Frango Grelhado".equals(item.name())
                                || "Carne Bovina Magra".equals(item.name()));
                assertThat(allProtein)
                        .as("All items in CARNIVORA lunch must be MAIN_PROTEIN foods")
                        .isTrue();
            });
        }
    }

    // =========================================================================
    // Scenario 4 — Pool insufficient: controlled BusinessException
    // =========================================================================

    @Nested
    @DisplayName("Scenario 4 — pool insufficient: BusinessException with mealType and 'mínimo calórico'")
    class InsufficientMainMealPoolThrows {

        /**
         * LOW_CARB LUNCH with an intentionally sparse pool: all three required LUNCH_LOW_CARB slots
         * (MAIN_PROTEIN, VEGETABLE_LEGUME, HEALTHY_FAT) have exactly one tiny food each, so
         * validateRequiredSlots passes but the combined calories (15+5+2=22 kcal) remain far below
         * the 280 kcal floor even after supplementation. Engine must throw BusinessException with
         * the meal type in the message — never persist the subcaloric plan.
         */
        @Test
        @DisplayName("LOW_CARB LUNCH mealsPerDay=4: pool too sparse — BusinessException")
        void lowCarb_lunch_4meals_tooSparse_throws() {
            // given
            stubUser(Assessment.DietType.LOW_CARB, 4);
            when(foodRepository.findByActiveTrue()).thenReturn(
                    lowCarbPool4Meals(
                            // MAIN_PROTEIN: 15 kcal/100g × 100g = 15 kcal
                            lowCarbProtein(10, "Clara de Ovo", 15, 100),
                            // VEGETABLE_LEGUME: 10 kcal/100g × 50g = 5 kcal
                            lowCarbVegetable(11, "Alface", 10, 50),
                            // HEALTHY_FAT: 20 kcal/100g × 10g = 2 kcal
                            lowCarbHealthyFat(12, "Azeite Minimo", 20, 10)
                            // total max ~22 kcal << 280 kcal floor — supplementation still fails
                    ));

            // when / then
            assertThatThrownBy(() -> engine.generatePlan(TEST_EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("mínimo calórico");
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
     * MAIN_PROTEIN food that passes LOW_CARB filter (carbs=0 <= 20g/100g).
     * Calories per portion = caloriesPer100g * portionGrams / 100.
     */
    private Food lowCarbProtein(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("AVES");
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(25));
        f.setCarbsPer100g(BigDecimal.valueOf(0));  // carbs=0 passes low-carb filter
        f.setFatPer100g(BigDecimal.valueOf(5));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /**
     * VEGETABLE_LEGUME food that passes LOW_CARB filter (carbs=5 <= 20g/100g).
     */
    private Food lowCarbVegetable(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("VEGETABLE");
        f.setSubcategory("LEGUMES_E_VERDURAS");
        f.setFoodRole(FoodRole.VEGETABLE_LEGUME);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(2));
        f.setCarbsPer100g(BigDecimal.valueOf(5));  // carbs=5 passes low-carb filter
        f.setFatPer100g(BigDecimal.valueOf(0));
        f.setFiberPer100g(BigDecimal.valueOf(3));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /**
     * MAIN_CARBOHYDRATE food that passes LOW_CARB filter (carbs=carbsPer100g <= 20g/100g).
     * This is the discriminating decoy: despite passing the diet filter, it must NOT be added
     * during supplementation because LUNCH_LOW_CARB.allowedRoles() excludes MAIN_CARBOHYDRATE.
     */
    private Food lowCarbCarbohydrateDecoy(int id, String name, double caloriesPer100g,
                                           double portionGrams, double carbsPer100g) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("CARBOHYDRATE");
        f.setSubcategory("CEREAIS_E_GRAOS");
        f.setFoodRole(FoodRole.MAIN_CARBOHYDRATE);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(3));
        f.setCarbsPer100g(BigDecimal.valueOf(carbsPer100g)); // <= 20 passes low-carb filter
        f.setFatPer100g(BigDecimal.valueOf(1));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /**
     * HEALTHY_FAT food that passes LOW_CARB filter (carbs=5 <= 20g/100g).
     * This is the correct supplement for LUNCH_LOW_CARB — HEALTHY_FAT is in allowedRoles().
     */
    private Food lowCarbHealthyFat(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("HEALTHY_FAT");
        f.setSubcategory("GORDURAS_E_OLEOS");
        f.setFoodRole(FoodRole.HEALTHY_FAT);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(0));
        f.setCarbsPer100g(BigDecimal.valueOf(5));  // carbs=5 passes low-carb filter
        f.setFatPer100g(BigDecimal.valueOf(70));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /** MAIN_PROTEIN vegan food (VEGANA diet path). */
    private Food veganProtein(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("LEGUMINOSAS");
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(16));
        f.setCarbsPer100g(BigDecimal.valueOf(10));
        f.setFatPer100g(BigDecimal.valueOf(8));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /**
     * VEGETABLE_LEGUME vegan food with higher caloric density for supplement purposes.
     * Used in Scenario 2 to push dinner above the floor via supplementation.
     */
    private Food veganVegetableDense(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("VEGETABLE");
        f.setSubcategory("LEGUMES_E_VERDURAS");
        f.setFoodRole(FoodRole.VEGETABLE_LEGUME);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(9));
        f.setCarbsPer100g(BigDecimal.valueOf(20));
        f.setFatPer100g(BigDecimal.valueOf(1));
        f.setFiberPer100g(BigDecimal.valueOf(8));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /**
     * MAIN_PROTEIN food for CARNIVORA: animal subcategory so it passes passesCarnivoraFilter.
     * carbs=0 is irrelevant here — CARNIVORA filter checks category/subcategory, not carbs.
     */
    private Food carnivoraProtein(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("AVES");  // in CARNIVORA_ANIMAL_SUBCATEGORIES
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(25));
        f.setCarbsPer100g(BigDecimal.valueOf(0));
        f.setFatPer100g(BigDecimal.valueOf(10));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("LUNCH,DINNER");
        f.setActive(true);
        return f;
    }

    /** Adequate ONIVORA breakfast: 370 kcal/100g × 100g = 370 kcal, clears tier-1 floor (280 kcal). */
    private Food adequateBreakfast(int id, String name, boolean vegan) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("CARBOHYDRATE");
        f.setSubcategory("CEREAIS_E_GRAOS");
        f.setFoodRole(FoodRole.BREAKFAST_LIGHT);
        f.setCaloriesPer100g(BigDecimal.valueOf(370));
        f.setProteinPer100g(BigDecimal.valueOf(10));
        f.setCarbsPer100g(BigDecimal.valueOf(18));  // 18 <= 20 — passes low-carb filter too
        f.setFatPer100g(BigDecimal.valueOf(5));
        f.setFiberPer100g(BigDecimal.valueOf(3));
        f.setReferencePortionGrams(BigDecimal.valueOf(100));
        f.setSuitableMeals("BREAKFAST");
        f.setVegan(vegan);
        f.setVegetarian(vegan);
        f.setActive(true);
        return f;
    }

    /** Adequate CARNIVORA breakfast (MAIN_PROTEIN, AVES subcategory). */
    private Food carnivoraBreakfast(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("AVES");
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(160));
        f.setProteinPer100g(BigDecimal.valueOf(22));
        f.setCarbsPer100g(BigDecimal.valueOf(0));
        f.setFatPer100g(BigDecimal.valueOf(8));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        f.setReferencePortionGrams(BigDecimal.valueOf(200)); // 320 kcal — clears 80 kcal BREAKFAST floor
        f.setSuitableMeals("BREAKFAST");
        f.setActive(true);
        return f;
    }

    // =========================================================================
    // Helpers — pool builders
    // =========================================================================

    /**
     * LOW_CARB mealsPerDay=4 pool: adequate breakfast (carbs=18 passes low-carb filter) +
     * an AFTERNOON_SNACK food (carbs=10 passes low-carb filter) +
     * caller-provided LUNCH/DINNER foods (all must also pass low-carb filter).
     * BREAKFAST target=20%×2000=400 kcal, tier-1 floor=280 kcal; adequateBreakfast=370 kcal → clears.
     * AFTERNOON_SNACK target=15%×2000=300 kcal, floor=120 kcal; snack=90×120/100=108 kcal — below floor.
     * A second snack food is added so the snack slot can be supplemented to floor.
     */
    private List<Food> lowCarbPool4Meals(Food... lunchDinnerFoods) {
        List<Food> pool = new ArrayList<>();
        pool.add(adequateBreakfast(1, "Aveia", false));
        pool.add(adequateBreakfast(2, "Granola", false));
        // AFTERNOON_SNACK: two FRUIT-role foods that pass low-carb filter (carbs=10 <= 20)
        // First: 30 kcal/100g × 120g = 36 kcal — below snack floor 120 kcal alone
        // Second (supplement): 620 kcal/100g × 15g = 93 kcal → total 129 >= 120 snack floor
        pool.add(lowCarbSnack(60, "Morango", 30, 120));
        pool.add(lowCarbSnack(61, "Coco Ralado Low Carb", 620, 15));
        pool.addAll(List.of(lunchDinnerFoods));
        return pool;
    }

    /**
     * VEGANA mealsPerDay=5 pool: adequate vegan breakfast + snacks for both snack slots +
     * complete vegan LUNCH foods (MAIN_PROTEIN + MAIN_CARBOHYDRATE + VEGETABLE_LEGUME required) +
     * caller-provided DINNER foods.
     *
     * <p>LUNCH uses the standard DINNER rule (same composition), so its three required slots must
     * all be satisfiable from the pool — otherwise validateRequiredSlots throws before supplementation.
     * The caller-provided foods are intended for DINNER only; LUNCH is pre-populated here with a
     * complete set so the engine can build a valid LUNCH option independently.
     *
     * <p>MORNING_SNACK and AFTERNOON_SNACK slots each receive a snack food to pass validateMealCoverage.
     */
    private List<Food> veganPool5Meals(Food... dinnerFoods) {
        List<Food> pool = new ArrayList<>();
        // Breakfast
        pool.add(adequateBreakfast(1, "Aveia", true));
        pool.add(adequateBreakfast(2, "Granola", true));
        // Snacks (both slots need at least one food to pass validateMealCoverage)
        pool.add(veganSnack(50, "Banana"));
        pool.add(veganSnack(51, "Castanha"));
        // LUNCH: complete three-slot vegan composition (MAIN_PROTEIN + MAIN_CARBOHYDRATE + VEGETABLE_LEGUME)
        pool.add(veganLunchProtein(60, "Feijao Preto"));
        pool.add(veganLunchCarbohydrate(61, "Arroz Integral"));
        pool.add(veganLunchVegetable(62, "Brocolis"));
        // Dinner foods from caller (used for the DINNER slot)
        pool.addAll(List.of(dinnerFoods));
        return pool;
    }

    /**
     * CARNIVORA mealsPerDay=3 pool: adequate carnivore breakfast + caller-provided LUNCH/DINNER foods.
     * No snack slots for 3-meal plans.
     */
    private List<Food> carnivoraPool3Meals(Food... lunchDinnerFoods) {
        List<Food> pool = new ArrayList<>();
        pool.add(carnivoraBreakfast(1, "Ovo Cozido"));
        pool.add(carnivoraBreakfast(2, "Peito de Frango Breakfast"));
        pool.addAll(List.of(lunchDinnerFoods));
        return pool;
    }

    /**
     * FRUIT snack food that passes LOW_CARB filter (carbs=10 <= 20g/100g).
     * Role FRUIT is snack-eligible per FoodRole.isSnackEligible() and satisfies the
     * FRUIT_OR_SNACK_SLOT required by AFTERNOON_SNACK composition.
     * Used to populate AFTERNOON_SNACK in the LOW_CARB 4-meal pool.
     */
    private Food lowCarbSnack(int id, String name, double caloriesPer100g, double portionGrams) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("FRUIT");
        f.setSubcategory("FRUTAS");
        f.setFoodRole(FoodRole.FRUIT);  // FRUIT is snack-eligible → satisfies FRUIT_OR_SNACK_SLOT
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(1));
        f.setCarbsPer100g(BigDecimal.valueOf(10));  // carbs=10 <= 20 passes low-carb filter
        f.setFatPer100g(BigDecimal.valueOf(1));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        f.setReferencePortionGrams(BigDecimal.valueOf(portionGrams));
        f.setSuitableMeals("MORNING_SNACK,AFTERNOON_SNACK");
        f.setActive(true);
        return f;
    }

    /** MAIN_PROTEIN vegan food for LUNCH slot (standard composition). */
    private Food veganLunchProtein(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("PROTEIN");
        f.setSubcategory("LEGUMINOSAS");
        f.setFoodRole(FoodRole.MAIN_PROTEIN);
        f.setCaloriesPer100g(BigDecimal.valueOf(130));
        f.setProteinPer100g(BigDecimal.valueOf(9));
        f.setCarbsPer100g(BigDecimal.valueOf(23));
        f.setFatPer100g(BigDecimal.valueOf(0));
        f.setFiberPer100g(BigDecimal.valueOf(8));
        f.setReferencePortionGrams(BigDecimal.valueOf(180)); // 234 kcal
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /** MAIN_CARBOHYDRATE vegan food for LUNCH slot (standard composition). */
    private Food veganLunchCarbohydrate(int id, String name) {
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
        f.setReferencePortionGrams(BigDecimal.valueOf(150)); // 225 kcal
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /** VEGETABLE_LEGUME vegan food for LUNCH slot (standard composition). */
    private Food veganLunchVegetable(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("VEGETABLE");
        f.setSubcategory("LEGUMES_E_VERDURAS");
        f.setFoodRole(FoodRole.VEGETABLE_LEGUME);
        f.setCaloriesPer100g(BigDecimal.valueOf(35));
        f.setProteinPer100g(BigDecimal.valueOf(3));
        f.setCarbsPer100g(BigDecimal.valueOf(7));
        f.setFatPer100g(BigDecimal.valueOf(0));
        f.setFiberPer100g(BigDecimal.valueOf(3));
        f.setReferencePortionGrams(BigDecimal.valueOf(100)); // 35 kcal
        f.setSuitableMeals("LUNCH,DINNER");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
    }

    /** Vegan snack food (FRUIT role) to populate MORNING_SNACK and AFTERNOON_SNACK slots. */
    private Food veganSnack(int id, String name) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory("FRUIT");
        f.setSubcategory("FRUTAS");
        f.setFoodRole(FoodRole.FRUIT);
        f.setCaloriesPer100g(BigDecimal.valueOf(90));
        f.setProteinPer100g(BigDecimal.valueOf(1));
        f.setCarbsPer100g(BigDecimal.valueOf(20));
        f.setFatPer100g(BigDecimal.valueOf(0));
        f.setFiberPer100g(BigDecimal.valueOf(2));
        f.setReferencePortionGrams(BigDecimal.valueOf(120)); // 108 kcal — clears 80 kcal snack floor
        f.setSuitableMeals("MORNING_SNACK,AFTERNOON_SNACK");
        f.setVegan(true);
        f.setVegetarian(true);
        f.setActive(true);
        return f;
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
