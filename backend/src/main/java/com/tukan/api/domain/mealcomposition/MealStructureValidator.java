package com.tukan.api.domain.mealcomposition;

import com.tukan.api.entity.FoodRole;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Pure structural validator for a generated meal: checks whether the roles actually selected
 * for a meal satisfy every required slot of its {@link MealCompositionRule}.
 *
 * <p>This validator never throws on business outcomes — it reports them. {@code UNSPECIFIED}
 * and {@code null} roles cannot satisfy any slot (a slot never accepts them), so they can never
 * mask an incomplete composition.
 */
public final class MealStructureValidator {

    private MealStructureValidator() {
        // utility class — not instantiable
    }

    /**
     * Validates the selected roles against the rule.
     *
     * @param rule          the composition rule for the meal; must not be {@code null}
     * @param selectedRoles the effective roles of the foods chosen for the meal; may be {@code null}
     * @return a {@link MealValidationResult} describing emptiness, completeness and missing slots
     */
    public static MealValidationResult validate(MealCompositionRule rule, Collection<FoodRole> selectedRoles) {
        Objects.requireNonNull(rule, "rule must not be null");

        boolean empty = selectedRoles == null || selectedRoles.isEmpty();

        List<String> unsatisfied = rule.requiredSlots().stream()
                .filter(slot -> !slot.isSatisfiedBy(selectedRoles))
                .map(MealCompositionSlot::name)
                .toList();

        return new MealValidationResult(empty, unsatisfied.isEmpty(), unsatisfied);
    }
}
