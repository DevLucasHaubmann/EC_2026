package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "meal_log", indexes = {
        @Index(name = "idx_meal_log_user_date", columnList = "user_id, meal_date"),
        @Index(name = "idx_meal_log_recommendation", columnList = "recommendation_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Plain Integer, not @ManyToOne — avoids loading the heavy plan_json (MEDIUMTEXT) via object graph.
    // Referential integrity is enforced by the FK constraint in the SQL schema.
    @Column(name = "recommendation_id", nullable = false)
    private Integer recommendationId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    // Stores the canonical MealType enum value (e.g. "BREAKFAST", "LUNCH_LOW_CARB").
    @Column(name = "meal_type", nullable = false, length = 40)
    private String mealType;

    @Column(name = "option_number", nullable = false)
    private Integer optionNumber;

    // Macros are copied at log time for historical immutability — future diet changes do not affect past logs.
    @Column(name = "calories", nullable = false)
    private Double calories;

    @Column(name = "protein", nullable = false)
    private Double protein;

    @Column(name = "carbs", nullable = false)
    private Double carbs;

    @Column(name = "fat", nullable = false)
    private Double fat;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
