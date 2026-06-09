package com.tukan.api.service.mealplan;

import java.util.List;

/**
 * Single source of truth for the progressive calorie acceptance floor.
 *
 * <p>Acceptance works as a descending tier ladder applied per meal slot:
 * <ol>
 *   <li>Tier 1 — Standard (70 %): options that reach 70 % of the meal target are accepted as-is.</li>
 *   <li>Tier 2 — Relaxed (55 %): activated only when no option reaches 70 %. Signals a restricted pool.</li>
 *   <li>Tier 3 — MVP minimum (40 %): activated only when no option reaches 55 %. Last resort.</li>
 *   <li>Below 40 %: hard failure — no plan is persisted.</li>
 * </ol>
 *
 * <p>The fallback relaxes ONLY the caloric gate. Composition structure (dietType, mealType,
 * required FoodRole slots, suitable_meals, and user restrictions) remains guaranteed because
 * options always come from the pool already filtered and composed by FoodSelector /
 * MealCompositionRule — this class never touches that chain.
 *
 * <p>This class is stateless and has no infrastructure dependencies; safe to use as a Spring
 * component or in plain unit tests without a Spring context.
 */
public final class MealCalorieFloorPolicy {

    /** Tier 1 — standard acceptance threshold (70 % of meal target). */
    static final double TIER_STANDARD = 0.70;

    /** Tier 2 — relaxed acceptance threshold (55 % of meal target). Signals a restricted food pool. */
    static final double TIER_RELAXED = 0.55;

    /** Tier 3 — MVP minimum acceptance threshold (40 % of meal target). Last resort before hard failure. */
    static final double TIER_MINIMUM = 0.40;

    private MealCalorieFloorPolicy() {
        // utility class — not instantiable
    }

    /**
     * Result returned by {@link #acceptOptions(double, List)}.
     *
     * @param accepted   options that passed the highest viable tier (renumbering is the caller's
     *                   responsibility — this record returns the raw accepted subset)
     * @param tierRatio  the ratio used for acceptance (0.70, 0.55, or 0.40)
     */
    record AcceptanceResult(List<FoodSelector.MealOptionBuild> accepted, double tierRatio) {}

    /**
     * Returns the options accepted by the highest viable tier, or an empty result when all options
     * fall below the hard minimum (40 %).
     *
     * <p>Tier selection:
     * <ul>
     *   <li>If any option reaches {@link #TIER_STANDARD} (70 %), accept all options at that level.</li>
     *   <li>Else if any reaches {@link #TIER_RELAXED} (55 %), accept all options at that level.</li>
     *   <li>Else if any reaches {@link #TIER_MINIMUM} (40 %), accept all options at that level.</li>
     *   <li>Otherwise: return an empty result — the caller must fail with a BusinessException.</li>
     * </ul>
     *
     * @param target  calorie target for the meal slot (must be &gt; 0)
     * @param options pre-built options from FoodSelector (never null, may be empty)
     * @return acceptance result; {@code accepted} is empty when the hard floor is not met
     */
    public static AcceptanceResult acceptOptions(double target,
                                                 List<FoodSelector.MealOptionBuild> options) {
        if (options.isEmpty()) {
            return new AcceptanceResult(List.of(), TIER_MINIMUM);
        }

        for (double tier : new double[]{TIER_STANDARD, TIER_RELAXED, TIER_MINIMUM}) {
            double floor = target * tier;
            List<FoodSelector.MealOptionBuild> atTier = options.stream()
                    .filter(opt -> opt.totalCalories() >= floor)
                    .toList();
            if (!atTier.isEmpty()) {
                return new AcceptanceResult(atTier, tier);
            }
        }

        return new AcceptanceResult(List.of(), TIER_MINIMUM);
    }
}
