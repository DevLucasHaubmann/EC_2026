package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "feedback_recomendacao", indexes = {
        @Index(name = "idx_feedback_recomendacao_id", columnList = "recomendacao_id")
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
    @JoinColumn(name = "recomendacao_id", nullable = false)
    private Recommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "avaliacao", nullable = false)
    private Rating rating;

    @Column(name = "motivo", length = 500)
    private String reason;

    @Column(name = "observacao", length = 1000)
    private String observation;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}