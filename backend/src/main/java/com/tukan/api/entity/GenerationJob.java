package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the lifecycle of an asynchronous AI recommendation generation request.
 * The opaque {@code id} is exposed to the frontend as {@code jobId}.
 *
 * {@code recommendationId} is a plain nullable column (not a relation) on purpose:
 * status polling must never load the heavy Recommendation/plan_json payload.
 */
@Entity
@Table(name = "generation_job", indexes = {
        // (user_id, status) also serves user_id-only lookups via its leftmost prefix.
        @Index(name = "idx_generation_job_user_status", columnList = "user_id, status"),
        @Index(name = "idx_generation_job_status_updated_at", columnList = "status, updated_at")
})
@Getter
@Setter
@NoArgsConstructor
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GenerationJobStatus status = GenerationJobStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "recommendation_id")
    private Integer recommendationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
