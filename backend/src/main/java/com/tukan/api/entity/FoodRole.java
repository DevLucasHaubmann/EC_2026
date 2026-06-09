package com.tukan.api.entity;

import java.util.Set;

/**
 * Functional role a food plays inside a meal composition (Task 2.4B).
 *
 * <p>This is intentionally a DIFFERENT axis from {@link Food#getCategory()}:
 * <ul>
 *   <li>{@code category} = broad nutritional classification (e.g. DAIRY, CARBOHYDRATE).</li>
 *   <li>{@code FoodRole} = how the food is used when assembling a meal.</li>
 * </ul>
 * Example: yogurt is {@code DAIRY} by category but {@code LIGHT_DAIRY} by role;
 * rice is {@code CARBOHYDRATE} by category but {@code MAIN_CARBOHYDRATE} by role.
 *
 * <p>The helper predicates below are the single source of truth for meal-composition
 * eligibility, consumed by the selection logic introduced in later 2.4 tasks. They
 * encode the Sprint 2.4 rules directly so callers never re-derive them from food names.
 */
public enum FoodRole {

    /** Protein base of lunch/dinner (chicken, beef, fish, eggs, tofu). */
    MAIN_PROTEIN,

    /** Energy base of lunch/dinner (rice, potato, pasta, cassava). */
    MAIN_CARBOHYDRATE,

    /** Greens, vegetables and legumes — the "verdura/legume" component of a main meal. */
    VEGETABLE_LEGUME,

    /** Whole fruit, eligible for breakfast and snacks. */
    FRUIT,

    /** Light breakfast staple (oats, bread, tapioca, granola). */
    BREAKFAST_LIGHT,

    /** Quick healthy snack (nuts, cereal bar, whole crackers). */
    HEALTHY_SNACK,

    /** Light dairy suitable for breakfast/snack (yogurt, light cheese, milk). */
    LIGHT_DAIRY,

    /** Source of healthy fat used as a complement (avocado, olive oil, seeds). */
    HEALTHY_FAT,

    /** Minor complement that does not anchor a meal (condiment, spread, beverage). */
    COMPLEMENT,

    /** Role not yet assigned — safe default for not-yet-reclassified foods. */
    UNSPECIFIED;

    private static final Set<FoodRole> MAIN_MEAL_COMPONENTS =
            Set.of(MAIN_PROTEIN, MAIN_CARBOHYDRATE, VEGETABLE_LEGUME);

    private static final Set<FoodRole> BREAKFAST_ELIGIBLE =
            Set.of(BREAKFAST_LIGHT, FRUIT, LIGHT_DAIRY, HEALTHY_SNACK);

    private static final Set<FoodRole> SNACK_ELIGIBLE =
            Set.of(HEALTHY_SNACK, FRUIT);

    /**
     * Resolves a possibly-null role (foods not yet reclassified hold NULL until 2.4C)
     * to a non-null value, keeping callers free of null checks.
     */
    public static FoodRole orUnspecified(FoodRole role) {
        return role != null ? role : UNSPECIFIED;
    }

    /**
     * One of the three pillars of a lunch/dinner: protein + carbohydrate + vegetable/legume.
     */
    public boolean isMainMealComponent() {
        return MAIN_MEAL_COMPONENTS.contains(this);
    }

    /**
     * Light enough to belong in a breakfast.
     */
    public boolean isBreakfastEligible() {
        return BREAKFAST_ELIGIBLE.contains(this);
    }

    /**
     * Valid as a morning/afternoon snack: a healthy snack OR a fruit.
     */
    public boolean isSnackEligible() {
        return SNACK_ELIGIBLE.contains(this);
    }
}
