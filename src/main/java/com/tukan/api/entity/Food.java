package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "alimento")
@Getter
@Setter
@NoArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false)
    private String name;

    @Column(name = "nome_normalizado")
    private String normalizedName;

    @Column(name = "calorias_100g")
    private BigDecimal caloriesPer100g;

    @Column(name = "proteina_100g")
    private BigDecimal proteinPer100g;

    @Column(name = "carboidrato_100g")
    private BigDecimal carbsPer100g;

    @Column(name = "gordura_100g")
    private BigDecimal fatPer100g;

    @Column(name = "fibra_100g")
    private BigDecimal fiberPer100g;

    @Column(name = "categoria", nullable = false)
    private String category;

    @Column(name = "subcategoria")
    private String subcategory;

    @Column(name = "tipo_refeicao_principal")
    private String primaryMealType;

    @Column(name = "refeicoes_indicadas")
    private String suitableMeals;

    @Column(name = "porcao_referencia_g")
    private BigDecimal referencePortionGrams;

    @Column(name = "contem_lactose", nullable = false)
    private boolean containsLactose;

    @Column(name = "contem_gluten", nullable = false)
    private boolean containsGluten;

    @Column(name = "contem_ovo", nullable = false)
    private boolean containsEgg;

    @Column(name = "vegetariano", nullable = false)
    private boolean vegetarian;

    @Column(name = "vegano", nullable = false)
    private boolean vegan;

    @Column(name = "restricoes_compativeis")
    private String compatibleRestrictions;

    @Column(name = "ativo", nullable = false)
    private boolean active;
}
