package com.tukan.api.domain.mealcomposition;

import com.tukan.api.entity.FoodRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MealStructureValidator — validação estrutural pura")
class MealStructureValidatorTest {

    private final MealCompositionRule lunchRule = MealCompositionRules.forMealType(MealType.LUNCH);
    private final MealCompositionRule breakfastRule = MealCompositionRules.forMealType(MealType.BREAKFAST);

    @Nested
    @DisplayName("Almoço/jantar (3 slots obrigatórios)")
    class MainMeal {

        @Test
        @DisplayName("Os 3 papéis presentes → completo, sem slots insatisfeitos")
        void todosOsPapeisPresentes() {
            // given
            List<FoodRole> roles = List.of(
                    FoodRole.MAIN_PROTEIN, FoodRole.MAIN_CARBOHYDRATE, FoodRole.VEGETABLE_LEGUME);

            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, roles);

            // then
            assertThat(result.empty()).isFalse();
            assertThat(result.complete()).isTrue();
            assertThat(result.unsatisfiedSlots()).isEmpty();
            assertThat(result.hasUnsatisfiedSlots()).isFalse();
        }

        @Test
        @DisplayName("Falta o carboidrato → incompleto, slot 'main carbohydrate' insatisfeito")
        void faltaCarboidrato() {
            // given
            List<FoodRole> roles = List.of(FoodRole.MAIN_PROTEIN, FoodRole.VEGETABLE_LEGUME);

            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, roles);

            // then
            assertThat(result.complete()).isFalse();
            assertThat(result.unsatisfiedSlots()).containsExactly("main carbohydrate");
        }

        @Test
        @DisplayName("Falta verdura/legume → incompleto, slot 'vegetable/legume' insatisfeito")
        void faltaVerdura() {
            // given
            List<FoodRole> roles = List.of(FoodRole.MAIN_PROTEIN, FoodRole.MAIN_CARBOHYDRATE);

            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, roles);

            // then
            assertThat(result.complete()).isFalse();
            assertThat(result.unsatisfiedSlots()).containsExactly("vegetable/legume");
        }
    }

    @Nested
    @DisplayName("UNSPECIFIED e null não mascaram baixa qualidade")
    class UnspecifiedAndNull {

        @Test
        @DisplayName("Apenas UNSPECIFIED → não satisfaz nenhum slot, incompleto")
        void apenasUnspecified() {
            // given
            List<FoodRole> roles = List.of(FoodRole.UNSPECIFIED, FoodRole.UNSPECIFIED);

            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, roles);

            // then
            assertThat(result.empty()).isFalse();
            assertThat(result.complete()).isFalse();
            assertThat(result.unsatisfiedSlots())
                    .containsExactlyInAnyOrder("main protein", "main carbohydrate", "vegetable/legume");
        }

        @Test
        @DisplayName("UNSPECIFIED ao lado de papéis válidos não conta como nenhum slot")
        void unspecifiedNaoConta() {
            // given
            List<FoodRole> roles = List.of(FoodRole.MAIN_PROTEIN, FoodRole.UNSPECIFIED, FoodRole.UNSPECIFIED);

            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, roles);

            // then — apenas a proteína conta; carb e verdura seguem insatisfeitos
            assertThat(result.complete()).isFalse();
            assertThat(result.unsatisfiedSlots())
                    .containsExactlyInAnyOrder("main carbohydrate", "vegetable/legume");
        }

        @Test
        @DisplayName("selectedRoles null → empty=true, todos os slots insatisfeitos, sem NPE")
        void rolesNull() {
            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, null);

            // then
            assertThat(result.empty()).isTrue();
            assertThat(result.complete()).isFalse();
            assertThat(result.unsatisfiedSlots()).hasSize(3);
        }

        @Test
        @DisplayName("Lista vazia → empty=true e incompleto")
        void rolesVazio() {
            // when
            MealValidationResult result = MealStructureValidator.validate(lunchRule, List.of());

            // then
            assertThat(result.empty()).isTrue();
            assertThat(result.complete()).isFalse();
        }
    }

    @Nested
    @DisplayName("Café (slot único, papéis leves)")
    class Breakfast {

        @Test
        @DisplayName("FRUIT satisfaz o slot leve do café → completo")
        void frutaSatisfazCafe() {
            // when
            MealValidationResult result =
                    MealStructureValidator.validate(breakfastRule, List.of(FoodRole.FRUIT));

            // then
            assertThat(result.complete()).isTrue();
        }

        @Test
        @DisplayName("Apenas HEALTHY_FAT não satisfaz o café (papel não-elegível)")
        void healthyFatNaoSatisfazCafe() {
            // when
            MealValidationResult result =
                    MealStructureValidator.validate(breakfastRule, List.of(FoodRole.HEALTHY_FAT));

            // then
            assertThat(result.complete()).isFalse();
            assertThat(result.unsatisfiedSlots()).hasSize(1);
        }
    }

    @Test
    @DisplayName("rule null lança NullPointerException")
    void ruleNull() {
        assertThatThrownBy(() -> MealStructureValidator.validate(null, List.of(FoodRole.FRUIT)))
                .isInstanceOf(NullPointerException.class);
    }
}
