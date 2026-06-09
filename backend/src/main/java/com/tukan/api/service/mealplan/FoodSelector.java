package com.tukan.api.service.mealplan;

import com.tukan.api.domain.mealcomposition.MealCompositionRule;
import com.tukan.api.domain.mealcomposition.MealCompositionRules;
import com.tukan.api.domain.mealcomposition.MealCompositionSlot;
import com.tukan.api.domain.mealcomposition.MealStructureValidator;
import com.tukan.api.entity.Food;
import com.tukan.api.entity.FoodRole;
import com.tukan.api.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Seleciona alimentos para compor opções de refeição.
 *
 * <p>When a {@link MealCompositionRule} exists for the given meal type, selection is driven
 * by {@link FoodRole}: required slots are filled first, then supplemental items are added from
 * allowed roles up to {@link #MAX_ITEMS_PER_OPTION} and the calorie ceiling.
 *
 * <p>Fallback to category-based selection (legacy) is used when:
 * <ul>
 *   <li>the meal type key is unknown or null (no rule defined), OR</li>
 *   <li>the pool contains no foods classified for any slot of the applicable rule.</li>
 * </ul>
 */
@Component
public class FoodSelector {

    private static final Logger log = LoggerFactory.getLogger(FoodSelector.class);

    private static final double CALORIE_TOLERANCE = 0.10;
    static final int MAX_ITEMS_PER_OPTION = 5;
    private static final List<FoodRole> MAIN_MEAL_SUPPLEMENT_ROLES =
            List.of(FoodRole.HEALTHY_FAT, FoodRole.COMPLEMENT);

    /**
     * Breakfast-compatible roles eligible for the engine's best-effort breakfast supplementation
     * (Bug B1 — MVP rule). {@code HEALTHY_FAT} is intentionally included here even though it is not
     * part of the default breakfast composition slot: nuts/seeds/avocado are the densest compatible
     * way to lift a subcaloric breakfast toward the floor. Order is the supplementation preference.
     */
    private static final Set<FoodRole> BREAKFAST_BEST_EFFORT_ROLES = Set.of(
            FoodRole.HEALTHY_FAT, FoodRole.FRUIT, FoodRole.BREAKFAST_LIGHT,
            FoodRole.LIGHT_DAIRY, FoodRole.HEALTHY_SNACK);

    /**
     * Snack-compatible roles eligible for the engine's best-effort snack supplementation (Bug B4).
     * Mirrors {@link FoodRole#isSnackEligible()} ({@code HEALTHY_SNACK}, {@code FRUIT}) and adds
     * {@code HEALTHY_FAT} for caloric density — nuts/seeds are the densest snack-compatible item
     * that can lift a subcaloric option toward the floor without violating snack composition rules.
     * Same rationale as {@link #BREAKFAST_BEST_EFFORT_ROLES}.
     */
    private static final Set<FoodRole> SNACK_BEST_EFFORT_ROLES = Set.of(
            FoodRole.HEALTHY_FAT, FoodRole.HEALTHY_SNACK, FoodRole.FRUIT);


    /**
     * Hard ceiling for the floor-seeking override in supplemental fill. When a supplemental item
     * would exceed the soft ceiling (target × 1.10) but the internal floor (target × 0.90) has
     * not been reached, the item may still be added if the total stays within this bound
     * (target × 1.25). This prevents breakfast options from being rejected due to the ceiling
     * blocking a high-calorie item (e.g. nuts) that is needed to reach the calorie floor, while
     * keeping a firm upper cap against grossly overcaloric options.
     */
    private static final double CALORIE_FLOOR_OVERRIDE_MAX = 0.25;

    // Structural categories used by the legacy category-based fallback
    private static final List<String> CORE_CATEGORIES = List.of(
            "PROTEIN", "CARBOHYDRATE", "VEGETABLE", "FRUIT",
            "LEGUME", "DAIRY", "HEALTHY_FAT");

    private final Random random = new Random();

    /**
     * Monta 2 opções de refeição a partir dos alimentos disponíveis,
     * cada uma respeitando a meta calórica com margem de ±10%.
     *
     * @param availableFoods non-empty pool of pre-filtered foods for this meal
     * @param mealTypeKey    canonical engine key (e.g. "LUNCH", "MORNING_SNACK"); may be null
     * @param calorieTarget  target calories for this meal slot
     */
    public List<MealOptionBuild> buildTwoOptions(List<Food> availableFoods,
                                                  String mealTypeKey,
                                                  double calorieTarget) {
        return buildTwoOptions(availableFoods, mealTypeKey, calorieTarget, null);
    }

    /**
     * Same as {@link #buildTwoOptions(List, String, double)} but with a caller-provided selection
     * ordering applied before role/category grouping.
     *
     * <p>{@code selectionPriority} is a diet-agnostic tie-break: the selector knows nothing about
     * diet types, it only honours the ordering it is given. It is applied as the primary sort key
     * (the internal shuffle is the secondary signal), so candidates the caller ranks first are
     * picked first for each slot — without ever removing the remaining candidates, which stay
     * available as fallback. {@code null} preserves the legacy shuffle-only behaviour.
     *
     * @param selectionPriority ordering preference among candidates, or {@code null} for none
     */
    public List<MealOptionBuild> buildTwoOptions(List<Food> availableFoods,
                                                  String mealTypeKey,
                                                  double calorieTarget,
                                                  Comparator<Food> selectionPriority) {
        if (availableFoods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Lista de alimentos não pode estar vazia para montar opções de refeição.");
        }

        Optional<MealCompositionRule> ruleOpt = MealCompositionRules.forCanonicalKey(mealTypeKey);

        List<Food> shuffled = shuffleAndPrioritize(availableFoods, selectionPriority);

        if (ruleOpt.isPresent()) {
            log.debug("Meal selection: mealType={}, pool={}, path=role-based", mealTypeKey, availableFoods.size());
            return buildTwoOptionsByRole(shuffled, ruleOpt.get(), calorieTarget, availableFoods, selectionPriority);
        }
        log.debug("Meal selection: mealType={}, pool={}, path=category-fallback (no rule)",
                mealTypeKey, availableFoods.size());
        return buildTwoOptionsByCategory(shuffled, calorieTarget, availableFoods, selectionPriority);
    }

    // ---------------------------------------------------------------------------
    // Role-based selection (primary path — rule present)
    // ---------------------------------------------------------------------------

    private List<MealOptionBuild> buildTwoOptionsByRole(List<Food> shuffled,
                                                         MealCompositionRule rule,
                                                         double calorieTarget,
                                                         List<Food> originalPool,
                                                         Comparator<Food> selectionPriority) {
        EnumMap<FoodRole, List<Food>> byRole = groupByRole(shuffled);
        if (log.isDebugEnabled()) {
            log.debug("Role distribution for meal: {}", roleCounts(byRole));
        }

        boolean anyClassified = byRole.keySet().stream()
                .anyMatch(role -> role != FoodRole.UNSPECIFIED);
        if (!anyClassified) {
            // No food classified for this meal — fall back to category selection
            log.debug("No food classified by role for this meal — falling back to category selection");
            return buildTwoOptionsByCategory(shuffled, calorieTarget, originalPool, selectionPriority);
        }

        // Validate that each required slot has at least one candidate before attempting selection.
        // Validation runs once on the full pool so option-2 de-duplication failures are handled
        // by relaxedOption2 (empty excluded set) rather than triggering a false alarm here.
        validateRequiredSlots(rule, byRole);

        Set<Integer> usedInOption1 = new HashSet<>();
        List<Food> option1Items = selectByRole(rule, byRole, calorieTarget, usedInOption1);

        if (option1Items.isEmpty()) {
            // Role-based produced nothing — fall back so the option is never empty
            log.debug("Role-based selection produced no items — falling back to category selection");
            return buildTwoOptionsByCategory(shuffled, calorieTarget, originalPool, selectionPriority);
        }

        logStructuralOutcome(rule, option1Items, 1);

        Set<Integer> excludedForOption2 = new HashSet<>(usedInOption1);
        List<Food> option2Items = selectByRole(rule, byRole, calorieTarget, excludedForOption2);

        boolean option1Complete = isStructurallyComplete(rule, option1Items);
        if (option2Items.isEmpty()) {
            option2Items = relaxedOption2(rule, byRole, calorieTarget, originalPool, selectionPriority);
        } else if (option1Complete && !isStructurallyComplete(rule, option2Items)) {
            // Option 1 proved the pool can satisfy every required slot; de-duplication left option 2
            // structurally incomplete. Restore completeness by allowing item reuse, but only adopt the
            // relaxed result when it is actually complete — otherwise keep the distinct option.
            List<Food> relaxed = relaxedOption2(rule, byRole, calorieTarget, originalPool, selectionPriority);
            if (isStructurallyComplete(rule, relaxed)) {
                option2Items = relaxed;
            }
        }

        logStructuralOutcome(rule, option2Items, 2);
        return List.of(buildOption(1, option1Items), buildOption(2, option2Items));
    }

    /**
     * Throws BusinessException if any required slot has zero eligible candidates in the pool.
     * Called only on the role-based path, after confirming the pool has classified foods.
     */
    private void validateRequiredSlots(MealCompositionRule rule, EnumMap<FoodRole, List<Food>> byRole) {
        for (MealCompositionSlot slot : rule.requiredSlots()) {
            boolean hasCandidates = byRole.entrySet().stream()
                    .anyMatch(e -> slot.accepts(e.getKey()) && !e.getValue().isEmpty());
            if (!hasCandidates) {
                throw new BusinessException(
                        "Não foi possível montar a refeição " + rule.mealType().name()
                        + ": slot obrigatório '" + slot.name() + "' sem alimentos elegíveis.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    /**
     * Fills required slots first, then supplements with allowed-role foods up to ceiling/max.
     *
     * <p>For each required slot, a candidate is chosen whose effective role is accepted by that
     * slot, is not excluded, and ideally fits within the calorie ceiling (1.10 × target).
     * If no candidate fits the ceiling but candidates exist, the one with fewest calories is
     * chosen: composition integrity outweighs the calorie ceiling for mandatory slots.
     */
    private List<Food> selectByRole(MealCompositionRule rule,
                                     EnumMap<FoodRole, List<Food>> byRole,
                                     double calorieTarget,
                                     Set<Integer> excluded) {
        List<Food> selected = new ArrayList<>();
        double accumulated = 0.0;
        double ceiling = calorieTarget * (1 + CALORIE_TOLERANCE);

        // Phase 1 — fill required slots
        for (MealCompositionSlot slot : rule.requiredSlots()) {
            Food picked = pickForSlot(slot, byRole, ceiling - accumulated, excluded);
            if (picked != null) {
                selected.add(picked);
                excluded.add(picked.getId());
                accumulated += caloriesForPortion(picked);
            } else {
                log.warn("Required slot '{}' could not be filled for {} — no eligible food in pool",
                        slot.name(), rule.mealType());
            }
        }

        // Phase 2 — supplemental fill with allowed roles. The critical calorie floor that decides
        // whether the resulting option is acceptable is enforced by MealPlanEngine, not here: this
        // selector is best-effort and must stay able to return a structurally valid (possibly
        // subcaloric) option for callers that test selection mechanics in isolation.
        double floor = calorieTarget * (1 - CALORIE_TOLERANCE);
        if (accumulated < floor && selected.size() < MAX_ITEMS_PER_OPTION) {
            fillSupplemental(rule, byRole, selected, excluded, accumulated, ceiling, floor, calorieTarget);
        }

        return selected;
    }

    /**
     * Picks the best candidate for a slot: prefers one that fits within the remaining calorie
     * budget; if none fits but candidates exist, returns the one with the fewest calories.
     */
    private Food pickForSlot(MealCompositionSlot slot,
                              EnumMap<FoodRole, List<Food>> byRole,
                              double remainingBudget,
                              Set<Integer> excluded) {
        Food bestFitting = null;
        Food cheapestFallback = null;
        double cheapestFallbackCal = Double.MAX_VALUE;

        for (var entry : byRole.entrySet()) {
            if (!slot.accepts(entry.getKey())) continue;
            for (Food food : entry.getValue()) {
                if (excluded.contains(food.getId())) continue;
                double cal = caloriesForPortion(food);
                if (cal <= remainingBudget) {
                    if (bestFitting == null) bestFitting = food;
                } else if (cal < cheapestFallbackCal) {
                    cheapestFallback = food;
                    cheapestFallbackCal = cal;
                }
            }
        }

        return bestFitting != null ? bestFitting : cheapestFallback;
    }

    /**
     * Adds supplemental foods from allowed roles not yet represented in {@code selected},
     * until the calorie floor is reached or MAX_ITEMS_PER_OPTION is hit.
     *
     * <p>At most one supplemental food per role is added. Roles already covered by the
     * required-slot selection are skipped entirely, which prevents a second MAIN_PROTEIN
     * (or any other already-filled role) from appearing alongside the mandatory one.
     *
     * <p>For LUNCH/DINNER all three allowed roles (MAIN_PROTEIN, MAIN_CARBOHYDRATE,
     * VEGETABLE_LEGUME) are filled by the required-slot phase. After that first loop, a
     * secondary {@code isMainMeal} block attempts HEALTHY_FAT and COMPLEMENT — roles
     * absent from {@code allowedRoles()} but appropriate caloric supplements — if the
     * calorie floor has still not been reached.
     *
     * <p>Floor-seeking override: when the internal floor has not been reached and a
     * supplemental item would exceed the soft ceiling (target × 1.10), the item is still
     * added if the total stays within the hard max (target × {@value #CALORIE_FLOOR_OVERRIDE_MAX}
     * + 1). This allows breakfast options to absorb high-calorie items (e.g. nuts) that are
     * blocked by the soft ceiling but are essential to meet the floor. The engine's critical
     * floor (target × 0.60) remains the final acceptance gate — this selector is best-effort.
     */
    private double fillSupplemental(MealCompositionRule rule,
                                     EnumMap<FoodRole, List<Food>> byRole,
                                     List<Food> selected,
                                     Set<Integer> excluded,
                                     double accumulated,
                                     double ceiling,
                                     double floor,
                                     double calorieTarget) {
        double hardMax = calorieTarget * (1 + CALORIE_FLOOR_OVERRIDE_MAX);
        Set<FoodRole> usedRoles = new HashSet<>();
        for (Food f : selected) {
            usedRoles.add(f.getEffectiveRole());
        }
        for (var entry : byRole.entrySet()) {
            FoodRole role = entry.getKey();
            if (!rule.isRoleAllowed(role)) continue;
            if (usedRoles.contains(role)) continue;
            for (Food food : entry.getValue()) {
                if (selected.size() >= MAX_ITEMS_PER_OPTION) return accumulated;
                if (accumulated >= floor) return accumulated;
                if (excluded.contains(food.getId())) continue;
                double cal = caloriesForPortion(food);
                boolean withinSoftCeiling = accumulated + cal <= ceiling;
                boolean floorOverride = accumulated < floor && accumulated + cal <= hardMax;
                if (withinSoftCeiling || floorOverride) {
                    selected.add(food);
                    excluded.add(food.getId());
                    accumulated += cal;
                    usedRoles.add(role);
                    break;
                }
            }
        }

        // Additional supplementation for main meals (LUNCH/DINNER): try HEALTHY_FAT and COMPLEMENT.
        // These roles are not in allowedRoles() for main meals but are appropriate caloric supplements.
        // Guard: only applies when the pool has MAIN_PROTEIN as a required role (= main meal proxy).
        boolean isMainMeal = rule.requiredSlots().stream()
                .anyMatch(s -> s.accepts(FoodRole.MAIN_PROTEIN));
        if (isMainMeal) {
            for (FoodRole suppRole : MAIN_MEAL_SUPPLEMENT_ROLES) {
                if (selected.size() >= MAX_ITEMS_PER_OPTION) break;
                if (accumulated >= floor) break;
                if (usedRoles.contains(suppRole)) continue;
                List<Food> candidates = byRole.getOrDefault(suppRole, List.of());
                for (Food food : candidates) {
                    if (excluded.contains(food.getId())) continue;
                    double cal = caloriesForPortion(food);
                    boolean withinSoftCeiling = accumulated + cal <= ceiling;
                    boolean floorOverride = accumulated < floor && accumulated + cal <= hardMax;
                    if (withinSoftCeiling || floorOverride) {
                        selected.add(food);
                        excluded.add(food.getId());
                        accumulated += cal;
                        usedRoles.add(suppRole);
                        break;
                    }
                }
            }
        }

        return accumulated;
    }

    /**
     * Generates option 2 when de-duplication left nothing: reshuffles and relaxes exclusion.
     * Falls back to category selection if role-based still produces nothing.
     */
    private List<Food> relaxedOption2(MealCompositionRule rule,
                                       EnumMap<FoodRole, List<Food>> byRole,
                                       double calorieTarget,
                                       List<Food> originalPool,
                                       Comparator<Food> selectionPriority) {
        List<Food> reshuffled = shuffleAndPrioritize(originalPool, selectionPriority);
        EnumMap<FoodRole, List<Food>> reshuffledByRole = groupByRole(reshuffled);
        List<Food> relaxed = selectByRole(rule, reshuffledByRole, calorieTarget, new HashSet<>());
        if (!relaxed.isEmpty()) return relaxed;
        // Last resort: category fallback so option 2 is never empty
        return selectByCategoryFallback(buildCategoryMap(reshuffled), calorieTarget, new HashSet<>());
    }

    // ---------------------------------------------------------------------------
    // Category-based selection (fallback path — no rule or fully unclassified pool)
    // ---------------------------------------------------------------------------

    /**
     * Activated when: (a) mealTypeKey is unknown/null and no rule exists, or
     * (b) the pool has no food classified for any slot of the applicable rule.
     * Uses the original category-based algorithm unchanged from pre-2.4E.
     */
    private List<MealOptionBuild> buildTwoOptionsByCategory(List<Food> shuffled,
                                                             double calorieTarget,
                                                             List<Food> originalPool,
                                                             Comparator<Food> selectionPriority) {
        Map<String, List<Food>> byCategory = buildCategoryMap(shuffled);

        Set<Integer> usedInOption1 = new HashSet<>();
        List<Food> option1Items = selectByCategoryFallback(byCategory, calorieTarget, usedInOption1);

        Set<Integer> excludedForOption2 = new HashSet<>(usedInOption1);
        List<Food> option2Items = selectByCategoryFallback(byCategory, calorieTarget, excludedForOption2);

        if (option2Items.isEmpty() && !option1Items.isEmpty()) {
            List<Food> reshuffled = shuffleAndPrioritize(originalPool, selectionPriority);
            Map<String, List<Food>> reshuffledByCategory = buildCategoryMap(reshuffled);
            option2Items = selectByCategoryFallback(reshuffledByCategory, calorieTarget, new HashSet<>());
        }

        return List.of(buildOption(1, option1Items), buildOption(2, option2Items));
    }

    private List<Food> selectByCategoryFallback(Map<String, List<Food>> byCategory,
                                                  double calorieTarget,
                                                  Set<Integer> excluded) {
        List<Food> selected = new ArrayList<>();
        double accumulated = 0.0;
        double maxCalories = calorieTarget * (1 + CALORIE_TOLERANCE);

        // First pass: one food per core category
        for (String category : CORE_CATEGORIES) {
            if (selected.size() >= MAX_ITEMS_PER_OPTION) break;
            if (accumulated >= maxCalories) break;

            for (Food food : byCategory.getOrDefault(category, List.of())) {
                if (excluded.contains(food.getId())) continue;
                double foodCalories = caloriesForPortion(food);
                if (accumulated + foodCalories <= maxCalories) {
                    selected.add(food);
                    excluded.add(food.getId());
                    accumulated += foodCalories;
                    break;
                }
            }
        }

        // Second pass: fill to minimum calorie floor
        double minCalories = calorieTarget * (1 - CALORIE_TOLERANCE);
        if (accumulated < minCalories) {
            for (var entry : byCategory.entrySet()) {
                if (selected.size() >= MAX_ITEMS_PER_OPTION) break;
                if (accumulated >= minCalories) break;
                for (Food food : entry.getValue()) {
                    if (excluded.contains(food.getId())) continue;
                    if (selected.size() >= MAX_ITEMS_PER_OPTION) break;
                    double foodCalories = caloriesForPortion(food);
                    if (accumulated + foodCalories <= maxCalories) {
                        selected.add(food);
                        excluded.add(food.getId());
                        accumulated += foodCalories;
                        if (accumulated >= minCalories) break;
                    }
                }
            }
        }

        // Last resort: never return an empty option while the pool still has a usable food.
        if (selected.isEmpty()) {
            Food cheapest = cheapestAvailable(byCategory, excluded);
            if (cheapest != null) {
                selected.add(cheapest);
                excluded.add(cheapest.getId());
            }
        }

        return selected;
    }

    /**
     * Cheapest non-excluded food across all categories, or {@code null} when none is available.
     *
     * <p>This is the last-resort guard for the category fallback path: it deliberately ignores the
     * calorie ceiling so an option is never silently empty when the pool still has a usable food.
     * Producing a single over-the-ceiling item is preferable to an empty meal, and is consistent
     * with {@link #pickForSlot} preferring composition integrity over the ceiling for mandatory slots.
     */
    private Food cheapestAvailable(Map<String, List<Food>> byCategory, Set<Integer> excluded) {
        Food cheapest = null;
        double cheapestCal = Double.MAX_VALUE;
        for (List<Food> foods : byCategory.values()) {
            for (Food food : foods) {
                if (excluded.contains(food.getId())) continue;
                double cal = caloriesForPortion(food);
                if (cal < cheapestCal) {
                    cheapest = food;
                    cheapestCal = cal;
                }
            }
        }
        return cheapest;
    }

    // ---------------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------------

    /** Count of foods per role, for observability only — no food contents are exposed. */
    private Map<FoodRole, Integer> roleCounts(EnumMap<FoodRole, List<Food>> byRole) {
        EnumMap<FoodRole, Integer> counts = new EnumMap<>(FoodRole.class);
        byRole.forEach((role, foods) -> counts.put(role, foods.size()));
        return counts;
    }

    /** Logs whether the given option satisfies the rule and, if not, which required slots are missing. */
    private void logStructuralOutcome(MealCompositionRule rule, List<Food> items, int optionNumber) {
        var result = MealStructureValidator.validate(rule, rolesOf(items));
        if (result.complete()) {
            log.debug("Structural validation: complete (option={}, items={})", optionNumber, items.size());
        } else {
            log.warn("Structural validation: incomplete for {} option {} — missingSlots={}",
                    rule.mealType(), optionNumber, result.unsatisfiedSlots());
        }
    }

    /** Effective roles of the selected foods, in selection order. */
    private List<FoodRole> rolesOf(List<Food> foods) {
        List<FoodRole> roles = new ArrayList<>(foods.size());
        for (Food food : foods) {
            roles.add(food.getEffectiveRole());
        }
        return roles;
    }

    /** Whether the selected foods satisfy every required slot of the rule. */
    private boolean isStructurallyComplete(MealCompositionRule rule, List<Food> items) {
        return MealStructureValidator.validate(rule, rolesOf(items)).complete();
    }

    /**
     * Shuffles for variety, then stably sorts so foods with a declared primary meal type come first.
     *
     * <p>When {@code selectionPriority} is provided it becomes the primary sort key, applied before
     * the primary-meal-type ordering. Because the sort is stable, candidates the caller does not rank
     * keep their shuffled order — the priority only reorders, it never drops anything from the pool.
     */
    private List<Food> shuffleAndPrioritize(List<Food> foods, Comparator<Food> selectionPriority) {
        List<Food> result = new ArrayList<>(foods);
        Collections.shuffle(result, random);
        Comparator<Food> byPrimaryMealType = Comparator.comparing(
                (Food f) -> f.getPrimaryMealType() != null ? 0 : 1);
        Comparator<Food> effective = selectionPriority != null
                ? selectionPriority.thenComparing(byPrimaryMealType)
                : byPrimaryMealType;
        result.sort(effective);
        return result;
    }

    private EnumMap<FoodRole, List<Food>> groupByRole(List<Food> foods) {
        EnumMap<FoodRole, List<Food>> byRole = new EnumMap<>(FoodRole.class);
        for (Food food : foods) {
            byRole.computeIfAbsent(food.getEffectiveRole(), k -> new ArrayList<>()).add(food);
        }
        return byRole;
    }

    private Map<String, List<Food>> buildCategoryMap(List<Food> foods) {
        return foods.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCategory() != null ? f.getCategory() : "OTHER",
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * Calories for the food's reference portion.
     *
     * <p>Uses {@code referencePortionGrams} when set, otherwise 100 g — the same default used
     * by {@link #buildOption} so that selection and option-building are always consistent.
     * Without this alignment, foods with a NULL portion contribute 0 kcal during selection
     * (always winning the ceiling check) but appear with a non-zero calorie total in the
     * final MealOptionBuild, producing misleading calorie accounting.
     */
    private double caloriesForPortion(Food food) {
        BigDecimal cal100g = food.getCaloriesPer100g();
        if (cal100g == null) return 0.0;
        BigDecimal portion = food.getReferencePortionGrams() != null
                ? food.getReferencePortionGrams() : BigDecimal.valueOf(100);
        return cal100g.multiply(portion).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Best-effort breakfast supplementation used only by the engine's breakfast fallback when the
     * standard calorie tiers fail (Bug B1 — MVP rule). Adds calorie-dense, breakfast-compatible
     * foods ({@link #BREAKFAST_BEST_EFFORT_ROLES}) from this meal's pool to an existing option,
     * sorted by calories descending, until the supplied floor is reached or
     * {@link #MAX_ITEMS_PER_OPTION} is hit.
     *
     * <p>Relaxes ONLY the caloric dimension: every candidate already comes from the pre-filtered
     * breakfast pool, so dietType, user restrictions and suitable_meals stay enforced. The original
     * option is returned unchanged when it already clears the floor or nothing compatible can be
     * added.
     *
     * @param option        option to supplement; its items must originate from {@code mealPool}
     * @param mealPool       breakfast pool already filtered/composed for this user
     * @param floorCalories  minimum calorie target to try to reach (e.g. target × 0.40)
     */
    public MealOptionBuild supplementBreakfast(MealOptionBuild option,
                                                List<Food> mealPool,
                                                double floorCalories) {
        return supplement(option, mealPool, floorCalories, BREAKFAST_BEST_EFFORT_ROLES);
    }

    /**
     * Best-effort snack supplementation used by the engine's snack fallback when the standard
     * calorie tiers fail (Bug B4). Adds calorie-dense, snack-compatible foods
     * ({@link #SNACK_BEST_EFFORT_ROLES}) from this meal's pool to an existing option, sorted by
     * calories descending, until the supplied floor is reached or {@link #MAX_ITEMS_PER_OPTION} is hit.
     *
     * <p>Relaxes ONLY the caloric dimension. Every candidate already comes from the pre-filtered
     * snack pool, so dietType, user restrictions and suitable_meals stay enforced.
     *
     * @param option        option to supplement; its items must originate from {@code mealPool}
     * @param mealPool       snack pool already filtered/composed for this user
     * @param floorCalories  minimum calorie target to try to reach (e.g. target × 0.40)
     */
    public MealOptionBuild supplementSnack(MealOptionBuild option,
                                            List<Food> mealPool,
                                            double floorCalories) {
        return supplement(option, mealPool, floorCalories, SNACK_BEST_EFFORT_ROLES);
    }

    /**
     * Best-effort lunch/dinner supplementation used by the engine's main-meal fallback when the
     * standard calorie tiers fail (Bug B4 extension). Eligible roles are derived from
     * {@code rule.allowedRoles()} — the effective composition of the variant being supplemented —
     * so no role outside the meal's composition contract (e.g. MAIN_CARBOHYDRATE for LOW_CARB,
     * VEGETABLE_LEGUME for CARNIVORA) can ever be added. The pool is already pre-filtered by
     * dietType, so this method only relaxes the caloric dimension without violating composition.
     *
     * @param option        option to supplement; its items must originate from {@code mealPool}
     * @param mealPool       lunch/dinner pool already filtered/composed for this user
     * @param floorCalories  minimum calorie target to try to reach (e.g. target × 0.40)
     * @param rule           effective composition rule for this meal variant (e.g. LUNCH_LOW_CARB);
     *                       {@link MealCompositionRule#allowedRoles()} determines eligible roles
     */
    public MealOptionBuild supplementMainMeal(MealOptionBuild option,
                                               List<Food> mealPool,
                                               double floorCalories,
                                               MealCompositionRule rule) {
        return supplement(option, mealPool, floorCalories, rule.allowedRoles());
    }

    /**
     * Generic best-effort supplementation used by both breakfast and snack fallbacks.
     * Adds calorie-dense foods from {@code eligibleRoles} in the meal pool to an existing option,
     * sorted by calories descending, until the floor is reached or {@link #MAX_ITEMS_PER_OPTION} is hit.
     *
     * <p>Relaxes ONLY the caloric dimension: every candidate already comes from the pre-filtered
     * pool for this meal, so dietType, user restrictions and suitable_meals stay enforced. The
     * original option is returned unchanged when it already clears the floor or nothing compatible
     * can be added.
     */
    private MealOptionBuild supplement(MealOptionBuild option,
                                        List<Food> mealPool,
                                        double floorCalories,
                                        Set<FoodRole> eligibleRoles) {
        if (option.totalCalories() >= floorCalories) {
            return option;
        }

        Map<Integer, Food> poolById = mealPool.stream()
                .collect(Collectors.toMap(Food::getId, food -> food, (a, b) -> a, LinkedHashMap::new));

        List<Food> selected = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (FoodPortionBuild item : option.items()) {
            Food food = poolById.get(item.foodId());
            if (food != null && used.add(food.getId())) {
                selected.add(food);
            }
        }

        double accumulated = option.totalCalories();
        List<Food> candidates = mealPool.stream()
                .filter(food -> !used.contains(food.getId()))
                .filter(food -> eligibleRoles.contains(food.getEffectiveRole()))
                .sorted(Comparator.comparingDouble(this::caloriesForPortion).reversed())
                .toList();

        for (Food candidate : candidates) {
            if (selected.size() >= MAX_ITEMS_PER_OPTION || accumulated >= floorCalories) break;
            selected.add(candidate);
            used.add(candidate.getId());
            accumulated += caloriesForPortion(candidate);
        }

        return buildOption(option.optionNumber(), selected);
    }

    private MealOptionBuild buildOption(int number, List<Food> items) {
        double totalCal = 0, totalProt = 0, totalCarbs = 0, totalFat = 0;

        List<FoodPortionBuild> portions = new ArrayList<>();
        for (Food food : items) {
            BigDecimal portion = food.getReferencePortionGrams() != null
                    ? food.getReferencePortionGrams() : BigDecimal.valueOf(100);
            double factor = portion.doubleValue() / 100.0;

            double cal = toDouble(food.getCaloriesPer100g()) * factor;
            double prot = toDouble(food.getProteinPer100g()) * factor;
            double carbs = toDouble(food.getCarbsPer100g()) * factor;
            double fat = toDouble(food.getFatPer100g()) * factor;
            double fiber = toDouble(food.getFiberPer100g()) * factor;

            totalCal += cal;
            totalProt += prot;
            totalCarbs += carbs;
            totalFat += fat;

            portions.add(new FoodPortionBuild(
                    food.getId(), food.getName(), food.getDisplayName(), food.getCategory(),
                    portion, round(cal), round(prot), round(carbs), round(fat), round(fiber)));
        }

        return new MealOptionBuild(number, portions,
                round(totalCal), round(totalProt), round(totalCarbs), round(totalFat));
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // ---------------------------------------------------------------------------
    // Output records (unchanged contract — no API impact)
    // ---------------------------------------------------------------------------

    public record MealOptionBuild(
            int optionNumber,
            List<FoodPortionBuild> items,
            double totalCalories,
            double totalProtein,
            double totalCarbs,
            double totalFat
    ) {}

    public record FoodPortionBuild(
            Integer foodId,
            String name,
            String displayName,
            String category,
            BigDecimal portionGrams,
            double calories,
            double protein,
            double carbs,
            double fat,
            double fiber
    ) {}
}
