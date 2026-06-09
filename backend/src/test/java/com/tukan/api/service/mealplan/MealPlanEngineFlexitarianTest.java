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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes comportamentais para suporte a FLEXITARIANA no MealPlanEngine (Task 2.8D).
 *
 * <p>FLEXITARIANA não é alias de ONIVORA: o engine deve passar ao FoodSelector um
 * {@code selectionPriority} (Comparator) que prioriza alimentos vegetais, sem excluir carnes.
 * ONIVORA e demais dietas não recebem essa priorização (chamam a sobrecarga de 3 args).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanEngine — suporte FLEXITARIANA (Task 2.8D)")
class MealPlanEngineFlexitarianTest {

    @Mock private UserService userService;
    @Mock private NutritionalProfileRepository profileRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private CalorieCalculator calorieCalculator;
    @Mock private FoodFilterService foodFilterService;
    @Mock private FoodCurationService foodCurationService;
    @Mock private MealSuitabilityService mealSuitabilityService;
    @Mock private MealDistributor mealDistributor;
    @Mock private FoodSelector foodSelector;

    @InjectMocks
    private MealPlanEngine engine;

    private User user;
    private NutritionalProfile profile;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("test@test.com");

        profile = new NutritionalProfile();
        profile.setGender(NutritionalProfile.Gender.FEMALE);
        profile.setWeightKg(65.0);
        profile.setHeightCm(165.0);
        profile.setActivityLevel(NutritionalProfile.ActivityLevel.MODERATE);
        profile.setDateOfBirth(LocalDate.now().minusYears(30));
    }

    private Assessment buildAssessment(Assessment.DietType dietType) {
        Assessment a = new Assessment();
        a.setGoal(Assessment.NutritionalGoal.WEIGHT_LOSS);
        a.setDietType(dietType);
        a.setMealsPerDay(3);
        return a;
    }

    private Food buildFood(int id, String suitableMeals) {
        Food f = new Food();
        f.setId(id);
        f.setName("Food" + id);
        f.setCategory("PROTEIN");
        f.setCaloriesPer100g(BigDecimal.valueOf(150));
        f.setProteinPer100g(BigDecimal.valueOf(20));
        f.setCarbsPer100g(BigDecimal.valueOf(5));
        f.setFatPer100g(BigDecimal.valueOf(7));
        f.setFiberPer100g(BigDecimal.valueOf(0));
        f.setReferencePortionGrams(BigDecimal.valueOf(100));
        f.setSuitableMeals(suitableMeals);
        f.setActive(true);
        return f;
    }

    // Calories above the critical floor (0.60 × target) for every target used here (max 750 → 450), so
    // the engine's subcaloric guard accepts these stubbed meals. The selector is mocked, so the value
    // only needs to clear the floor; these tests assert the selection priority, not calorie coverage.
    private static final double ABOVE_FLOOR_CALORIES = 1000.0;

    private FoodSelector.MealOptionBuild emptyOption(int number) {
        return new FoodSelector.MealOptionBuild(number, List.of(), ABOVE_FLOOR_CALORIES, 0.0, 0.0, 0.0);
    }

    private void stubCommonPipeline(Assessment assessment) {
        Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
        Map<String, List<Food>> grouped = Map.of(
                "BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food));

        when(userService.findByEmail("test@test.com")).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
        when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(1800.0);
        when(mealDistributor.distribute(1800.0, 3))
                .thenReturn(Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0));

        List<Food> eligible = List.of(food);
        when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
        when(foodCurationService.curate(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(foodFilterService.groupByMealType(eligible)).thenReturn(grouped);
        when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("FLEXITARIANA: buildTwoOptions recebe selectionPriority não-nulo")
    void flexitariana_passesNonNullSelectionPriority() {
        // given
        Assessment assessment = buildAssessment(Assessment.DietType.FLEXITARIANA);
        stubCommonPipeline(assessment);
        when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble(), any()))
                .thenReturn(List.of(emptyOption(1), emptyOption(2)));

        // when
        engine.generatePlan("test@test.com");

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Comparator<Food>> captor = ArgumentCaptor.forClass(Comparator.class);
        verify(foodSelector, atLeastOnce())
                .buildTwoOptions(anyList(), anyString(), anyDouble(), captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("FLEXITARIANA: comparator ordena alimento vegetariano antes de carne")
    void flexitariana_comparatorRanksPlantBeforeMeat() {
        // given
        Assessment assessment = buildAssessment(Assessment.DietType.FLEXITARIANA);
        stubCommonPipeline(assessment);
        when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble(), any()))
                .thenReturn(List.of(emptyOption(1), emptyOption(2)));

        // when
        engine.generatePlan("test@test.com");

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Comparator<Food>> captor = ArgumentCaptor.forClass(Comparator.class);
        verify(foodSelector, atLeastOnce())
                .buildTwoOptions(anyList(), anyString(), anyDouble(), captor.capture());

        Comparator<Food> priority = captor.getValue();
        Food vegetal = new Food();
        vegetal.setVegetarian(true);
        Food carne = new Food();
        carne.setVegetarian(false);
        assertThat(priority.compare(vegetal, carne)).isLessThan(0);
        assertThat(priority.compare(carne, vegetal)).isGreaterThan(0);
    }

    @Test
    @DisplayName("ONIVORA: não recebe priorização flexitariana — usa sobrecarga de 3 args")
    void onivora_doesNotReceiveFlexitarianPriority() {
        // given
        Assessment assessment = buildAssessment(Assessment.DietType.ONIVORA);
        stubCommonPipeline(assessment);
        when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                .thenReturn(List.of(emptyOption(1), emptyOption(2)));

        // when
        engine.generatePlan("test@test.com");

        // then — a sobrecarga com selectionPriority nunca é chamada
        verify(foodSelector, never())
                .buildTwoOptions(anyList(), anyString(), anyDouble(), any());
        verify(foodSelector, atLeastOnce())
                .buildTwoOptions(anyList(), anyString(), anyDouble());
    }

    /**
     * Teste de geração real (Task 2.8D — blocker 1 do QA): exercita o fluxo de seleção completo
     * com {@link FoodSelector}, {@link FoodFilterService}, {@link FoodCurationService},
     * {@link MealSuitabilityService}, {@link MealDistributor} e {@code MealCompositionRules} REAIS.
     * Apenas os limites externos (repositórios, usuário e cálculo calórico) são mockados.
     *
     * <p>Prova, sem mockar o FoodSelector, que FLEXITARIANA com pool misto (proteína vegetal +
     * animal): gera plano completo, prioriza a proteína vegetal e mantém a carne como alternativa.
     */
    @Nested
    @DisplayName("Geração real com pool misto (FoodSelector + FoodFilterService reais)")
    class RealGeneration {

        private static final Set<String> PROTEINS = Set.of("Tofu", "Frango");
        private static final Set<String> CARBS = Set.of("Arroz", "Batata");
        private static final Set<String> VEGETABLES = Set.of("Brocolis", "Cenoura");

        private MealPlanEngine realEngine;
        private FoodRepository foodRepository;
        private UserService realUserService;
        private NutritionalProfileRepository realProfileRepository;
        private AssessmentRepository realAssessmentRepository;
        private CalorieCalculator realCalorieCalculator;

        @BeforeEach
        void setUpRealPipeline() {
            foodRepository = mock(FoodRepository.class);
            realUserService = mock(UserService.class);
            realProfileRepository = mock(NutritionalProfileRepository.class);
            realAssessmentRepository = mock(AssessmentRepository.class);
            realCalorieCalculator = mock(CalorieCalculator.class);

            realEngine = new MealPlanEngine(
                    realUserService,
                    realProfileRepository,
                    realAssessmentRepository,
                    realCalorieCalculator,
                    new FoodFilterService(foodRepository),
                    new FoodCurationService(),
                    new MealSuitabilityService(),
                    new MealDistributor(),
                    new FoodSelector());
        }

        @Test
        @DisplayName("Gera plano completo: prioriza proteína vegetal e mantém carne como alternativa")
        void flexitariana_generatesCompletePlan_plantPrioritizedMeatAsFallback() {
            // given — pool misto: proteína vegetal (Tofu) e animal (Frango), com 2 carbos e 2 vegetais
            // para que ambas as opções de almoço possam ser estruturalmente completas e distintas.
            Assessment assessment = buildAssessment(Assessment.DietType.FLEXITARIANA);
            when(realUserService.findByEmail("test@test.com")).thenReturn(user);
            when(realProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
            when(realAssessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
            when(realCalorieCalculator.calculateDailyCalorieTarget(any(), any(), anyInt()))
                    .thenReturn(2000.0);
            when(foodRepository.findByActiveTrue()).thenReturn(mixedPool());

            // when — fluxo real ponta a ponta da seleção
            DailyMealPlan plan = realEngine.generatePlan("test@test.com");

            // then — nenhuma refeição vazia: toda refeição tem pelo menos 1 opção válida e não vazia.
            // O engine garante ≥1 opção por refeição; pools com apenas 1 food adequada ao piso calórico
            // (ex: Aveia 760 kcal como único item BREAKFAST com porção 200g, Banana 180 kcal subcalórica)
            // resultam em 1 opção — correto. LUNCH mantém 2 opções (Tofu + Frango), testado abaixo.
            assertThat(plan.meals()).isNotEmpty();
            for (MealPlanMeal meal : plan.meals()) {
                assertThat(meal.options()).isNotEmpty();
                for (MealOption option : meal.options()) {
                    assertThat(option.items()).isNotEmpty();
                }
            }

            MealPlanMeal lunch = plan.meals().stream()
                    .filter(m -> m.mealType().equals("LUNCH"))
                    .findFirst().orElseThrow();

            Set<String> option1 = itemNames(optionByNumber(lunch, 1));
            Set<String> option2 = itemNames(optionByNumber(lunch, 2));

            // proteína vegetal priorizada na opção 1; carne não entra na primeira escolha
            assertThat(option1).contains("Tofu").doesNotContain("Frango");

            // carne permanece elegível como alternativa (opção 2) — fallback, não excluída.
            // Determinístico porque o pool tem exatamente 1 proteína vegetal: Tofu é excluído
            // (usado na opção 1), restando Frango como único candidato ao slot MAIN_PROTEIN.
            assertThat(option2).contains("Frango");

            // ambas as opções são refeições principais completas (proteína + carbo + vegetal)
            assertCompleteMainMeal(option1);
            assertCompleteMainMeal(option2);
        }

        private void assertCompleteMainMeal(Set<String> names) {
            assertThat(names).anyMatch(PROTEINS::contains);
            assertThat(names).anyMatch(CARBS::contains);
            assertThat(names).anyMatch(VEGETABLES::contains);
        }

        private Set<String> itemNames(MealOption option) {
            return option.items().stream()
                    .map(MealPlanFoodItem::name)
                    .collect(Collectors.toSet());
        }

        private MealOption optionByNumber(MealPlanMeal meal, int number) {
            return meal.options().stream()
                    .filter(o -> o.optionNumber() == number)
                    .findFirst().orElseThrow();
        }

        private List<Food> mixedPool() {
            return List.of(
                    mealFood(1, "Tofu", "PROTEIN", FoodRole.MAIN_PROTEIN, true, 150, "LUNCH,DINNER"),
                    mealFood(2, "Frango", "PROTEIN", FoodRole.MAIN_PROTEIN, false, 165, "LUNCH,DINNER"),
                    mealFood(3, "Arroz", "CARBOHYDRATE", FoodRole.MAIN_CARBOHYDRATE, true, 130, "LUNCH,DINNER"),
                    mealFood(4, "Batata", "CARBOHYDRATE", FoodRole.MAIN_CARBOHYDRATE, true, 140, "LUNCH,DINNER"),
                    mealFood(5, "Brocolis", "VEGETABLE", FoodRole.VEGETABLE_LEGUME, true, 35, "LUNCH,DINNER"),
                    mealFood(6, "Cenoura", "VEGETABLE", FoodRole.VEGETABLE_LEGUME, true, 40, "LUNCH,DINNER"),
                    mealFood(7, "Aveia", "CARBOHYDRATE", FoodRole.BREAKFAST_LIGHT, true, 380, "BREAKFAST"),
                    mealFood(8, "Banana", "FRUIT", FoodRole.FRUIT, true, 90, "BREAKFAST"));
        }

        private Food mealFood(int id, String name, String category, FoodRole role,
                              boolean vegetarian, double caloriesPer100g, String suitableMeals) {
            Food f = new Food();
            f.setId(id);
            f.setName(name);
            f.setDisplayName(name);
            f.setCategory(category);
            f.setFoodRole(role);
            f.setVegetarian(vegetarian);
            f.setVegan(vegetarian);
            f.setCaloriesPer100g(BigDecimal.valueOf(caloriesPer100g));
            f.setProteinPer100g(BigDecimal.valueOf(10));
            f.setCarbsPer100g(BigDecimal.valueOf(10));
            f.setFatPer100g(BigDecimal.valueOf(5));
            f.setFiberPer100g(BigDecimal.valueOf(2));
            // Main-meal portion sized so both lunch options (protein + carb + vegetable) clear the
            // lunch calorie floor (0.60 × 800 = 480 kcal): the lighter Tofu option yields ~630 kcal.
            f.setReferencePortionGrams(BigDecimal.valueOf(200));
            f.setSuitableMeals(suitableMeals);
            f.setActive(true);
            return f;
        }
    }
}
