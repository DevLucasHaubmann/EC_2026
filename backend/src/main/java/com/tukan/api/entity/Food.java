package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "food")
@Getter
@Setter
@NoArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "normalized_name")
    private String normalizedName;

    @Column(name = "calories_per_100g")
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_per_100g")
    private BigDecimal proteinPer100g;

    @Column(name = "carbs_per_100g")
    private BigDecimal carbsPer100g;

    @Column(name = "fat_per_100g")
    private BigDecimal fatPer100g;

    @Column(name = "fiber_per_100g")
    private BigDecimal fiberPer100g;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "primary_meal_type")
    private String primaryMealType;

    @Column(name = "suitable_meals")
    private String suitableMeals;

    /**
     * Functional role of the food in a meal composition (Task 2.4B).
     * Separate axis from {@link #category}. NULL until reclassified in 2.4C;
     * read it through {@link #getEffectiveRole()} for a non-null value.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "food_role", length = 40)
    private FoodRole foodRole;

    @Column(name = "reference_portion_grams")
    private BigDecimal referencePortionGrams;

    @Column(name = "contains_lactose", nullable = false)
    private boolean containsLactose;

    @Column(name = "contains_gluten", nullable = false)
    private boolean containsGluten;

    @Column(name = "contains_egg", nullable = false)
    private boolean containsEgg;

    @Column(name = "vegetarian", nullable = false)
    private boolean vegetarian;

    @Column(name = "vegan", nullable = false)
    private boolean vegan;

    @Column(name = "compatible_restrictions")
    private String compatibleRestrictions;

    @Column(name = "active", nullable = false)
    private boolean active;

    /**
     * Non-null view of {@link #foodRole}: foods not yet reclassified (NULL) are
     * treated as {@link FoodRole#UNSPECIFIED}. Keeps selection logic null-safe
     * during the retrocompatibility window before 2.4C.
     */
    public FoodRole getEffectiveRole() {
        return FoodRole.orUnspecified(foodRole);
    }
}
