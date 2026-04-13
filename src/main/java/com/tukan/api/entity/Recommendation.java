package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recomendacao", indexes = {
        @Index(name = "idx_recomendacao_usuario_id", columnList = "usuario_id")
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
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "resumo", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "plano_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String planJson;

    @Column(name = "explicacao_refeicoes_json", columnDefinition = "TEXT")
    private String mealExplanationsJson;

    @Column(name = "dicas_json", columnDefinition = "TEXT")
    private String tipsJson;

    @Column(name = "alertas_json", columnDefinition = "TEXT")
    private String alertsJson;

    @Column(name = "contexto_json", nullable = false, columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}