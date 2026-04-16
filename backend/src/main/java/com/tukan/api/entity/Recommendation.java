package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recommendation", indexes = {
        @Index(name = "idx_recommendation_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

    public enum RecommendationStatus {
        GENERATED,
        VIEWED,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "plan_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String planJson;

    @Column(name = "meal_explanations_json", columnDefinition = "TEXT")
    private String mealExplanationsJson;

    @Column(name = "tips_json", columnDefinition = "TEXT")
    private String tipsJson;

    @Column(name = "alerts_json", columnDefinition = "TEXT")
    private String alertsJson;

    @Column(name = "context_json", nullable = false, columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}