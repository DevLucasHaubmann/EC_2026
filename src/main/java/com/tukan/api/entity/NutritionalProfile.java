package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "perfil_nutricional", indexes = {
        @Index(name = "idx_perfil_usuario_id", columnList = "usuario_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class NutritionalProfile {

    public enum Gender {
        MASCULINO,
        FEMININO
    }

    public enum ActivityLevel {
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
    private User user;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false)
    private Gender gender;

    @Column(name = "peso_kg", nullable = false)
    private Double weightKg;

    @Column(name = "altura_cm", nullable = false)
    private Double heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_atividade", nullable = false)
    private ActivityLevel activityLevel;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "atualizado_em")
    private Instant updatedAt;

    public int calculateAge(LocalDate reference) {
        return Period.between(this.dateOfBirth, reference).getYears();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}