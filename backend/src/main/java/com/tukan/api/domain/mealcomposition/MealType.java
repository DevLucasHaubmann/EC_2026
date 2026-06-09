package com.tukan.api.domain.mealcomposition;

import java.util.Optional;

/**
 * Typed representation of the five canonical meal types used in the meal plan engine.
 *
 * <p>The canonical string keys ("BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK",
 * "DINNER") defined in {@code MealDistributor} and {@code MealSuitabilityService} remain
 * the authoritative source of truth. This enum mirrors them 1:1 to allow type-safe
 * composition rule lookups without touching the engine.
 *
 * <p>Use {@link #fromCanonicalKey(String)} for safe conversion from engine-produced strings.
 */
public enum MealType {

    BREAKFAST,
    MORNING_SNACK,
    LUNCH,
    AFTERNOON_SNACK,
    DINNER,
    LUNCH_LOW_CARB,
    DINNER_LOW_CARB,
    LUNCH_CETOGENICA,
    DINNER_CETOGENICA,
    BREAKFAST_CETOGENICA,
    LUNCH_CARNIVORA,
    DINNER_CARNIVORA,
    BREAKFAST_CARNIVORA;

    /**
     * Converts a canonical engine key to a {@link MealType}, or {@link Optional#empty()} when
     * the key is null, blank, or does not match any known meal type.
     *
     * <p>Matching is null-safe, trims surrounding whitespace, and is case-insensitive.
     *
     * @param key canonical string key produced by the meal plan engine
     * @return the matching {@link MealType}, or {@link Optional#empty()} for unknown input
     */
    public static Optional<MealType> fromCanonicalKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        String normalized = key.trim().toUpperCase();
        for (MealType type : values()) {
            if (type.name().equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
