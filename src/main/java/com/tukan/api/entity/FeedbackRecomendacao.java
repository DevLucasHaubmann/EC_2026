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
public class FeedbackRecomendacao {

    public enum Avaliacao {
        GOSTOU,
        NAO_GOSTOU
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recomendacao_id", nullable = false)
    private Recomendacao recomendacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "avaliacao", nullable = false)
    private Avaliacao avaliacao;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "observacao", length = 1000)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = Instant.now();
    }
}
