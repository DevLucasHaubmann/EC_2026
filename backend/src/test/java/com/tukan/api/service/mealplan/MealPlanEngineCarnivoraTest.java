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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Teste integrado de geração CARNIVORA (Task 2.8F — blocker do QA).
 *
 * <p>Exercita o fluxo de seleção ponta a ponta com {@link MealPlanEngine}, {@link FoodFilterService},
 * {@link FoodCurationService}, {@link MealSuitabilityService}, {@link MealDistributor},
 * {@link FoodSelector} e {@code MealCompositionRules} REAIS. Apenas as fronteiras externas
 * (repositórios, usuário e cálculo calórico) são mockadas — não há mock do FoodSelector, ao
 * contrário do teste de unidade do engine, de modo a provar a interação real
 * filtro → agrupamento → composição → seleção.
 *
 * <p>Prova, com um pool misto (proteína animal + laticínio + intrusos vegetais/leguminosa/embutido),
 * que CARNIVORA: gera plano completo, mantém apenas origem animal e nunca deixa vazar vegetal,
 * fruta, grão/carboidrato vegetal ou leguminosa.
 */
@DisplayName("MealPlanEngine — geração integrada CARNIVORA (Task 2.8F)")
class MealPlanEngineCarnivoraTest {

    // Origem animal permitida (proteínas de almoço/jantar)
    private static final Set<String> ANIMAL_PROTEINS =
            Set.of("Carne Bovina", "Frango Grelhado", "Salmao", "Lombo Suino");
    // Origem animal permitida no café (laticínios — DAIRY passa no filtro CARNIVORA)
    private static final Set<String> ANIMAL_BREAKFAST = Set.of("Iogurte Natural", "Queijo Minas");
    // Intrusos que o filtro CARNIVORA deve remover: vegetal, fruta, grão, leguminosa e embutido
    private static final Set<String> NON_CARNIVORA =
            Set.of("Arroz", "Brocolis", "Banana", "Feijao Carioca", "Linguica");

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

        // Pipeline real: somente as fronteiras acima são mockadas.
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
        user.setEmail("carnivora@test.com");

        profile = new NutritionalProfile();
        profile.setGender(NutritionalProfile.Gender.MALE);
        profile.setWeightKg(80.0);
        profile.setHeightCm(180.0);
        profile.setActivityLevel(NutritionalProfile.ActivityLevel.MODERATE);
        profile.setDateOfBirth(LocalDate.now().minusYears(30));
    }

    @Test
    @DisplayName("Gera plano completo apenas com origem animal — sem vegetal, fruta, grão ou leguminosa")
    void carnivora_generatesAnimalOnlyPlan() {
        // given — pool misto: o filtro CARNIVORA roda de verdade sobre origem animal vs. intrusos vegetais
        Assessment assessment = buildAssessment();
        when(userService.findByEmail("carnivora@test.com")).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
        when(calorieCalculator.calculateDailyCalorieTarget(any(), any(), anyInt())).thenReturn(2000.0);
        when(foodRepository.findByActiveTrue()).thenReturn(mixedPool());

        // when — fluxo real ponta a ponta (filtro → agrupamento → composição → selector)
        DailyMealPlan plan = engine.generatePlan("carnivora@test.com");

        // then — plano não é parcial: toda refeição tem pelo menos 1 opção válida e não vazia.
        // O engine garante ≥1 opção por refeição (filtra subcalóricas); pools com apenas 1 food
        // adequada ao piso calórico (ex: laticínio gordo para BREAKFAST_CARNIVORA) resultam
        // corretamente em 1 opção — isso é comportamento esperado, não falha.
        assertThat(plan.meals()).isNotEmpty();
        for (MealPlanMeal meal : plan.meals()) {
            assertThat(meal.options()).isNotEmpty();
            for (MealOption option : meal.options()) {
                assertThat(option.items()).isNotEmpty();
            }
        }

        // nenhum intruso vegetal/fruta/grão/leguminosa/embutido vaza em nenhuma refeição
        assertThat(allItemNames(plan)).doesNotContainAnyElementsOf(NON_CARNIVORA);

        // almoço e jantar: composição CARNIVORA usa apenas MAIN_PROTEIN animal
        assertMainMealIsAnimalProteinOnly(mealByType(plan, "LUNCH"));
        assertMainMealIsAnimalProteinOnly(mealByType(plan, "DINNER"));

        // café: somente laticínio animal (prova que DAIRY é aceito e que nada vegetal entra)
        for (MealOption option : mealByType(plan, "BREAKFAST").options()) {
            assertThat(itemNames(option)).isSubsetOf(ANIMAL_BREAKFAST);
        }
    }

    /** Cada opção da refeição principal contém somente proteína animal (slot único MAIN_PROTEIN). */
    private void assertMainMealIsAnimalProteinOnly(MealPlanMeal meal) {
        for (MealOption option : meal.options()) {
            Set<String> names = itemNames(option);
            assertThat(names).isNotEmpty();
            assertThat(names).isSubsetOf(ANIMAL_PROTEINS);
        }
    }

    private Assessment buildAssessment() {
        Assessment a = new Assessment();
        a.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        a.setDietType(Assessment.DietType.CARNIVORA);
        a.setMealsPerDay(3);
        return a;
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

    /**
     * Pool misto: proteínas animais (almoço/jantar), laticínios (café) e intrusos que devem ser
     * filtrados — grão (CARBOHYDRATE), vegetal (VEGETABLE), fruta (FRUIT), leguminosa (LEGUME) e
     * embutido (PROTEIN/EMBUTIDOS, excluído por subcategoria, não por nome).
     */
    private List<Food> mixedPool() {
        return List.of(
                animalFood(1, "Carne Bovina", "CARNES_BOVINAS", 250, "LUNCH,DINNER"),
                animalFood(2, "Frango Grelhado", "AVES", 165, "LUNCH,DINNER"),
                animalFood(3, "Salmao", "PEIXES_E_FRUTOS_DO_MAR", 200, "LUNCH,DINNER"),
                animalFood(4, "Lombo Suino", "CARNES_SUINAS", 210, "LUNCH,DINNER"),
                dairyFood(5, "Iogurte Natural", 60, "BREAKFAST"),
                dairyFood(6, "Queijo Minas", 240, "BREAKFAST"),
                // intrusos — devem ser removidos pelo filtro CARNIVORA antes da seleção
                intruder(7, "Arroz", "CARBOHYDRATE", "CEREAIS_E_GRAOS", FoodRole.MAIN_CARBOHYDRATE, 130, "LUNCH,DINNER"),
                intruder(8, "Brocolis", "VEGETABLE", "LEGUMES_E_VERDURAS", FoodRole.VEGETABLE_LEGUME, 35, "LUNCH,DINNER"),
                intruder(9, "Banana", "FRUIT", "FRUTAS", FoodRole.FRUIT, 90, "BREAKFAST"),
                intruder(10, "Feijao Carioca", "LEGUME", "LEGUMINOSAS", FoodRole.VEGETABLE_LEGUME, 76, "LUNCH,DINNER"),
                intruder(11, "Linguica", "PROTEIN", "EMBUTIDOS", FoodRole.MAIN_PROTEIN, 290, "LUNCH,DINNER"));
    }

    private Food animalFood(int id, String name, String subcategory, double caloriesPer100g, String suitableMeals) {
        return buildFood(id, name, "PROTEIN", subcategory, FoodRole.MAIN_PROTEIN, caloriesPer100g, suitableMeals);
    }

    private Food dairyFood(int id, String name, double caloriesPer100g, String suitableMeals) {
        return buildFood(id, name, "DAIRY", "LATICINIOS_E_OVOS", FoodRole.LIGHT_DAIRY, caloriesPer100g, suitableMeals);
    }

    private Food intruder(int id, String name, String category, String subcategory,
                          FoodRole role, double caloriesPer100g, String suitableMeals) {
        return buildFood(id, name, category, subcategory, role, caloriesPer100g, suitableMeals);
    }

    private Food buildFood(int id, String name, String category, String subcategory,
                           FoodRole role, double caloriesPer100g, String suitableMeals) {
        Food f = new Food();
        f.setId(id);
        f.setName(name);
        f.setDisplayName(name);
        f.setCategory(category);
        f.setSubcategory(subcategory);
        f.setFoodRole(role);
        f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
        f.setProteinPer100g(BigDecimal.valueOf(20));
        f.setCarbsPer100g(BigDecimal.valueOf(5));
        f.setFatPer100g(BigDecimal.valueOf(10));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        // Main-meal protein portion sized so every animal protein clears the lunch calorie floor
        // (0.60 × 800 = 480 kcal): the leanest here (Frango, 165 kcal/100g) yields 577 kcal at 350 g.
        f.setReferencePortionGrams(BigDecimal.valueOf(350));
        f.setSuitableMeals(suitableMeals);
        f.setActive(true);
        return f;
    }
}
