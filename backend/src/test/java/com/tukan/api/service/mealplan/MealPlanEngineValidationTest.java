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
        assessment.setMealsPerDay(4);
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

    // Calories comfortably above the critical floor (0.60 × target) for every target used across these
    // tests (max target 800 → floor 480), so the engine's subcaloric guard accepts otherwise-valid
    // stubbed meals. The selector is mocked here, so the value only needs to clear the floor.
    private static final double SAFE_OPTION_CALORIES = 1000.0;

    // Cria um MealOptionBuild mínimo para evitar NPE no toMealOption
    private FoodSelector.MealOptionBuild buildOption(int number) {
        return new FoodSelector.MealOptionBuild(number, List.of(), SAFE_OPTION_CALORIES, 0.0, 0.0, 0.0);
    }

    private List<FoodSelector.MealOptionBuild> twoOptions() {
        return List.of(buildOption(1), buildOption(2));
    }

    private FoodSelector.MealOptionBuild optionWithCalories(int number, double calories) {
        return new FoodSelector.MealOptionBuild(number, List.of(), calories, 0.0, 0.0, 0.0);
    }

    private List<FoodSelector.MealOptionBuild> twoOptionsWithCalories(double calories) {
        return List.of(optionWithCalories(1, calories), optionWithCalories(2, calories));
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
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(twoOptions());

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
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(twoOptions());

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
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(twoOptions());

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
    @DisplayName("Validação de faixa etária")
    class ValidacaoDeIdade {

        @Test
        @DisplayName("Idade 121 anos lança BusinessException")
        void idadeAcimaDoLimite() {
            profile.setDateOfBirth(LocalDate.now().minusYears(121));
            stubUserLoading();

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Idade fora do intervalo");
        }

        @Test
        @DisplayName("Idade 0 anos (nascido hoje) lança BusinessException")
        void idadeAbaixoDoLimite() {
            profile.setDateOfBirth(LocalDate.now());
            stubUserLoading();

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Idade fora do intervalo");
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

    @Nested
    @DisplayName("Validação de número de refeições")
    class ValidacaoDeRefeicoes {

        @Test
        @DisplayName("mealsPerDay = 2 lança BusinessException")
        void mealsPerDayAbaixoDoLimite() {
            assessment.setMealsPerDay(2);
            stubUserLoading();

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("entre 3 e 5");
        }

        @Test
        @DisplayName("mealsPerDay = 6 lança BusinessException")
        void mealsPerDayAcimaDoLimite() {
            assessment.setMealsPerDay(6);
            stubUserLoading();

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("entre 3 e 5");
        }

        @Test
        @DisplayName("mealsPerDay = null usa padrão 4 e gera plano normalmente")
        void mealsPerDayNuloUsaPadraoDe4() {
            assessment.setMealsPerDay(null);
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
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(twoOptions());

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Fallback quando suitability esvazia a refeição (2.4F)")
    class SuitabilityFallback {

        @Test
        @DisplayName("filterAndPrioritize vazio: usa o pool da refeição e não lança exceção")
        void suitabilityVaziaUsaPoolDaRefeicao() {
            assessment.setMealsPerDay(3);
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 3)).thenReturn(Map.of(
                    "BREAKFAST", 500.0, "LUNCH", 800.0, "DINNER", 700.0));

            Food food = createFood(1, "Frango", "BREAKFAST,LUNCH,DINNER");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            stubCurationPassThrough();
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of(
                    "BREAKFAST", List.of(food),
                    "LUNCH", List.of(food),
                    "DINNER", List.of(food)));
            // suitability esvazia todas as refeições
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString())).thenReturn(List.of());
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(twoOptions());

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            // plano gerado normalmente, sem IllegalArgumentException
            assertThat(plan.meals()).hasSize(3);
            // buildTwoOptions deve receber o pool não-vazio, nunca a lista esvaziada pela suitability
            org.mockito.ArgumentCaptor<List<Food>> poolCaptor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            org.mockito.Mockito.verify(foodSelector, org.mockito.Mockito.times(3))
                    .buildTwoOptions(poolCaptor.capture(), anyString(), anyDouble());
            assertThat(poolCaptor.getAllValues()).allSatisfy(pool -> assertThat(pool).isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Task 2.8H.2 — Bloqueio de refeição severamente subcalórica")
    class Task28H2 {

        /** Stubs the full flow for a single LUNCH meal with the given target; selector is stubbed per test. */
        private void stubSingleLunch(double target) {
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4)).thenReturn(Map.of("LUNCH", target));
            Food food = createFood(1, "Frango", "LUNCH");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            stubCurationPassThrough();
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of("LUNCH", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Opção abaixo de 40% do alvo (hard floor) lança BusinessException com mealType")
        void opcaoSubcaloricaLancaBusinessException() {
            stubSingleLunch(800.0);
            // 200 kcal = 25% do alvo 800 — abaixo do hard floor de 40% (320 kcal) → falha
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(200.0));
            // Pool sem alimento denso: a suplementação main-meal não eleva acima do piso.
            when(foodSelector.supplementMainMeal(any(), anyList(), anyDouble(), any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH")
                    .hasMessageContaining("mínimo calórico");
        }

        @Test
        @DisplayName("50% do alvo é aceito via tier 3 (40%): a escada progressiva evita falha desnecessária")
        void planoA50PorCentoAceitoViaTier3() {
            // With the progressive floor ladder (70→55→40%), 50% of target clears tier 3 (40%).
            // Previously failed with fixed 60% floor. Now succeeds — this is intentional behaviour.
            stubSingleLunch(800.0);
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(400.0)); // 50% = above tier 3 floor (320)

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("Opção exatamente no tier 2 (55%) é válida — aceita via tier 2")
        void opcaoNoTier2Valida() {
            stubSingleLunch(800.0);
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(440.0)); // == 55% do alvo

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("Opção acima do tier 1 (70%) continua válida e gera plano")
        void opcaoAcimaDoPisoGeraPlano() {
            stubSingleLunch(800.0);
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(720.0)); // 90% do alvo

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("Slot obrigatório preenchido mas baixa caloria: falha por caloria, não por slot vazio")
        void slotPreenchidoFalhaPorCaloria() {
            stubSingleLunch(800.0);
            // 150 kcal = ~19% do alvo 800 — abaixo do hard floor de 40% (320 kcal)
            FoodSelector.FoodPortionBuild item = new FoodSelector.FoodPortionBuild(
                    1, "Frango", "Frango", "PROTEIN", BigDecimal.valueOf(100),
                    150.0, 20.0, 5.0, 7.0, 0.0);
            List<FoodSelector.MealOptionBuild> options = List.of(
                    new FoodSelector.MealOptionBuild(1, List.of(item), 150.0, 20.0, 5.0, 7.0),
                    new FoodSelector.MealOptionBuild(2, List.of(item), 150.0, 20.0, 5.0, 7.0));
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(options);
            // Pool sem alimento denso: a suplementação main-meal não eleva acima do piso.
            when(foodSelector.supplementMainMeal(any(), anyList(), anyDouble(), any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mínimo calórico")
                    .hasMessageNotContaining("slot");
        }

        @Test
        @DisplayName("VEGANA com pool abaixo do hard floor (40%) não retorna sucesso")
        void veganaSubcaloricaNaoRetornaSucesso() {
            assessment.setDietType(Assessment.DietType.VEGANA);
            stubSingleLunch(800.0);
            // 200 kcal = 25% — abaixo do hard floor 40%
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(200.0));
            // Pool sem alimento denso: a suplementação main-meal não eleva acima do piso.
            when(foodSelector.supplementMainMeal(any(), anyList(), anyDouble(), any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH");
        }

        @Test
        @DisplayName("FLEXITARIANA com pool abaixo do hard floor (40%) não retorna sucesso (overload com prioridade)")
        void flexitarianaSubcaloricaNaoRetornaSucesso() {
            assessment.setDietType(Assessment.DietType.FLEXITARIANA);
            stubSingleLunch(800.0);
            // FLEXITARIANA aciona o overload de 4 argumentos (Comparator de prioridade plant-first)
            // 200 kcal = 25% — abaixo do hard floor 40%
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble(), any()))
                    .thenReturn(twoOptionsWithCalories(200.0));
            // Pool sem alimento denso: a suplementação main-meal não eleva acima do piso.
            when(foodSelector.supplementMainMeal(any(), anyList(), anyDouble(), any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LUNCH");
        }

        @Test
        @DisplayName("Regressão ONIVORA: pool adequado continua gerando plano")
        void onivoraAdequadaContinuaGerando() {
            assessment.setDietType(Assessment.DietType.ONIVORA);
            stubSingleLunch(800.0);
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(760.0)); // 95% do alvo

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Task 2.8I.2 — filterValidOptions: opção secundária subcalórica descartada")
    class Task28I2 {

        /** Stubs the full flow for a single BREAKFAST meal with the given target. */
        private void stubSingleBreakfast(double target) {
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4)).thenReturn(Map.of("BREAKFAST", target));
            Food food = createFood(1, "Pao", "BREAKFAST");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            stubCurationPassThrough();
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of("BREAKFAST", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Opção 1 válida + opção 2 subcalórica: plano persiste com 1 opção renumerada")
        void opcao1ValidaOpcao2Subcalorica_persisteSo1Opcao() {
            stubSingleBreakfast(400.0);
            // option 1: 320 kcal (80% do alvo — acima do piso 60%=240). option 2: 50 kcal (subcalórica)
            List<FoodSelector.MealOptionBuild> mixedOptions = List.of(
                    optionWithCalories(1, 320.0),
                    optionWithCalories(2, 50.0)
            );
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(mixedOptions);

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(1);
            // Apenas 1 opção deve ser retornada (a válida)
            assertThat(plan.meals().get(0).options()).hasSize(1);
            // A opção sobrevivente deve ser renumerada como 1
            assertThat(plan.meals().get(0).options().get(0).optionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Ambas as opções válidas: plano persiste com 2 opções")
        void ambasOpcaoValidas_persisteComDuasOpcoes() {
            stubSingleBreakfast(400.0);
            // option 1: 300 kcal (75%), option 2: 320 kcal (80%) — ambas acima do piso (240)
            List<FoodSelector.MealOptionBuild> bothValid = List.of(
                    optionWithCalories(1, 300.0),
                    optionWithCalories(2, 320.0)
            );
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(bothValid);

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(1);
            assertThat(plan.meals().get(0).options()).hasSize(2);
            // Renumeração preserva ordem: 1, 2
            assertThat(plan.meals().get(0).options().get(0).optionNumber()).isEqualTo(1);
            assertThat(plan.meals().get(0).options().get(1).optionNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("BREAKFAST com ambas as opções abaixo de 40%: best-effort gera o plano (não lança)")
        void ambasSubcaloricas_breakfastBestEffortGeraPlano() {
            // Bug B1 (MVP): BREAKFAST subcalórico não pode derrubar o plano. Após a suplementação
            // falhar em atingir o piso, o engine aceita as melhores opções válidas (best-effort).
            stubSingleBreakfast(400.0);
            List<FoodSelector.MealOptionBuild> bothSubcaloric = List.of(
                    optionWithCalories(1, 50.0),
                    optionWithCalories(2, 50.0)
            );
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(bothSubcaloric);
            // Pool só tem alimentos subcalóricos: a suplementação não eleva acima do piso.
            when(foodSelector.supplementBreakfast(any(), anyList(), anyDouble()))
                    .thenAnswer(inv -> inv.getArgument(0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(1);
            assertThat(plan.meals().get(0).options()).hasSize(2);
            assertThat(plan.meals().get(0).options().get(0).optionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Opção 1 subcalórica + opção 2 válida: plano persiste com opção 2 renumerada como 1")
        void opcao1Subcalorica_opcao2ValidaRenumeradaComo1() {
            stubSingleBreakfast(400.0);
            // option 1: 50 kcal (subcalórica), option 2: 350 kcal (válida 87.5%)
            List<FoodSelector.MealOptionBuild> reversed = List.of(
                    optionWithCalories(1, 50.0),
                    optionWithCalories(2, 350.0)
            );
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(reversed);

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            assertThat(plan.meals()).hasSize(1);
            assertThat(plan.meals().get(0).options()).hasSize(1);
            // Opção sobrevivente (era 2, 350 kcal) renumerada como 1
            assertThat(plan.meals().get(0).options().get(0).optionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Opção exatamente no piso (60%) permanece válida — não é descartada")
        void opcaoExataNoP_iso60_permaneceValida() {
            stubSingleBreakfast(400.0);
            // 60% de 400 = 240 — exatamente no piso, deve passar
            List<FoodSelector.MealOptionBuild> onFloor = List.of(
                    optionWithCalories(1, 240.0),
                    optionWithCalories(2, 50.0)
            );
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(onFloor);

            DailyMealPlan plan = engine.generatePlan("test@test.com");

            // opção 240 passa (>= piso), opção 50 é descartada
            assertThat(plan.meals().get(0).options()).hasSize(1);
            assertThat(plan.meals().get(0).options().get(0).optionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Refeição não-BREAKFAST: exception contém calorias da melhor opção e o hard floor (40%)")
        void exceptionContemCalorias_melhoresOpcaoTentada() {
            // LUNCH/DINNER tentam suplementação (como snacks); se o pool não eleva ao piso, falham
            // com diagnóstico contendo a melhor caloria observada e o hard floor (40%).
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(2000.0);
            when(mealDistributor.distribute(2000.0, 4)).thenReturn(Map.of("LUNCH", 400.0));
            Food food = createFood(1, "Frango", "LUNCH");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            stubCurationPassThrough();
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of("LUNCH", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
            // ambas abaixo do hard floor (40% de 400 = 160): melhor = 100 kcal, piso reportado = 160
            List<FoodSelector.MealOptionBuild> botSubcaloric = List.of(
                    optionWithCalories(1, 80.0),
                    optionWithCalories(2, 100.0)
            );
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble())).thenReturn(botSubcaloric);
            // Pool sem alimento denso: a suplementação main-meal não eleva acima do piso.
            when(foodSelector.supplementMainMeal(any(), anyList(), anyDouble(), any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("100")  // melhor caloria observada
                    .hasMessageContaining("160"); // hard floor: 40% de 400
        }
    }

    @Nested
    @DisplayName("Threshold boundary — escada progressiva de pisos (70% → 55% → 40%)")
    class ThresholdBoundaryProgressiveLadder {

        // target = 1000 kcal para facilitar cálculo:
        //   tier 1 (standard):  700 kcal (70%)
        //   tier 2 (relaxed):   550 kcal (55%)
        //   tier 3 (minimum):   400 kcal (40%)
        //   hard failure:       < 400 kcal

        private void stubSingleLunch1000() {
            stubUserLoading();
            when(calorieCalculator.calculateDailyCalorieTarget(profile, assessment, 30)).thenReturn(4000.0);
            when(mealDistributor.distribute(4000.0, 4)).thenReturn(Map.of("LUNCH", 1000.0));
            Food food = createFood(1, "Frango", "LUNCH");
            List<Food> eligible = List.of(food);
            when(foodFilterService.findEligibleFoods(assessment)).thenReturn(eligible);
            stubCurationPassThrough();
            when(foodFilterService.groupByMealType(eligible)).thenReturn(Map.of("LUNCH", List.of(food)));
            when(mealSuitabilityService.filterAndPrioritize(anyList(), anyString()))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("39% do alvo (390 kcal) — abaixo do hard floor (40%) → lança BusinessException")
        void shouldRejectMealBelowHardFloor() {
            stubSingleLunch1000();
            // 390 kcal = 39% de 1000 — abaixo do tier 3 (400 kcal)
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(390.0));
            // Pool sem alimento denso: a suplementação main-meal não eleva acima do piso.
            when(foodSelector.supplementMainMeal(any(), anyList(), anyDouble(), any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> engine.generatePlan("test@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mínimo calórico");
        }

        @Test
        @DisplayName("40% do alvo (400 kcal) — exatamente no hard floor → aceito via tier 3")
        void shouldAcceptMealAtExactHardFloor() {
            stubSingleLunch1000();
            // 400 kcal = exatamente 40% de 1000 — tier 3 boundary
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(400.0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("50% do alvo (500 kcal) — entre tier 3 e tier 2 → aceito via tier 3")
        void shouldAcceptMealBetweenTier3AndTier2() {
            stubSingleLunch1000();
            // 500 kcal = 50% de 1000 — acima de 40%, abaixo de 55% → tier 3
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(500.0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("55% do alvo (550 kcal) — exatamente no tier 2 → aceito via tier 2")
        void shouldAcceptMealAtExactTier2() {
            stubSingleLunch1000();
            // 550 kcal = exatamente 55% de 1000
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(550.0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("65% do alvo (650 kcal) — entre tier 2 e tier 1 → aceito via tier 2")
        void shouldAcceptMealBetweenTier2AndTier1() {
            stubSingleLunch1000();
            // 650 kcal = 65% de 1000 — acima de 55%, abaixo de 70% → tier 2
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(650.0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("70% do alvo (700 kcal) — exatamente no tier 1 → aceito via tier 1 (standard)")
        void shouldAcceptMealAtExactTier1() {
            stubSingleLunch1000();
            // 700 kcal = exatamente 70% de 1000
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(700.0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }

        @Test
        @DisplayName("80% do alvo (800 kcal) — acima do tier 1 → aceito normalmente")
        void shouldAcceptMealAboveTier1() {
            stubSingleLunch1000();
            // 800 kcal = 80% de 1000
            when(foodSelector.buildTwoOptions(anyList(), anyString(), anyDouble()))
                    .thenReturn(twoOptionsWithCalories(800.0));

            DailyMealPlan plan = engine.generatePlan("test@test.com");
            assertThat(plan.meals()).hasSize(1);
        }
    }

}
