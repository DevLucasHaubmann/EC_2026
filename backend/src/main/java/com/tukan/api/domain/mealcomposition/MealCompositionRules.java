package com.tukan.api.domain.mealcomposition;

import com.tukan.api.entity.FoodRole;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Static registry of {@link MealCompositionRule} instances, one per {@link MealType}.
 *
 * <p>This is the composition contract for Sprint 2.4D. Each rule defines which
 * {@link MealCompositionSlot}s must be satisfied for the meal to be considered complete.
 *
 * <p>Alignment guarantees:
 * <ul>
 *   <li>Canonical string keys remain in the engine ({@code MealDistributor} /
 *       {@code MealSuitabilityService}) — this registry does not replace them.</li>
 *   <li>Slot accepted-role sets are aligned to {@link FoodRole} helper predicates:
 *       {@code BREAKFAST_LIGHT_SLOT} mirrors {@link FoodRole#isBreakfastEligible()};
 *       {@code FRUIT_OR_SNACK_SLOT} mirrors {@link FoodRole#isSnackEligible()}.</li>
 *   <li>{@code FoodRole.UNSPECIFIED} and {@code null} are never eligible roles in any slot.</li>
 * </ul>
 */
public final class MealCompositionRules {

    // ---------------------------------------------------------------------------
    // Reusable slot constants
    // ---------------------------------------------------------------------------

    static final MealCompositionSlot MAIN_PROTEIN_SLOT =
            new MealCompositionSlot("main protein", Set.of(FoodRole.MAIN_PROTEIN));

    static final MealCompositionSlot MAIN_CARBOHYDRATE_SLOT =
            new MealCompositionSlot("main carbohydrate", Set.of(FoodRole.MAIN_CARBOHYDRATE));

    static final MealCompositionSlot VEGETABLE_LEGUME_SLOT =
            new MealCompositionSlot("vegetable/legume", Set.of(FoodRole.VEGETABLE_LEGUME));

    static final MealCompositionSlot HEALTHY_FAT_SLOT =
            new MealCompositionSlot("healthy fat", Set.of(FoodRole.HEALTHY_FAT));

    /**
     * Light breakfast item slot — accepted roles are derived from
     * {@link FoodRole#isBreakfastEligible()}, the single source of truth for breakfast
     * eligibility, so the slot can never drift from the helper.
     */
    static final MealCompositionSlot BREAKFAST_LIGHT_SLOT =
            new MealCompositionSlot("light breakfast item",
                    rolesMatching(FoodRole::isBreakfastEligible));

    /**
     * Fruit or healthy snack slot — accepted roles are derived from
     * {@link FoodRole#isSnackEligible()}, the single source of truth for snack eligibility.
     */
    static final MealCompositionSlot FRUIT_OR_SNACK_SLOT =
            new MealCompositionSlot("fruit or healthy snack",
                    rolesMatching(FoodRole::isSnackEligible));

    /**
     * Keto breakfast item slot (Task 2.8H.1). The default {@link #BREAKFAST_LIGHT_SLOT} only
     * accepts breakfast-eligible roles ({@code BREAKFAST_LIGHT}, {@code FRUIT}, {@code LIGHT_DAIRY},
     * {@code HEALTHY_SNACK}), all of which are carb-dense and removed by the keto food filter
     * (carbs ≤ 10 AND fat ≥ 15), leaving the slot without candidates and aborting generation.
     * A keto breakfast is built from eggs ({@code MAIN_PROTEIN}), avocado/seeds/oils
     * ({@code HEALTHY_FAT}) or full-fat dairy ({@code LIGHT_DAIRY}). The keto filter already
     * guarantees keto compliance of whatever survives, so this slot only enforces structure.
     * No {@code MAIN_CARBOHYDRATE}, {@code FRUIT} or {@code BREAKFAST_LIGHT} by design.
     */
    static final MealCompositionSlot BREAKFAST_CETOGENICA_SLOT =
            new MealCompositionSlot("keto breakfast item",
                    Set.of(FoodRole.MAIN_PROTEIN, FoodRole.HEALTHY_FAT, FoodRole.LIGHT_DAIRY));

    /**
     * Carnivore breakfast item slot (Task 2.8H.1). Same root cause as the keto slot: the default
     * breakfast slot excludes {@code MAIN_PROTEIN}, yet for CARNIVORA the only animal-origin
     * breakfast foods are eggs/meat ({@code MAIN_PROTEIN}) and dairy ({@code LIGHT_DAIRY}).
     * {@code HEALTHY_FAT} is intentionally absent: the only fat pool (GORDURAS_E_OLEOS) is mixed
     * animal/vegetable origin and the CARNIVORA filter excludes it entirely. No carbs, vegetables
     * or fruit by design.
     */
    static final MealCompositionSlot BREAKFAST_CARNIVORA_SLOT =
            new MealCompositionSlot("animal breakfast item",
                    Set.of(FoodRole.MAIN_PROTEIN, FoodRole.LIGHT_DAIRY));

    /** Collects every {@link FoodRole} satisfying the given eligibility predicate. */
    private static Set<FoodRole> rolesMatching(Predicate<FoodRole> eligibility) {
        return Arrays.stream(FoodRole.values())
                .filter(eligibility)
                .collect(Collectors.toUnmodifiableSet());
    }

    // ---------------------------------------------------------------------------
    // Static rule registry
    // ---------------------------------------------------------------------------

    // Map.ofEntries (not Map.of) because the registry now exceeds the 10-pair Map.of limit.
    private static final Map<MealType, MealCompositionRule> RULES = Map.ofEntries(
            Map.entry(MealType.BREAKFAST,
                    new MealCompositionRule(MealType.BREAKFAST, List.of(BREAKFAST_LIGHT_SLOT))),

            Map.entry(MealType.MORNING_SNACK,
                    new MealCompositionRule(MealType.MORNING_SNACK, List.of(FRUIT_OR_SNACK_SLOT))),

            Map.entry(MealType.AFTERNOON_SNACK,
                    new MealCompositionRule(MealType.AFTERNOON_SNACK, List.of(FRUIT_OR_SNACK_SLOT))),

            Map.entry(MealType.LUNCH,
                    new MealCompositionRule(MealType.LUNCH,
                            List.of(MAIN_PROTEIN_SLOT, MAIN_CARBOHYDRATE_SLOT, VEGETABLE_LEGUME_SLOT))),

            Map.entry(MealType.DINNER,
                    new MealCompositionRule(MealType.DINNER,
                            List.of(MAIN_PROTEIN_SLOT, MAIN_CARBOHYDRATE_SLOT, VEGETABLE_LEGUME_SLOT))),

            Map.entry(MealType.LUNCH_LOW_CARB,
                    new MealCompositionRule(MealType.LUNCH_LOW_CARB,
                            List.of(MAIN_PROTEIN_SLOT, VEGETABLE_LEGUME_SLOT, HEALTHY_FAT_SLOT))),

            Map.entry(MealType.DINNER_LOW_CARB,
                    new MealCompositionRule(MealType.DINNER_LOW_CARB,
                            List.of(MAIN_PROTEIN_SLOT, VEGETABLE_LEGUME_SLOT, HEALTHY_FAT_SLOT))),

            // CETOGENICA (Task 2.8E): stricter than LOW_CARB. No MAIN_CARBOHYDRATE and no required
            // VEGETABLE_LEGUME slot — the keto food filter (fatPer100g >= 15) removes most vegetables,
            // so requiring that slot would leave it without candidates and abort plan generation.
            // HEALTHY_FAT is structural (required) here, alongside MAIN_PROTEIN.
            Map.entry(MealType.LUNCH_CETOGENICA,
                    new MealCompositionRule(MealType.LUNCH_CETOGENICA,
                            List.of(MAIN_PROTEIN_SLOT, HEALTHY_FAT_SLOT))),

            Map.entry(MealType.DINNER_CETOGENICA,
                    new MealCompositionRule(MealType.DINNER_CETOGENICA,
                            List.of(MAIN_PROTEIN_SLOT, HEALTHY_FAT_SLOT))),

            // CETOGENICA breakfast (Task 2.8H.1): the default BREAKFAST_LIGHT slot is incompatible
            // with the keto filter — see BREAKFAST_CETOGENICA_SLOT. Single structural slot.
            Map.entry(MealType.BREAKFAST_CETOGENICA,
                    new MealCompositionRule(MealType.BREAKFAST_CETOGENICA,
                            List.of(BREAKFAST_CETOGENICA_SLOT))),

            // CARNIVORA (Task 2.8F): animal-origin only. MAIN_PROTEIN is the single structural slot.
            // HEALTHY_FAT is intentionally NOT required: the only HEALTHY_FAT pool (subcategory
            // GORDURAS_E_OLEOS) mixes animal fats with vegetable oils, so the CARNIVORA food filter
            // excludes it entirely — requiring that slot would leave it empty and abort generation.
            // No MAIN_CARBOHYDRATE and no VEGETABLE_LEGUME by design (no grains/vegetables allowed).
            Map.entry(MealType.LUNCH_CARNIVORA,
                    new MealCompositionRule(MealType.LUNCH_CARNIVORA,
                            List.of(MAIN_PROTEIN_SLOT))),

            Map.entry(MealType.DINNER_CARNIVORA,
                    new MealCompositionRule(MealType.DINNER_CARNIVORA,
                            List.of(MAIN_PROTEIN_SLOT))),

            // CARNIVORA breakfast (Task 2.8H.1): the default BREAKFAST_LIGHT slot excludes
            // MAIN_PROTEIN — see BREAKFAST_CARNIVORA_SLOT. Single structural slot.
            Map.entry(MealType.BREAKFAST_CARNIVORA,
                    new MealCompositionRule(MealType.BREAKFAST_CARNIVORA,
                            List.of(BREAKFAST_CARNIVORA_SLOT)))
    );

    private MealCompositionRules() {
        // utility class — not instantiable
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Returns the composition rule for the given {@link MealType}.
     *
     * @param mealType the meal type to look up; must not be {@code null}
     * @return the corresponding {@link MealCompositionRule}
     * @throws IllegalStateException if no rule is defined for the given meal type
     *                               (defensive guard against registry drift)
     */
    public static MealCompositionRule forMealType(MealType mealType) {
        Objects.requireNonNull(mealType, "mealType must not be null");
        MealCompositionRule rule = RULES.get(mealType);
        if (rule == null) {
            throw new IllegalStateException(
                    "No composition rule defined for meal type: " + mealType);
        }
        return rule;
    }

    /**
     * Converts a canonical engine key to a rule, if both the key and the rule are known.
     *
     * @param mealTypeKey canonical string key (e.g. "LUNCH", "morning_snack"); may be {@code null}
     * @return the rule wrapped in {@link Optional}, or {@link Optional#empty()} for unknown input
     */
    public static Optional<MealCompositionRule> forCanonicalKey(String mealTypeKey) {
        return MealType.fromCanonicalKey(mealTypeKey)
                .flatMap(type -> Optional.ofNullable(RULES.get(type)));
    }
}
