package com.tukan.api.dto;

import com.tukan.api.dto.bodyweight.RegisterBodyWeightRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure Bean Validation unit test for RegisterBodyWeightRequest.
 * No Spring context — uses the Jakarta Validator directly.
 * Boundary matrix: min=20.00, max=500.00, fraction limit=2 decimal places.
 */
class RegisterBodyWeightRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Set<ConstraintViolation<RegisterBodyWeightRequest>> validate(BigDecimal weight) {
        return validator.validate(new RegisterBodyWeightRequest(weight));
    }

    // -------------------------------------------------------------------------
    // Valid cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("75.99 — dentro do intervalo, 2 casas decimais — válido")
    void weight_75_99_isValid() {
        // given
        BigDecimal weight = new BigDecimal("75.99");

        // when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(weight);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("20.00 — mínimo exato — válido")
    void weight_20_00_isValid() {
        // given
        BigDecimal weight = new BigDecimal("20.00");

        // when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(weight);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("500.00 — máximo exato — válido")
    void weight_500_00_isValid() {
        // given
        BigDecimal weight = new BigDecimal("500.00");

        // when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(weight);

        // then
        assertThat(violations).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Invalid cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("75.999 — 3 casas decimais — inválido")
    void weight_75_999_isInvalid() {
        // given
        BigDecimal weight = new BigDecimal("75.999");

        // when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(weight);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("O peso deve ter no máximo 2 casas decimais."));
    }

    @Test
    @DisplayName("19.99 — abaixo do mínimo — inválido")
    void weight_19_99_isInvalid() {
        // given
        BigDecimal weight = new BigDecimal("19.99");

        // when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(weight);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("500.01 — acima do máximo — inválido")
    void weight_500_01_isInvalid() {
        // given
        BigDecimal weight = new BigDecimal("500.01");

        // when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(weight);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("null — campo obrigatório — inválido")
    void weight_null_isInvalid() {
        // given / when
        Set<ConstraintViolation<RegisterBodyWeightRequest>> violations = validate(null);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("O peso é obrigatório."));
    }
}
