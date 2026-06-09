package com.tukan.api.domain.mealcomposition;

import com.tukan.api.entity.FoodRole;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * A single composition requirement for a meal, satisfied by AT LEAST ONE of the accepted roles.
 *
 * <p>Example: the "vegetable/legume" slot accepts {@code VEGETABLE_LEGUME} only; the
 * "light breakfast item" slot accepts any of {@code BREAKFAST_LIGHT}, {@code LIGHT_DAIRY},
 * {@code FRUIT}, or {@code HEALTHY_SNACK}.
 *
 * <p>{@code FoodRole.UNSPECIFIED} is never a valid accepted role — the compact constructor
 * enforces this invariant. {@link #accepts(FoodRole)} maps {@code null} to
 * {@code UNSPECIFIED} via {@link FoodRole#orUnspecified}, which then safely returns
 * {@code false} without calling {@code Set.contains(null)}.
 */
public record MealCompositionSlot(String name, Set<FoodRole> acceptedRoles) {

    /**
     * Compact constructor: validates invariants and makes the set defensively immutable.
     */
    public MealCompositionSlot {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(acceptedRoles, "acceptedRoles must not be null");
        if (acceptedRoles.isEmpty()) {
            throw new IllegalArgumentException("acceptedRoles must not be empty");
        }
        if (acceptedRoles.contains(FoodRole.UNSPECIFIED)) {
            throw new IllegalArgumentException(
                    "UNSPECIFIED is not a valid accepted role in a composition slot");
        }
        acceptedRoles = Set.copyOf(acceptedRoles);
    }

    /**
     * Returns {@code true} if the given role satisfies this slot.
     *
     * <p>{@code null} is mapped to {@code UNSPECIFIED} via {@link FoodRole#orUnspecified},
     * which is never present in the accepted set, so {@code null} always returns {@code false}
     * without triggering a {@link NullPointerException} on the immutable set.
     *
     * @param role the role to test; may be {@code null}
     * @return {@code true} if {@code acceptedRoles} contains the resolved role
     */
    public boolean accepts(FoodRole role) {
        return acceptedRoles.contains(FoodRole.orUnspecified(role));
    }

    /**
     * Returns {@code true} if at least one role in {@code selectedRoles} satisfies this slot.
     *
     * @param selectedRoles the roles of the foods chosen for the meal; may be {@code null}
     * @return {@code false} if {@code selectedRoles} is null or empty; {@code true} otherwise
     *         if any role satisfies {@link #accepts(FoodRole)}
     */
    public boolean isSatisfiedBy(Collection<FoodRole> selectedRoles) {
        if (selectedRoles == null || selectedRoles.isEmpty()) {
            return false;
        }
        for (FoodRole role : selectedRoles) {
            if (accepts(role)) {
                return true;
            }
        }
        return false;
    }
}
