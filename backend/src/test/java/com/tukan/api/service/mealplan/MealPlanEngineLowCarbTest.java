package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.repository.AssessmentRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes comportamentais para suporte a LOW_CARB no MealPlanEngine.
 *
 * <p>Verifica indiretamente o comportamento de resolveCompositionKey: confirma que
 * buildTwoOptions é chamado com a chave de composição correta dependendo do DietType.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealPlanEngine — suporte LOW_CARB (Task 2.8C)")
class MealPlanEngineLowCarbTest {

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
    // only needs to clear the floor; these tests assert the composition key, not calorie coverage.
    private static final double ABOVE_FLOOR_CALORIES = 1000.0;

    private FoodSelector.MealOptionBuild emptyOption(int number) {
        return new FoodSelector.MealOptionBuild(number, List.of(), ABOVE_FLOOR_CALORIES, 0.0, 0.0, 0.0);
    }

    private void stubFullPipeline(Assessment assessment, Map<String, Double> distribution,
                                   Map<String, List<Food>> grouped) {
        when(userService.findByEmail("test@test.com")).thenReturn(user);
        when(profileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(assessmentRepository.findByUserId(1)).thenReturn(Optional.of(assessment));
        when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(1800.0);
        when(mealDistributor.distribute(1800.0, 3)).thenReturn(distribution);

        List<Food> eligible = grouped.values().stream().flatMap(List::stream).distinct().toList();
        when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
        when(foodCurationService.curate(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(foodFilterService.groupByMealType(eligible)).thenReturn(grouped);
        when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                .thenReturn(List.of(emptyOption(1), emptyOption(2)));
    }

    @Nested
    @DisplayName("resolveCompositionKey — comportamento via buildTwoOptions")
    class ResolveCompositionKey {

        @Test
        @DisplayName("LOW_CARB: buildTwoOptions chamado com LUNCH_LOW_CARB para refeição LUNCH")
        void lowCarb_lunch_usesLunchLowCarbKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.LOW_CARB);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues()).contains("LUNCH_LOW_CARB");
        }

        @Test
        @DisplayName("LOW_CARB: buildTwoOptions chamado com DINNER_LOW_CARB para refeição DINNER")
        void lowCarb_dinner_usesDinnerLowCarbKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.LOW_CARB);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues()).contains("DINNER_LOW_CARB");
        }

        @Test
        @DisplayName("ONIVORA: buildTwoOptions chamado com LUNCH (não LUNCH_LOW_CARB) — regressão")
        void onivora_lunch_usesStandardLunchKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.ONIVORA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("LUNCH")
                    .doesNotContain("LUNCH_LOW_CARB");
        }

        @Test
        @DisplayName("dietType null: LUNCH e DINNER não são alterados — sem NPE nem fallback LOW_CARB")
        void nullDietType_lunch_usesStandardLunchKey() {
            // given
            Assessment assessment = buildAssessment(null);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("LUNCH", "DINNER")
                    .doesNotContain("LUNCH_LOW_CARB", "DINNER_LOW_CARB");
        }

        @Test
        @DisplayName("CETOGENICA: buildTwoOptions chamado com LUNCH_CETOGENICA para refeição LUNCH")
        void keto_lunch_usesLunchKetoKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.CETOGENICA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("LUNCH_CETOGENICA")
                    .doesNotContain("LUNCH_LOW_CARB");
        }

        @Test
        @DisplayName("CETOGENICA: buildTwoOptions chamado com DINNER_CETOGENICA para refeição DINNER")
        void keto_dinner_usesDinnerKetoKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.CETOGENICA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("DINNER_CETOGENICA")
                    .doesNotContain("DINNER_LOW_CARB");
        }

        @Test
        @DisplayName("CETOGENICA: BREAKFAST resolve para BREAKFAST_CETOGENICA (Task 2.8H.1)")
        void keto_breakfast_usesKetoBreakfastKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.CETOGENICA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("BREAKFAST_CETOGENICA")
                    .doesNotContain("BREAKFAST");
        }

        @Test
        @DisplayName("CARNIVORA: buildTwoOptions chamado com LUNCH_CARNIVORA para refeição LUNCH")
        void carnivora_lunch_usesLunchCarnivoraKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.CARNIVORA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("LUNCH_CARNIVORA")
                    .doesNotContain("LUNCH_CETOGENICA", "LUNCH_LOW_CARB");
        }

        @Test
        @DisplayName("CARNIVORA: buildTwoOptions chamado com DINNER_CARNIVORA para refeição DINNER")
        void carnivora_dinner_usesDinnerCarnivoraKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.CARNIVORA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("DINNER_CARNIVORA")
                    .doesNotContain("DINNER_CETOGENICA", "DINNER_LOW_CARB");
        }

        @Test
        @DisplayName("CARNIVORA: BREAKFAST resolve para BREAKFAST_CARNIVORA (Task 2.8H.1)")
        void carnivora_breakfast_usesCarnivoraBreakfastKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.CARNIVORA);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues())
                    .contains("BREAKFAST_CARNIVORA")
                    .doesNotContain("BREAKFAST");
        }

        @Test
        @DisplayName("LOW_CARB: BREAKFAST não é alterado — buildTwoOptions recebe BREAKFAST")
        void lowCarb_breakfast_usesStandardBreakfastKey() {
            // given
            Assessment assessment = buildAssessment(Assessment.DietType.LOW_CARB);
            Food food = buildFood(1, "BREAKFAST,LUNCH,DINNER");
            stubFullPipeline(assessment,
                    Map.of("BREAKFAST", 450.0, "LUNCH", 750.0, "DINNER", 600.0),
                    Map.of("BREAKFAST", List.of(food), "LUNCH", List.of(food), "DINNER", List.of(food)));

            // when
            engine.generatePlan("test@test.com");

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(foodSelector, org.mockito.Mockito.atLeastOnce())
                    .buildTwoOptions(anyList(), keyCaptor.capture(), anyDouble());
            assertThat(keyCaptor.getAllValues()).contains("BREAKFAST");
        }
    }
}
