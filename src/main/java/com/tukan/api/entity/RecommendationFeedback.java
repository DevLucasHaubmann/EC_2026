package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recommendation_feedback", indexes = {
        @Index(name = "idx_feedback_recommendation_id", columnList = "recommendation_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RecommendationFeedback {

    public enum Rating {
        LIKED,
        DISLIKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false)
    private Rating rating;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "observation", length = 1000)
    private String observation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}