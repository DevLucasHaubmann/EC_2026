package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "triagem", indexes = {
        @Index(name = "idx_triagem_usuario_id", columnList = "usuario_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Triagem {

    public enum ObjetivoNutricional {
        PERDA_DE_PESO,
        GANHO_DE_MASSA,
        MANUTENCAO,
        REEDUCACAO_ALIMENTAR,
        PERFORMANCE_ESPORTIVA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "objetivo", nullable = false)
    private ObjetivoNutricional objetivo;

    @Column(name = "restricoes_alimentares", length = 500)
    private String restricoesAlimentares;

    @Column(name = "alergias", length = 500)
    private String alergias;

    @Column(name = "condicoes_saude", length = 500)
    private String condicoesSaude;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = Instant.now();
    }
}
