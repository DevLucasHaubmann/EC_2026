package com.tukan.api.domain.mealcomposition;

import java.util.List;

/**
 * Immutable result of validating a meal's structural composition against its
 * {@link MealCompositionRule}.
 *
 * @param empty             {@code true} when no food was selected for the meal
 * @param complete          {@code true} when every required slot is satisfied
 * @param unsatisfiedSlots  names of the required slots left unsatisfied (empty when complete)
 */
public record MealValidationResult(boolean empty, boolean complete, List<String> unsatisfiedSlots) {

    public MealValidationResult {
        unsatisfiedSlots = unsatisfiedSlots == null ? List.of() : List.copyOf(unsatisfiedSlots);
    }

    /** Returns {@code true} when at least one required slot was not satisfied. */
    public boolean hasUnsatisfiedSlots() {
        return !unsatisfiedSlots.isEmpty();
    }
}
