package com.tukan.api.service.mealplan;

import com.tukan.api.domain.mealcomposition.MealCompositionRule;
import com.tukan.api.domain.mealcomposition.MealCompositionRules;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    private final MealDistributor mealDistributor;
    private final FoodSelector foodSelector;

    private static final int MAX_FOODS_PER_MEAL_CONTEXT = 30;

    @Transactional(readOnly = true)
    public DailyMealPlan generatePlan(String authenticatedEmail) {
        UserProfileData data = loadUserData(authenticatedEmail);

        double dailyCalories = calorieCalculator.calculateDailyCalorieTarget(
                data.profile, data.assessment, data.age);
        int mealsPerDay = resolveMealsPerDay(data.assessment.getMealsPerDay());
        Map<String, Double> distribution = mealDistributor.distribute(dailyCalories, mealsPerDay);
        log.info("Meal plan generation started: targetCalories={}, meals={}, distribution={}",
                Math.round(dailyCalories), distribution.size(), roundedDistribution(distribution));

        List<Food> eligible = foodFilterService.findEligibleFoods(data.assessment);
        eligible = foodCurationService.curate(eligible);
        validateEligibleFoods(eligible);

        Map<String, List<Food>> grouped = foodFilterService.groupByMealType(eligible);
        validateMealCoverage(grouped, distribution);
        log.debug("Eligible foods after curation: total={}, perMeal={}",
                eligible.size(), eligibleCountsByMeal(grouped));

        List<MealPlanMeal> meals = new ArrayList<>();
        for (var entry : distribution.entrySet()) {
            String mealType = entry.getKey();
            double target = entry.getValue();
            List<Food> mealFoods = grouped.getOrDefault(mealType, List.of());
            List<Food> prioritized = mealSuitabilityService.filterAndPrioritize(mealFoods, mealType);
            // Suitability is a prioritization filter, not a coverage gate: it must not empty a meal
            // that validateMealCoverage already guaranteed non-empty. Fall back to the broader pool.
            if (prioritized.isEmpty()) {
                prioritized = mealFoods;
            }

            String compositionKey = resolveCompositionKey(mealType, data.assessment.getDietType());
            Comparator<Food> selectionPriority = resolveSelectionPriority(data.assessment.getDietType());
            List<FoodSelector.MealOptionBuild> options = selectionPriority != null
                    ? foodSelector.buildTwoOptions(prioritized, compositionKey, target, selectionPriority)
                    : foodSelector.buildTwoOptions(prioritized, compositionKey, target);
            List<FoodSelector.MealOptionBuild> validOptions = filterValidOptions(mealType, compositionKey, target, options, prioritized);
            meals.add(toMealPlanMeal(mealType, target, validOptions));
        }

        log.info("Meal plan generation completed: meals={}", meals.size());
        return new DailyMealPlan(Math.round(dailyCalories), data.assessment.getGoal().name(), meals);
    }

    @Transactional(readOnly = true)
    public MealPlanContext buildContext(String authenticatedEmail) {
        UserProfileData data = loadUserData(authenticatedEmail);

        double dailyCalories = calorieCalculator.calculateDailyCalorieTarget(
                data.profile, data.assessment, data.age);
        int mealsPerDay = resolveMealsPerDay(data.assessment.getMealsPerDay());
        Map<String, Double> distribution = mealDistributor.distribute(dailyCalories, mealsPerDay);

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
        if (age < 1 || age > 120) {
            throw new BusinessException("Idade fora do intervalo aceito para cálculo nutricional.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return new UserProfileData(user, profile, assessment, age);
    }

    /** Per-meal calorie targets rounded for logs — derived plan values, not raw biometrics. */
    private Map<String, Long> roundedDistribution(Map<String, Double> distribution) {
        Map<String, Long> rounded = new LinkedHashMap<>();
        distribution.forEach((meal, kcal) -> rounded.put(meal, Math.round(kcal)));
        return rounded;
    }

    /** Eligible food counts per meal type — counts only, never the food contents. */
    private Map<String, Integer> eligibleCountsByMeal(Map<String, List<Food>> grouped) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        grouped.forEach((meal, foods) -> counts.put(meal, foods.size()));
        return counts;
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

    /**
     * Filters meal options using a progressive calorie acceptance ladder (70 % → 55 % → 40 %),
     * then validates that at least one option survived. Subcaloric options are discarded before
     * persistence. Remaining options are renumbered 1..N so the caller never sees gaps.
     *
     * <p>The fallback relaxes ONLY the caloric gate. Composition structure (dietType, mealType,
     * required FoodRole slots, suitable_meals, and user restrictions) remains guaranteed because
     * options always come from the pool already filtered and composed by FoodSelector /
     * MealCompositionRule — this method never touches that chain.
     *
     * <p>If no option survives (best observed calories &lt; 40 % of target), a BusinessException
     * is thrown carrying the best observed calories and the hard floor (40 %) for diagnosis.
     */
    private List<FoodSelector.MealOptionBuild> filterValidOptions(
            String mealType, String compositionKey, double target,
            List<FoodSelector.MealOptionBuild> options, List<Food> mealPool) {

        MealCalorieFloorPolicy.AcceptanceResult result =
                MealCalorieFloorPolicy.acceptOptions(target, options);

        if (!result.accepted().isEmpty()) {
            logRelaxedTier(mealType, target, result);
            return renumber(result.accepted());
        }

        // No option reached the MVP minimum floor (40 %). BREAKFAST is allowed a best-effort
        // fallback so a subcaloric café never fails the whole plan. SNACKS (MORNING_SNACK /
        // AFTERNOON_SNACK) and LUNCH/DINNER attempt supplementation but fail hard if still below
        // floor — these slots must never be silently persisted subcaloric (lunch/dinner are the
        // largest caloric slots of the plan). Any other meal type fails immediately.
        if (isBreakfast(mealType)) {
            return breakfastBestEffort(mealType, target, options, mealPool);
        }

        if (isSnack(mealType)) {
            return snackBestEffort(mealType, target, options, mealPool);
        }

        if (isMainMeal(mealType)) {
            MealCompositionRule rule = MealCompositionRules.forCanonicalKey(compositionKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Composition rule ausente para main meal: " + compositionKey));
            return mainMealBestEffort(mealType, target, options, mealPool, rule);
        }

        double hardFloor = target * MealCalorieFloorPolicy.TIER_MINIMUM;
        throw new BusinessException(
                "Não foi possível montar " + mealType
                        + ": nenhuma opção atingiu o mínimo calórico após suplementação ("
                        + Math.round(bestCalories(options)) + " kcal, mínimo "
                        + Math.round(hardFloor) + " kcal).",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * BREAKFAST-only best-effort fallback (Bug B1 — MVP rule), reached only when no breakfast option
     * cleared the MVP minimum floor (40 %). First attempts compatible supplementation to reach the
     * floor; if it still falls short, the best valid options are accepted as-is so a subcaloric
     * breakfast never fails the plan. Only the caloric floor is relaxed — composition, dietType,
     * suitable_meals and user restrictions remain enforced by the pre-filtered pool. The empty-pool
     * case (no compatible food at all) is already rejected upstream by {@link #validateMealCoverage}.
     */
    private List<FoodSelector.MealOptionBuild> breakfastBestEffort(
            String mealType, double target, List<FoodSelector.MealOptionBuild> options,
            List<Food> mealPool) {

        double floor = target * MealCalorieFloorPolicy.TIER_MINIMUM;
        List<FoodSelector.MealOptionBuild> supplemented = options.stream()
                .map(option -> foodSelector.supplementBreakfast(option, mealPool, floor))
                .toList();

        MealCalorieFloorPolicy.AcceptanceResult retry =
                MealCalorieFloorPolicy.acceptOptions(target, supplemented);
        if (!retry.accepted().isEmpty()) {
            log.warn("BREAKFAST reached the MVP minimum floor only after compatible supplementation: "
                            + "targetKcal={}, bestKcal={}",
                    Math.round(target), Math.round(bestCalories(retry.accepted())));
            return renumber(retry.accepted());
        }

        // Extreme best-effort: accept the subcaloric breakfast rather than failing the whole plan.
        log.warn("BREAKFAST extreme best-effort fallback (subcaloric breakfast accepted): "
                        + "targetKcal={}, bestKcal={}, hardFloorKcal={}",
                Math.round(target), Math.round(bestCalories(supplemented)), Math.round(floor));
        return renumber(supplemented);
    }

    private void logRelaxedTier(String mealType, double target,
                                 MealCalorieFloorPolicy.AcceptanceResult result) {
        if (result.tierRatio() < MealCalorieFloorPolicy.TIER_STANDARD) {
            log.warn("Meal accepted via relaxed calorie floor: mealType={}, tierRatio={}, "
                            + "bestOptionKcal={}, targetKcal={}",
                    mealType, result.tierRatio(),
                    bestCalories(result.accepted()), Math.round(target));
        }
    }

    private double bestCalories(List<FoodSelector.MealOptionBuild> options) {
        return options.stream()
                .mapToDouble(FoodSelector.MealOptionBuild::totalCalories)
                .max().orElse(0.0);
    }

    /** Renumbers accepted options 1..N so the caller never sees gaps left by discarded options. */
    private List<FoodSelector.MealOptionBuild> renumber(List<FoodSelector.MealOptionBuild> options) {
        List<FoodSelector.MealOptionBuild> renumbered = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            FoodSelector.MealOptionBuild orig = options.get(i);
            renumbered.add(new FoodSelector.MealOptionBuild(
                    i + 1, orig.items(), orig.totalCalories(),
                    orig.totalProtein(), orig.totalCarbs(), orig.totalFat()));
        }
        return renumbered;
    }

    /**
     * Snack best-effort fallback (Bug B4), reached only when no snack option cleared the MVP
     * minimum floor (40 %). Attempts supplementation with snack-compatible roles to reach the
     * floor; if the pool is insufficient, throws a controlled BusinessException — unlike breakfast,
     * a snack that cannot reach the floor must NOT be persisted subcaloric.
     *
     * <p>Only the caloric floor is relaxed. Composition, dietType, suitable_meals and user
     * restrictions remain enforced by the pre-filtered pool.
     */
    private List<FoodSelector.MealOptionBuild> snackBestEffort(
            String mealType, double target, List<FoodSelector.MealOptionBuild> options,
            List<Food> mealPool) {

        double floor = target * MealCalorieFloorPolicy.TIER_MINIMUM;
        List<FoodSelector.MealOptionBuild> supplemented = options.stream()
                .map(option -> foodSelector.supplementSnack(option, mealPool, floor))
                .toList();

        MealCalorieFloorPolicy.AcceptanceResult retry =
                MealCalorieFloorPolicy.acceptOptions(target, supplemented);
        if (!retry.accepted().isEmpty()) {
            log.warn("SNACK reached the MVP minimum floor only after compatible supplementation: "
                            + "mealType={}, targetKcal={}, bestKcal={}",
                    mealType, Math.round(target), Math.round(bestCalories(retry.accepted())));
            return renumber(retry.accepted());
        }

        throw new BusinessException(
                "Não foi possível montar " + mealType
                        + ": nenhuma opção atingiu o mínimo calórico após suplementação ("
                        + Math.round(bestCalories(supplemented)) + " kcal, mínimo "
                        + Math.round(floor) + " kcal).",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Lunch/dinner best-effort fallback (Bug B4 extension), reached only when no main-meal option
     * cleared the MVP minimum floor (40 %). Eligible roles for supplementation are derived from
     * {@code rule.allowedRoles()} so that no role outside the effective composition contract
     * (e.g. MAIN_CARBOHYDRATE for LOW_CARB, VEGETABLE_LEGUME for CARNIVORA) is ever added.
     * If the pool is still insufficient after supplementation, throws a controlled
     * BusinessException — like a snack and unlike breakfast, a lunch/dinner that cannot reach
     * the floor must NOT be persisted subcaloric, since these are the largest caloric slots of
     * the plan.
     *
     * <p>Only the caloric floor is relaxed. Composition, dietType, suitable_meals and user
     * restrictions remain enforced by the pre-filtered pool and {@code rule}.
     */
    private List<FoodSelector.MealOptionBuild> mainMealBestEffort(
            String mealType, double target, List<FoodSelector.MealOptionBuild> options,
            List<Food> mealPool, MealCompositionRule rule) {

        double floor = target * MealCalorieFloorPolicy.TIER_MINIMUM;
        List<FoodSelector.MealOptionBuild> supplemented = options.stream()
                .map(option -> foodSelector.supplementMainMeal(option, mealPool, floor, rule))
                .toList();

        MealCalorieFloorPolicy.AcceptanceResult retry =
                MealCalorieFloorPolicy.acceptOptions(target, supplemented);
        if (!retry.accepted().isEmpty()) {
            log.warn("MAIN MEAL reached the MVP minimum floor only after compatible supplementation: "
                            + "mealType={}, targetKcal={}, bestKcal={}",
                    mealType, Math.round(target), Math.round(bestCalories(retry.accepted())));
            return renumber(retry.accepted());
        }

        throw new BusinessException(
                "Não foi possível montar " + mealType
                        + ": nenhuma opção atingiu o mínimo calórico após suplementação ("
                        + Math.round(bestCalories(supplemented)) + " kcal, mínimo "
                        + Math.round(floor) + " kcal).",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private boolean isBreakfast(String mealType) {
        return mealType != null && mealType.toUpperCase().startsWith("BREAKFAST");
    }

    private boolean isSnack(String mealType) {
        if (mealType == null) return false;
        String upper = mealType.toUpperCase();
        return upper.equals("MORNING_SNACK") || upper.equals("AFTERNOON_SNACK");
    }

    /**
     * Matches LUNCH and DINNER and all their diet variants (LUNCH_LOW_CARB, DINNER_CETOGENICA,
     * LUNCH_CARNIVORA, etc.) via prefix, so the main-meal fallback applies regardless of the diet
     * type or whether the plan has 3, 4 or 5 meals — meal count only changes the per-slot target,
     * never which slots exist.
     */
    private boolean isMainMeal(String mealType) {
        if (mealType == null) return false;
        String upper = mealType.toUpperCase();
        return upper.startsWith("LUNCH") || upper.startsWith("DINNER");
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
                        p.displayName(),
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
        String dietType = assessment.getDietType() != null ? assessment.getDietType().name() : null;
        return new AiAssessmentContext(
                assessment.getGoal().name(),
                dietType,
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

    private String resolveCompositionKey(String mealTypeKey, Assessment.DietType dietType) {
        if (mealTypeKey == null) return null;
        if (dietType == Assessment.DietType.LOW_CARB) {
            return switch (mealTypeKey.toUpperCase()) {
                case "LUNCH" -> "LUNCH_LOW_CARB";
                case "DINNER" -> "DINNER_LOW_CARB";
                default -> mealTypeKey;
            };
        }
        if (dietType == Assessment.DietType.CETOGENICA) {
            return switch (mealTypeKey.toUpperCase()) {
                case "LUNCH" -> "LUNCH_CETOGENICA";
                case "DINNER" -> "DINNER_CETOGENICA";
                case "BREAKFAST" -> "BREAKFAST_CETOGENICA";
                default -> mealTypeKey;
            };
        }
        if (dietType == Assessment.DietType.CARNIVORA) {
            return switch (mealTypeKey.toUpperCase()) {
                case "LUNCH" -> "LUNCH_CARNIVORA";
                case "DINNER" -> "DINNER_CARNIVORA";
                case "BREAKFAST" -> "BREAKFAST_CARNIVORA";
                default -> mealTypeKey;
            };
        }
        return mealTypeKey;
    }

    /**
     * Selection ordering preference for the given diet, or {@code null} when none applies.
     *
     * <p>FLEXITARIANA prefers plant-based foods first without excluding animal foods: the comparator
     * ranks vegetarian foods ahead of the rest, so a plant MAIN_PROTEIN is picked over meat when both
     * are eligible, while meat stays in the pool as fallback. All other diets keep the default order.
     */
    private Comparator<Food> resolveSelectionPriority(Assessment.DietType dietType) {
        if (dietType == Assessment.DietType.FLEXITARIANA) {
            return Comparator.comparingInt((Food food) -> food.isVegetarian() ? 0 : 1);
        }
        return null;
    }

    private int resolveMealsPerDay(Integer mealsPerDay) {
        if (mealsPerDay == null) return 4;
        if (mealsPerDay < 3 || mealsPerDay > 5) {
            throw new BusinessException(
                    "Número de refeições deve estar entre 3 e 5.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return mealsPerDay;
    }

    private record UserProfileData(User user, NutritionalProfile profile,
                                    Assessment assessment, int age) {}
}
