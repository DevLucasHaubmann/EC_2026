package com.tukan.api.domain.mealcomposition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MealValidationResult — invariantes do record")
class MealValidationResultTest {

    @Test
    @DisplayName("unsatisfiedSlots null é normalizado para lista vazia")
    void slotsNullNormalizado() {
        MealValidationResult result = new MealValidationResult(false, true, null);

        assertThat(result.unsatisfiedSlots()).isEmpty();
        assertThat(result.hasUnsatisfiedSlots()).isFalse();
    }

    @Test
    @DisplayName("hasUnsatisfiedSlots true quando há slots insatisfeitos")
    void hasUnsatisfiedSlots() {
        MealValidationResult result =
                new MealValidationResult(false, false, List.of("main protein"));

        assertThat(result.hasUnsatisfiedSlots()).isTrue();
        assertThat(result.unsatisfiedSlots()).containsExactly("main protein");
    }

    @Test
    @DisplayName("Lista de slots é defensivamente imutável")
    void listaImutavel() {
        List<String> mutable = new ArrayList<>(List.of("vegetable/legume"));
        MealValidationResult result = new MealValidationResult(false, false, mutable);

        mutable.add("main protein");

        assertThat(result.unsatisfiedSlots()).containsExactly("vegetable/legume");
        assertThatThrownBy(() -> result.unsatisfiedSlots().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
