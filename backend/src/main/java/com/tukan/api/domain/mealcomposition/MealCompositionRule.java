package com.tukan.api.domain.mealcomposition;

import com.tukan.api.entity.FoodRole;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable composition contract for a single meal type.
 *
 * <p>A rule is satisfied when ALL required slots are individually satisfied by the
 * set of food roles selected for that meal (see {@link #isComplete(Collection)}).
 *
 * <p>{@link #allowedRoles()} is derived at construction time as the union of all
 * accepted roles across every slot — it is the set of roles that could ever contribute
 * to this meal type.
 */
public final class MealCompositionRule {

    private final MealType mealType;
    private final List<MealCompositionSlot> requiredSlots;
    private final Set<FoodRole> allowedRoles;

    /**
     * @param mealType      the meal type this rule governs; must not be {@code null}
     * @param requiredSlots the ordered list of required slots; must not be null or empty
     */
    MealCompositionRule(MealType mealType, List<MealCompositionSlot> requiredSlots) {
        Objects.requireNonNull(mealType, "mealType must not be null");
        Objects.requireNonNull(requiredSlots, "requiredSlots must not be null");
        if (requiredSlots.isEmpty()) {
            throw new IllegalArgumentException("requiredSlots must not be empty");
        }
        this.mealType = mealType;
        this.requiredSlots = List.copyOf(requiredSlots);
        this.allowedRoles = requiredSlots.stream()
                .flatMap(slot -> slot.acceptedRoles().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns the meal type this rule governs. */
    public MealType mealType() {
        return mealType;
    }

    /** Returns the immutable ordered list of required composition slots. */
    public List<MealCompositionSlot> requiredSlots() {
        return requiredSlots;
    }

    /**
     * Returns the immutable union of all accepted roles across every slot.
     * {@code FoodRole.UNSPECIFIED} is never present here by the {@link MealCompositionSlot}
     * constructor invariant.
     */
    public Set<FoodRole> allowedRoles() {
        return allowedRoles;
    }

    /**
     * Returns {@code true} if the given role appears in the allowed set for this meal type.
     *
     * <p>{@code null} is mapped to {@code UNSPECIFIED} via {@link FoodRole#orUnspecified},
     * which is never in the allowed set, so {@code null} always returns {@code false}.
     *
     * @param role the role to test; may be {@code null}
     */
    public boolean isRoleAllowed(FoodRole role) {
        return allowedRoles.contains(FoodRole.orUnspecified(role));
    }

    /**
     * Returns {@code true} when every required slot is satisfied by at least one role in
     * {@code selectedRoles}.
     *
     * <p>Complexity: O(slots * selectedRoles). Both are small constants (≤ 3 slots,
     * ≤ 5 food roles per meal in practice) — acceptable without further optimization.
     *
     * @param selectedRoles the roles of the foods chosen for this meal; may be {@code null}
     * @return {@code false} if {@code selectedRoles} is null; {@code true} only when all
     *         slots report {@link MealCompositionSlot#isSatisfiedBy(Collection)} as true
     */
    public boolean isComplete(Collection<FoodRole> selectedRoles) {
        if (selectedRoles == null) {
            return false;
        }
        return requiredSlots.stream().allMatch(slot -> slot.isSatisfiedBy(selectedRoles));
    }
}
