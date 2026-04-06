package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "perfil_nutricional", indexes = {
        @Index(name = "idx_perfil_usuario_id", columnList = "usuario_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Perfil {

    public enum Sexo {
        MASCULINO,
        FEMININO
    }

    public enum NivelAtividade {
        SEDENTARIO,
        LEVE,
        MODERADO,
        INTENSO,
        MUITO_INTENSO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User usuario;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false)
    private Sexo sexo;

    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "altura_cm", nullable = false)
    private Double alturaCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_atividade", nullable = false)
    private NivelAtividade nivelAtividade;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;
}
