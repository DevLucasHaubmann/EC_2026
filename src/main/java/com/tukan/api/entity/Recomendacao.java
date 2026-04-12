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
public class Recomendacao {

    public enum StatusRecomendacao {
        GERADA,
        VISUALIZADA,
        ARQUIVADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(name = "resumo", nullable = false, columnDefinition = "TEXT")
    private String resumo;

    @Column(name = "recomendacoes", nullable = false, columnDefinition = "TEXT")
    private String recomendacoes;

    @Column(name = "alertas", nullable = false, columnDefinition = "TEXT")
    private String alertas;

    @Column(name = "contexto_json", nullable = false, columnDefinition = "TEXT")
    private String contextoJson;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusRecomendacao status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = Instant.now();
    }
}
