package com.tukan.api.entity;

import com.tukan.api.converter.FeedbackTagListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendation_feedback", indexes = {
        @Index(name = "idx_feedback_recommendation_id", columnList = "recommendation_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RecommendationFeedback {

    public enum FeedbackTag {
        PRACTICAL,
        VARIED,
        AFFORDABLE,
        BALANCED,
        TASTY,
        TOO_RESTRICTIVE,
        REPETITIVE,
        EXPENSIVE,
        LACKING_PROTEIN,
        EASY_TO_PREPARE,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Convert(converter = FeedbackTagListConverter.class)
    @Column(name = "liked_tags", columnDefinition = "TEXT")
    private List<FeedbackTag> likedTags = new ArrayList<>();

    @Convert(converter = FeedbackTagListConverter.class)
    @Column(name = "disliked_tags", columnDefinition = "TEXT")
    private List<FeedbackTag> dislikedTags = new ArrayList<>();

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
