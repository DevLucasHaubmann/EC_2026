package com.tukan.api.service.mealplan;

import com.tukan.api.dto.ai.AiProfileContext;
import com.tukan.api.dto.ai.AiAssessmentContext;
import com.tukan.api.dto.mealplan.*;
import com.tukan.api.dto.mealplan.MealPlanContext.EligibleFoodSummary;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.exception.IncompleteProfileException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orquestra a geração do plano alimentar diário.
 *
 * Responsabilidades:
 * 1. Carregar perfil e triagem do usuário
 * 2. Calcular meta calórica via CalorieCalculator
 * 3. Distribuir kcal entre refeições via MealDistributor
 * 4. Filtrar alimentos elegíveis via FoodFilterService
 * 5. Selecionar alimentos com variedade via FoodSelector
 * 6. Montar DailyMealPlan completo (4 refeições × 2 opções)
 * 7. Montar MealPlanContext compacto para envio à IA
 */
@Service
@RequiredArgsConstructor
public class MealPlanEngine {

    private final UserService userService;
    private final NutritionalProfileRepository profileRepository;
    private final AssessmentRepository assessmentRepository;
    private final CalorieCalculator calorieCalculator;
    private final FoodFilterService foodFilterService;
    private final FoodCurationService foodCurationService;
    private final MealSuitabilityService mealSuitabilityService;
    private final FoodDisplayNameService foodDisplayNameService;
    private final MealDistributor mealDistributor;
    private final FoodSelector foodSelector;

    private static final int MAX_FOODS_PER_MEAL_CONTEXT = 30;

    @Transactional(readOnly = true)
    public DailyMealPlan generatePlan(String authenticatedEmail) {
        UserProfileData data = loadUserData(authenticatedEmail);

        double dailyCalories = calorieCalculator.calculateDailyCalorieTarget(
                data.profile, data.assessment, data.age);
        Map<String, Double> distribution = mealDistributor.distribute(dailyCalories);

        List<Food> eligible = foodFilterService.findEligibleFoods(data.assessment);
        eligible = foodCurationService.curate(eligible);
        validateEligibleFoods(eligible);

        Map<String, List<Food>> grouped = foodFilterService.groupByMealType(eligible);
        validateMealCoverage(grouped, distribution);

        List<MealPlanMeal> meals = new ArrayList<>();
        for (var entry : distribution.entrySet()) {
            String mealType = entry.getKey();
            double target = entry.getValue();
            List<Food> mealFoods = grouped.getOrDefault(mealType, List.of());
            mealFoods = mealSuitabilityService.filterAndPrioritize(mealFoods, mealType);

            List<FoodSelector.MealOptionBuild> options = foodSelector.buildTwoOptions(mealFoods, target);
            meals.add(toMealPlanMeal(mealType, target, options));
        }

        return new DailyMealPlan(Math.round(dailyCalories), data.assessment.getGoal().name(), meals);
    }

    @Transactional(readOnly = true)
    public MealPlanContext buildContext(String authenticatedEmail) {
        UserProfileData data = loadUserData(authenticatedEmail);

        double dailyCalories = calorieCalculator.calculateDailyCalorieTarget(
                data.profile, data.assessment, data.age);
        Map<String, Double> distribution = mealDistributor.distribute(dailyCalories);

        List<Food> eligible = foodFilterService.findEligibleFoods(data.assessment);
        eligible = foodCurationService.curate(eligible);
        validateEligibleFoods(eligible);

        Map<String, List<Food>> grouped = foodFilterService.groupByMealType(eligible);
        validateMealCoverage(grouped, distribution);
        Map<String, List<EligibleFoodSummary>> foodsByMeal = buildFoodSummaries(grouped);

        AiProfileContext profileCtx = new AiProfileContext(
                data.profile.getGender().name(),
                data.age,
                data.profile.getWeightKg(),
                data.profile.getHeightCm(),
                data.profile.getActivityLevel().name()
        );

        AiAssessmentContext assessmentCtx = buildAssessmentContext(data.assessment);

        return new MealPlanContext(profileCtx, assessmentCtx,
                Math.round(dailyCalories), distribution, foodsByMeal);
    }

    private UserProfileData loadUserData(String authenticatedEmail) {
        User user = userService.findByEmail(authenticatedEmail);

        NutritionalProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "perfil",
                        "Perfil nutricional não encontrado. Complete seu perfil antes de gerar um plano alimentar."));

        Assessment assessment = assessmentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "triagem",
                        "Triagem não encontrada. Complete sua triagem antes de gerar um plano alimentar."));

        int age = profile.calculateAge(LocalDate.now());
        if (age < 1) {
            throw new BusinessException("Idade inválida para cálculo nutricional.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return new UserProfileData(user, profile, assessment, age);
    }

    private void validateEligibleFoods(List<Food> eligible) {
        if (eligible.isEmpty()) {
            throw new BusinessException(
                    "Não foi possível gerar um plano alimentar com as restrições atuais do usuário.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateMealCoverage(Map<String, List<Food>> grouped, Map<String, Double> distribution) {
        List<String> emptyMeals = distribution.keySet().stream()
                .filter(meal -> grouped.getOrDefault(meal, List.of()).isEmpty())
                .toList();

        if (!emptyMeals.isEmpty()) {
            throw new BusinessException(
                    "Não foi possível gerar um plano alimentar com as restrições atuais do usuário. "
                            + "Sem alimentos disponíveis para: " + String.join(", ", emptyMeals) + ".",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private MealPlanMeal toMealPlanMeal(String mealType, double target,
                                         List<FoodSelector.MealOptionBuild> builds) {
        List<MealOption> options = builds.stream()
                .map(this::toMealOption)
                .toList();
        return new MealPlanMeal(mealType, target, options);
    }

    private MealOption toMealOption(FoodSelector.MealOptionBuild build) {
        List<MealPlanFoodItem> items = build.items().stream()
                .map(p -> new MealPlanFoodItem(
                        p.foodId(), p.name(),
                        foodDisplayNameService.generateDisplayName(p.name()),
                        p.category(), p.portionGrams(),
                        BigDecimal.valueOf(p.calories()),
                        BigDecimal.valueOf(p.protein()),
                        BigDecimal.valueOf(p.carbs()),
                        BigDecimal.valueOf(p.fat()),
                        BigDecimal.valueOf(p.fiber())))
                .toList();
        return new MealOption(build.optionNumber(), items,
                build.totalCalories(), build.totalProtein(),
                build.totalCarbs(), build.totalFat());
    }

    private Map<String, List<EligibleFoodSummary>> buildFoodSummaries(
            Map<String, List<Food>> grouped) {

        Map<String, List<EligibleFoodSummary>> result = new LinkedHashMap<>();

        for (var entry : grouped.entrySet()) {
            List<Food> foods = entry.getValue();
            List<Food> shuffled = new ArrayList<>(foods);
            Collections.shuffle(shuffled);

            List<EligibleFoodSummary> summaries = shuffled.stream()
                    .limit(MAX_FOODS_PER_MEAL_CONTEXT)
                    .map(this::toSummary)
                    .collect(Collectors.toList());

            result.put(entry.getKey(), summaries);
        }

        return result;
    }

    private EligibleFoodSummary toSummary(Food food) {
        return new EligibleFoodSummary(
                food.getId(),
                food.getName(),
                food.getCategory(),
                toDouble(food.getCaloriesPer100g()),
                toDouble(food.getProteinPer100g()),
                toDouble(food.getCarbsPer100g()),
                toDouble(food.getFatPer100g()),
                toDouble(food.getReferencePortionGrams())
        );
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private AiAssessmentContext buildAssessmentContext(Assessment assessment) {
        return new AiAssessmentContext(
                assessment.getGoal().name(),
                normalizeList(assessment.getDietaryRestrictions()),
                normalizeList(assessment.getAllergies()),
                normalizeList(assessment.getHealthConditions()),
                assessment.getMealsPerDay()
        );
    }

    private List<String> normalizeList(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.stream(value.split("[,;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private record UserProfileData(User user, NutritionalProfile profile,
                                    Assessment assessment, int age) {}
}
