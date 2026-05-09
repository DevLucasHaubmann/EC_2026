package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tukan.api.dto.mealplan.DailyMealPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanEngineValidationTest {

    @Mock private UserService userService;
    @Mock private NutritionalProfileRepository profileRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private CalorieCalculator calorieCalculator;
    @Mock private FoodFilterService foodFilterService;
    @Mock private FoodCurationService foodCurationService;
    @Mock private MealSuitabilityService mealSuitabilityService;
    @Mock private FoodDisplayNameService foodDisplayNameService;
    @Mock private MealDistributor mealDistributor;
    @Mock private FoodSelector foodSelector;

    @InjectMocks
    private MealPlanEngine engine;

    private User user;
    private NutritionalProfile profile;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("test@test.com");

        profile = new NutritionalProfile();
        profile.setGender(NutritionalProfile.Gender.MALE);
        profile.setWeightKg(80.0);
        profile.setHeightCm(175.0);
        profile.setActivityLevel(NutritionalProfile.ActivityLevel.MODERATE);
        profile.setDateOfBirth(LocalDate.now().minusYears(30));

        assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
    }

    private void stubUserLoading() {
        when(userService.findByEmail("test@test.com")).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
    }

    private void stubCurationPassThrough() {
        when(foodCurationService.curate(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Food createFood(int id, String name, String suitableMeals) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCategory("PROTEIN");
        food.setCaloriesPer100g(BigDecimal.valueOf(150));
        food.setProteinPer100g(BigDecimal.valueOf(20));
        food.setCarbsPer100g(BigDecimal.valueOf(5));
        food.setFatPer100g(BigDecimal.valueOf(7));
        food.setFiberPer100g(BigDecimal.valueOf(0));
        food.setReferencePortionGrams(BigDecimal.valueOf(100));
        food.setSuitableMeals(suitableMeals);
        food.setActive(true);
        return food;
    }

    // Cria um MealOptionBuild mínimo para evitar NPE no toMealOption
    private FoodSelector.MealOptionBuild buildOption(int number) {
        return new FoodSelector.MealOptionBuild(number, List.of(), 0.0, 0.0, 0.0, 0.0);
    }

    private List<FoodSelector.MealOptionBuild> twoOptions() {
        return List.of(buildOption(1), buildOption(2));
    }

    @Nested
    @DisplayName("Geração por número de refeições")
    class GeracaoPorNumeroDeRefeicoes {

        @Test
        @DisplayName("3 refeições gera DailyMealPlan com BREAKFAST, LUNCH e DINNER")
        void tresRefeicoesGeraPlanCorreto() {
            assessment.setMealsPerDay(3);
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 3)).thenReturn(Map.of(
                    "BREAKFAST", 500.0, "LUNCH", 800.0, "DINNER", 700.0));

            Food food = createFood(1, "Frango", "BREAKFAST,LUNCH,DINNER");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            when(foodCurationService.curate(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of(
                    "BREAKFAST", List.of(food),
                    "LUNCH", List.of(food),
                    "DINNER", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(foodSelector.buildTwoOptions(anyList(), anyDouble())).thenReturn(twoOptions());

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(3);
            assertThat(plan.meals()).extracting("mealType")
                    .containsExactlyInAnyOrder("BREAKFAST", "LUNCH", "DINNER");
        }

        @Test
        @DisplayName("4 refeições gera DailyMealPlan com BREAKFAST, LUNCH, AFTERNOON_SNACK e DINNER")
        void quatroRefeicoesGeraPlanCorreto() {
            assessment.setMealsPerDay(4);
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4)).thenReturn(Map.of(
                    "BREAKFAST", 400.0, "LUNCH", 700.0,
                    "AFTERNOON_SNACK", 300.0, "DINNER", 600.0));

            Food food = createFood(1, "Frango", "BREAKFAST,LUNCH,AFTERNOON_SNACK,DINNER");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            when(foodCurationService.curate(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of(
                    "BREAKFAST", List.of(food),
                    "LUNCH", List.of(food),
                    "AFTERNOON_SNACK", List.of(food),
                    "DINNER", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(foodSelector.buildTwoOptions(anyList(), anyDouble())).thenReturn(twoOptions());

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(4);
            assertThat(plan.meals()).extracting("mealType")
                    .containsExactlyInAnyOrder("BREAKFAST", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }

        @Test
        @DisplayName("5 refeições gera DailyMealPlan incluindo MORNING_SNACK")
        void cincoRefeicoesGeraPlanComMorningSnack() {
            assessment.setMealsPerDay(5);
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 5)).thenReturn(Map.of(
                    "BREAKFAST", 400.0, "MORNING_SNACK", 200.0, "LUNCH", 700.0,
                    "AFTERNOON_SNACK", 200.0, "DINNER", 500.0));

            Food food = createFood(1, "Frango", "BREAKFAST,MORNING_SNACK,LUNCH,AFTERNOON_SNACK,DINNER");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            when(foodCurationService.curate(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of(
                    "BREAKFAST", List.of(food),
                    "MORNING_SNACK", List.of(food),
                    "LUNCH", List.of(food),
                    "AFTERNOON_SNACK", List.of(food),
                    "DINNER", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(foodSelector.buildTwoOptions(anyList(), anyDouble())).thenReturn(twoOptions());

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(5);
            assertThat(plan.meals()).extracting("mealType")
                    .containsExactlyInAnyOrder(
                            "BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER");
        }
    }

    @Nested
    @DisplayName("Validação de pool vazio")
    class PoolVazio {

        @Test
        @DisplayName("Pool global vazio lança BusinessException")
        void poolGlobalVazio() {
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30))
                    .thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4))
                    .thenReturn(java.util.Map.of(
                            "BREAKFAST", 400.0, "LUNCH", 700.0,
                            "AFTERNOON_SNACK", 300.0, "DINNER", 600.0));
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(List.of());
            when(foodCurationService.curate(anyList())).thenReturn(List.of());

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Não foi possível gerar um plano alimentar");
        }
    }

    @Nested
    @DisplayName("Validação de cobertura por refeição")
    class CoberturaPorRefeicao {

        @Test
        @DisplayName("Refeição sem alimentos lança BusinessException com nome da refeição")
        void refeicaoSemAlimentos() {
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30))
                    .thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4))
                    .thenReturn(java.util.Map.of(
                            "BREAKFAST", 400.0, "LUNCH", 700.0,
                            "AFTERNOON_SNACK", 300.0, "DINNER", 600.0));

            stubCurationPassThrough();

            // Alimentos existem no global, mas nenhum para LANCHE_TARDE
            List<Food> eligible = List.of(
                    createFood(1, "Frango", "LUNCH,DINNER"),
                    createFood(2, "Pão", "BREAKFAST")
            );
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            when(foodFilterService.groupByMealType(eligible)).thenReturn(java.util.Map.of(
                    "BREAKFAST", List.of(eligible.get(1)),
                    "LUNCH", List.of(eligible.get(0)),
                    "AFTERNOON_SNACK", List.of(),
                    "DINNER", List.of(eligible.get(0))
            ));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AFTERNOON_SNACK");
        }

        @Test
        @DisplayName("Múltiplas refeições vazias lista todas na mensagem")
        void multiplasRefeicoesSemAlimentos() {
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30))
                    .thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4))
                    .thenReturn(java.util.Map.of(
                            "BREAKFAST", 400.0, "LUNCH", 700.0,
                            "AFTERNOON_SNACK", 300.0, "DINNER", 600.0));

            stubCurationPassThrough();

            List<Food> eligible = List.of(createFood(1, "Frango", "LUNCH"));
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            when(foodFilterService.groupByMealType(eligible)).thenReturn(java.util.Map.of(
                    "BREAKFAST", List.of(),
                    "LUNCH", List.of(eligible.get(0)),
                    "AFTERNOON_SNACK", List.of(),
                    "DINNER", List.of()
            ));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Não foi possível gerar um plano alimentar");
        }
    }
}
