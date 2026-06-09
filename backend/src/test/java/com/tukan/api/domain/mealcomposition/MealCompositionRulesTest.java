package com.tukan.api.domain.mealcomposition;

import com.tukan.api.entity.FoodRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MealCompositionRules — Sprint 2.4D")
class MealCompositionRulesTest {

    // =========================================================================
    // 1. BREAKFAST
    // =========================================================================

    @Nested
    @DisplayName("1. Café da manhã (BREAKFAST)")
    class BreakfastTests {

        private final MealCompositionRule rule = MealCompositionRules.forMealType(MealType.BREAKFAST);

        @Test
        @DisplayName("Aceita BREAKFAST_LIGHT como papel permitido")
        void allowsBreakfastLight() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.BREAKFAST_LIGHT)).isTrue();
        }

        @Test
        @DisplayName("Aceita FRUIT como papel permitido")
        void allowsFruit() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.FRUIT)).isTrue();
        }

        @Test
        @DisplayName("Aceita LIGHT_DAIRY como papel permitido")
        void allowsLightDairy() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.LIGHT_DAIRY)).isTrue();
        }

        @Test
        @DisplayName("Aceita HEALTHY_SNACK como papel permitido")
        void allowsHealthySnack() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_SNACK)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna true com apenas FRUIT")
        void isCompleteWithFruitOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.FRUIT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna true com apenas BREAKFAST_LIGHT")
        void isCompleteWithBreakfastLightOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.BREAKFAST_LIGHT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("Rejeita MAIN_PROTEIN como papel permitido")
        void rejectsMainProtein() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isFalse();
        }

        @Test
        @DisplayName("Rejeita MAIN_CARBOHYDRATE como papel permitido")
        void rejectsMainCarbohydrate() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @Test
        @DisplayName("Rejeita VEGETABLE_LEGUME como papel permitido")
        void rejectsVegetableLegume() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.VEGETABLE_LEGUME)).isFalse();
        }

        @Test
        @DisplayName("isComplete retorna false com MAIN_PROTEIN")
        void isCompleteReturnsFalseWithMainProtein() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }

        @Test
        @DisplayName("Rejeita UNSPECIFIED como papel permitido")
        void rejectsUnspecified() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.UNSPECIFIED)).isFalse();
        }

        @Test
        @DisplayName("isRoleAllowed(null) retorna false sem NPE")
        void nullRoleReturnsFalseWithoutNpe() {
            // given / when / then
            assertThat(rule.isRoleAllowed(null)).isFalse();
        }

        @Test
        @DisplayName("Não exige múltiplos papéis — um papel leve é suficiente para isComplete")
        void singleLightRoleIsSufficientForCompletion() {
            // given
            List<FoodRole> selected = List.of(FoodRole.LIGHT_DAIRY);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }
    }

    // =========================================================================
    // 2. LANCHES (MORNING_SNACK e AFTERNOON_SNACK)
    // =========================================================================

    @Nested
    @DisplayName("2. Lanches (MORNING_SNACK e AFTERNOON_SNACK)")
    class SnackTests {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("Aceita FRUIT como papel permitido")
        void allowsFruit(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.FRUIT)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("Aceita HEALTHY_SNACK como papel permitido")
        void allowsHealthySnack(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_SNACK)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("isComplete retorna true com apenas FRUIT")
        void isCompleteWithFruit(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.FRUIT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("isComplete retorna true com apenas HEALTHY_SNACK")
        void isCompleteWithHealthySnack(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.HEALTHY_SNACK);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("Não exige fruta E snack juntos — um basta")
        void doesNotRequireBothFruitAndSnack(MealType mealType) {
            // given — apenas fruit, sem snack
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.FRUIT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("Rejeita MAIN_PROTEIN como papel de lanche")
        void rejectsMainProtein(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"MORNING_SNACK", "AFTERNOON_SNACK"})
        @DisplayName("LIGHT_DAIRY não é permitido em lanche — consistente com isSnackEligible()")
        void lightDairyNotAllowedInSnack(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.LIGHT_DAIRY)).isFalse();
        }
    }

    // =========================================================================
    // 3. ALMOÇO (LUNCH)
    // =========================================================================

    @Nested
    @DisplayName("3. Almoço (LUNCH)")
    class LunchTests {

        private final MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH);

        @Test
        @DisplayName("isComplete retorna true com os três papéis exigidos")
        void isCompleteWithAllThreeRoles() {
            // given
            List<FoodRole> selected = List.of(
                    FoodRole.MAIN_PROTEIN, FoodRole.MAIN_CARBOHYDRATE, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna false sem VEGETABLE_LEGUME")
        void isIncompleteWithoutVegetableLegume() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN, FoodRole.MAIN_CARBOHYDRATE);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }

        @Test
        @DisplayName("isComplete retorna false sem MAIN_CARBOHYDRATE")
        void isIncompleteWithoutCarbohydrate() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }

        @Test
        @DisplayName("isComplete retorna false sem MAIN_PROTEIN")
        void isIncompleteWithoutProtein() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_CARBOHYDRATE, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }
    }

    // =========================================================================
    // 4. JANTAR (DINNER)
    // =========================================================================

    @Nested
    @DisplayName("4. Jantar (DINNER)")
    class DinnerTests {

        private final MealCompositionRule rule = MealCompositionRules.forMealType(MealType.DINNER);

        @Test
        @DisplayName("isComplete retorna true com os três papéis exigidos")
        void isCompleteWithAllThreeRoles() {
            // given
            List<FoodRole> selected = List.of(
                    FoodRole.MAIN_PROTEIN, FoodRole.MAIN_CARBOHYDRATE, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("requiredSlots() contém exatamente 3 slots")
        void hasThreeRequiredSlots() {
            // given / when / then
            assertThat(rule.requiredSlots()).hasSize(3);
        }

        @Test
        @DisplayName("Mesma regra do almoço — isComplete false sem qualquer pilar")
        void isIncompleteWithOnlyProteinAndVegetable() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }
    }

    // =========================================================================
    // 5. LOW_CARB (LUNCH_LOW_CARB e DINNER_LOW_CARB)
    // =========================================================================

    @Nested
    @DisplayName("5. Almoço low carb (LUNCH_LOW_CARB)")
    class LunchLowCarbTests {

        private final MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH_LOW_CARB);

        @Test
        @DisplayName("Tem slot que aceita MAIN_PROTEIN")
        void lunchLowCarb_hasMainProteinSlot() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
        }

        @Test
        @DisplayName("Tem slot que aceita VEGETABLE_LEGUME")
        void lunchLowCarb_hasVegetableLegumeSlot() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.VEGETABLE_LEGUME)).isTrue();
        }

        @Test
        @DisplayName("Tem slot que aceita HEALTHY_FAT")
        void lunchLowCarb_hasHealthyFatSlot() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isTrue();
        }

        @Test
        @DisplayName("Não tem slot que aceita MAIN_CARBOHYDRATE")
        void lunchLowCarb_doesNotHaveMainCarbSlot() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @Test
        @DisplayName("isComplete retorna true com MAIN_PROTEIN + VEGETABLE_LEGUME + HEALTHY_FAT")
        void lunchLowCarb_isCompleteWithAllThreeSlots() {
            // given
            List<FoodRole> selected = List.of(
                    FoodRole.MAIN_PROTEIN, FoodRole.VEGETABLE_LEGUME, FoodRole.HEALTHY_FAT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna false com MAIN_PROTEIN + MAIN_CARBOHYDRATE + VEGETABLE_LEGUME (sem HEALTHY_FAT)")
        void lunchLowCarb_isIncompleteWithStandardComposition() {
            // given
            List<FoodRole> selected = List.of(
                    FoodRole.MAIN_PROTEIN, FoodRole.MAIN_CARBOHYDRATE, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }
    }

    @Nested
    @DisplayName("5b. Jantar low carb (DINNER_LOW_CARB)")
    class DinnerLowCarbTests {

        private final MealCompositionRule rule = MealCompositionRules.forMealType(MealType.DINNER_LOW_CARB);

        @Test
        @DisplayName("Tem slot que aceita MAIN_PROTEIN")
        void dinnerLowCarb_hasMainProteinSlot() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
        }

        @Test
        @DisplayName("Não tem slot que aceita MAIN_CARBOHYDRATE")
        void dinnerLowCarb_doesNotHaveMainCarbSlot() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @Test
        @DisplayName("isComplete retorna true com MAIN_PROTEIN + VEGETABLE_LEGUME + HEALTHY_FAT")
        void dinnerLowCarb_isCompleteWithAllThreeSlots() {
            // given
            List<FoodRole> selected = List.of(
                    FoodRole.MAIN_PROTEIN, FoodRole.VEGETABLE_LEGUME, FoodRole.HEALTHY_FAT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }
    }

    @Nested
    @DisplayName("5c. Regressão — LUNCH e DINNER padrão preservam MAIN_CARBOHYDRATE")
    class StandardMealRegressionTests {

        @Test
        @DisplayName("LUNCH padrão ainda aceita MAIN_CARBOHYDRATE")
        void lunchStandard_stillHasMainCarbSlot() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isTrue();
        }

        @Test
        @DisplayName("DINNER padrão ainda aceita MAIN_CARBOHYDRATE")
        void dinnerStandard_stillHasMainCarbSlot() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.DINNER);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isTrue();
        }
    }

    // =========================================================================
    // 5d. CETOGENICA (LUNCH_CETOGENICA e DINNER_CETOGENICA) — Task 2.8E
    // =========================================================================

    @Nested
    @DisplayName("5d. Cetogênica (LUNCH_CETOGENICA e DINNER_CETOGENICA)")
    class KetoTests {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CETOGENICA", "DINNER_CETOGENICA"})
        @DisplayName("Regra existe e é não-null")
        void keto_ruleExists(MealType mealType) {
            // given / when / then
            assertThat(MealCompositionRules.forMealType(mealType)).isNotNull();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CETOGENICA", "DINNER_CETOGENICA"})
        @DisplayName("Exige MAIN_PROTEIN")
        void keto_hasMainProteinSlot(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CETOGENICA", "DINNER_CETOGENICA"})
        @DisplayName("Exige HEALTHY_FAT (papel estrutural)")
        void keto_hasHealthyFatSlot(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CETOGENICA", "DINNER_CETOGENICA"})
        @DisplayName("Não exige MAIN_CARBOHYDRATE")
        void keto_doesNotHaveMainCarbSlot(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CETOGENICA", "DINNER_CETOGENICA"})
        @DisplayName("isComplete retorna true com MAIN_PROTEIN + HEALTHY_FAT")
        void keto_isCompleteWithProteinAndFat(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN, FoodRole.HEALTHY_FAT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CETOGENICA", "DINNER_CETOGENICA"})
        @DisplayName("isComplete retorna false sem HEALTHY_FAT")
        void keto_isIncompleteWithoutHealthyFat(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }

        @Test
        @DisplayName("Regressão: LOW_CARB permanece intacto — LUNCH_LOW_CARB ainda exige VEGETABLE_LEGUME")
        void keto_doesNotBreakLowCarb() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH_LOW_CARB);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.VEGETABLE_LEGUME)).isTrue();
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isTrue();
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }
    }

    // =========================================================================
    // 5e. CARNIVORA (LUNCH_CARNIVORA e DINNER_CARNIVORA) — Task 2.8F
    // =========================================================================

    @Nested
    @DisplayName("5e. Carnívora (LUNCH_CARNIVORA e DINNER_CARNIVORA)")
    class CarnivoraTests {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("Regra existe e é não-null")
        void carnivora_ruleExists(MealType mealType) {
            // given / when / then
            assertThat(MealCompositionRules.forMealType(mealType)).isNotNull();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("Exige MAIN_PROTEIN")
        void carnivora_hasMainProteinSlot(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("Não exige MAIN_CARBOHYDRATE")
        void carnivora_doesNotHaveMainCarbSlot(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("Não exige VEGETABLE_LEGUME")
        void carnivora_doesNotHaveVegetableLegumeSlot(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.VEGETABLE_LEGUME)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("HEALTHY_FAT não é estrutural (pool de gordura é de origem mista)")
        void carnivora_doesNotRequireHealthyFat(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("isComplete retorna true apenas com MAIN_PROTEIN")
        void carnivora_isCompleteWithProteinOnly(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = MealType.class, names = {"LUNCH_CARNIVORA", "DINNER_CARNIVORA"})
        @DisplayName("isComplete retorna false sem MAIN_PROTEIN")
        void carnivora_isIncompleteWithoutProtein(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            List<FoodRole> selected = List.of(FoodRole.HEALTHY_FAT, FoodRole.VEGETABLE_LEGUME);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }

        @Test
        @DisplayName("Regressão: CETOGENICA permanece intacta — LUNCH_CETOGENICA ainda exige HEALTHY_FAT")
        void carnivora_doesNotBreakKeto() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH_CETOGENICA);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isTrue();
        }

        @Test
        @DisplayName("Regressão: LOW_CARB permanece intacta — LUNCH_LOW_CARB ainda exige VEGETABLE_LEGUME")
        void carnivora_doesNotBreakLowCarb() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH_LOW_CARB);
            // when / then
            assertThat(rule.isRoleAllowed(FoodRole.VEGETABLE_LEGUME)).isTrue();
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isTrue();
        }
    }

    // =========================================================================
    // 5f. CAFÉ DA MANHÃ CETOGÊNICO (BREAKFAST_CETOGENICA) — Task 2.8H.1
    // =========================================================================

    @Nested
    @DisplayName("5f. Café da manhã cetogênico (BREAKFAST_CETOGENICA)")
    class BreakfastKetoTests {

        private final MealCompositionRule rule =
                MealCompositionRules.forMealType(MealType.BREAKFAST_CETOGENICA);

        @Test
        @DisplayName("Regra existe e é não-null")
        void breakfastKeto_ruleExists() {
            // given / when / then
            assertThat(rule).isNotNull();
        }

        @Test
        @DisplayName("Não exige BREAKFAST_LIGHT (causa raiz do blocker)")
        void breakfastKeto_doesNotAllowBreakfastLight() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.BREAKFAST_LIGHT)).isFalse();
        }

        @Test
        @DisplayName("Não exige MAIN_CARBOHYDRATE")
        void breakfastKeto_doesNotAllowMainCarbohydrate() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @Test
        @DisplayName("Não aceita FRUIT (dieta sem carboidrato)")
        void breakfastKeto_doesNotAllowFruit() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.FRUIT)).isFalse();
        }

        @Test
        @DisplayName("Aceita MAIN_PROTEIN")
        void breakfastKeto_allowsMainProtein() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
        }

        @Test
        @DisplayName("Aceita HEALTHY_FAT")
        void breakfastKeto_allowsHealthyFat() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.HEALTHY_FAT)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna true apenas com MAIN_PROTEIN")
        void breakfastKeto_isCompleteWithProteinOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna true apenas com HEALTHY_FAT")
        void breakfastKeto_isCompleteWithHealthyFatOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.HEALTHY_FAT);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna false com apenas MAIN_CARBOHYDRATE")
        void breakfastKeto_isIncompleteWithCarbOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_CARBOHYDRATE);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }
    }

    // =========================================================================
    // 5g. CAFÉ DA MANHÃ CARNÍVORO (BREAKFAST_CARNIVORA) — Task 2.8H.1
    // =========================================================================

    @Nested
    @DisplayName("5g. Café da manhã carnívoro (BREAKFAST_CARNIVORA)")
    class BreakfastCarnivoraTests {

        private final MealCompositionRule rule =
                MealCompositionRules.forMealType(MealType.BREAKFAST_CARNIVORA);

        @Test
        @DisplayName("Regra existe e é não-null")
        void breakfastCarnivora_ruleExists() {
            // given / when / then
            assertThat(rule).isNotNull();
        }

        @Test
        @DisplayName("Não exige BREAKFAST_LIGHT (causa raiz do blocker)")
        void breakfastCarnivora_doesNotAllowBreakfastLight() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.BREAKFAST_LIGHT)).isFalse();
        }

        @Test
        @DisplayName("Não exige MAIN_CARBOHYDRATE")
        void breakfastCarnivora_doesNotAllowMainCarbohydrate() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_CARBOHYDRATE)).isFalse();
        }

        @Test
        @DisplayName("Não aceita VEGETABLE_LEGUME")
        void breakfastCarnivora_doesNotAllowVegetableLegume() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.VEGETABLE_LEGUME)).isFalse();
        }

        @Test
        @DisplayName("Não aceita FRUIT")
        void breakfastCarnivora_doesNotAllowFruit() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.FRUIT)).isFalse();
        }

        @Test
        @DisplayName("Aceita MAIN_PROTEIN (estrutural)")
        void breakfastCarnivora_allowsMainProtein() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.MAIN_PROTEIN)).isTrue();
        }

        @Test
        @DisplayName("Aceita LIGHT_DAIRY (laticínio animal)")
        void breakfastCarnivora_allowsLightDairy() {
            // given / when / then
            assertThat(rule.isRoleAllowed(FoodRole.LIGHT_DAIRY)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna true apenas com MAIN_PROTEIN")
        void breakfastCarnivora_isCompleteWithProteinOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.MAIN_PROTEIN);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna true apenas com LIGHT_DAIRY")
        void breakfastCarnivora_isCompleteWithDairyOnly() {
            // given
            List<FoodRole> selected = List.of(FoodRole.LIGHT_DAIRY);
            // when / then
            assertThat(rule.isComplete(selected)).isTrue();
        }

        @Test
        @DisplayName("isComplete retorna false sem papel animal de café")
        void breakfastCarnivora_isIncompleteWithoutAnimalRole() {
            // given
            List<FoodRole> selected = List.of(FoodRole.VEGETABLE_LEGUME, FoodRole.FRUIT);
            // when / then
            assertThat(rule.isComplete(selected)).isFalse();
        }
    }

    // =========================================================================
    // 6. SEGURANÇA DE CONTRATO
    // =========================================================================

    @Nested
    @DisplayName("5. Segurança de contrato")
    class ContractSafetyTests {

        @ParameterizedTest(name = "forMealType({0}) não lança e retorna não-null")
        @EnumSource(MealType.class)
        @DisplayName("forMealType retorna regra não-null para todo MealType")
        void forMealTypeNeverThrowsAndReturnsNonNull(MealType mealType) {
            // given / when
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // then
            assertThat(rule).isNotNull();
        }

        @ParameterizedTest(name = "allowedRoles de {0} não contém UNSPECIFIED")
        @EnumSource(MealType.class)
        @DisplayName("Nenhum allowedRoles() contém UNSPECIFIED")
        void allowedRolesNeverContainsUnspecified(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.allowedRoles()).doesNotContain(FoodRole.UNSPECIFIED);
        }

        @ParameterizedTest(name = "isComplete(null) em {0} retorna false sem NPE")
        @EnumSource(MealType.class)
        @DisplayName("isComplete(null) retorna false sem NPE para todo MealType")
        void isCompleteWithNullReturnsFalse(MealType mealType) {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(mealType);
            // when / then
            assertThat(rule.isComplete(null)).isFalse();
        }

        @Test
        @DisplayName("forCanonicalKey(\"LUNCH\") retorna regra presente")
        void forCanonicalKeyUppercase() {
            // given / when / then
            assertThat(MealCompositionRules.forCanonicalKey("LUNCH")).isPresent();
        }

        @Test
        @DisplayName("forCanonicalKey(\"lunch\") retorna regra presente (case-insensitive)")
        void forCanonicalKeyLowercase() {
            // given / when / then
            assertThat(MealCompositionRules.forCanonicalKey("lunch")).isPresent();
        }

        @Test
        @DisplayName("forCanonicalKey(\" Lunch \") retorna regra presente (trim)")
        void forCanonicalKeyWithWhitespace() {
            // given / when / then
            assertThat(MealCompositionRules.forCanonicalKey(" Lunch ")).isPresent();
        }

        @Test
        @DisplayName("forCanonicalKey(null) retorna Optional.empty()")
        void forCanonicalKeyNull() {
            // given / when / then
            assertThat(MealCompositionRules.forCanonicalKey(null)).isEmpty();
        }

        @Test
        @DisplayName("forCanonicalKey(\"XPTO\") retorna Optional.empty()")
        void forCanonicalKeyUnknown() {
            // given / when / then
            assertThat(MealCompositionRules.forCanonicalKey("XPTO")).isEmpty();
        }
    }

    // =========================================================================
    // 6. IMUTABILIDADE
    // =========================================================================

    @Nested
    @DisplayName("6. Imutabilidade")
    class ImmutabilityTests {

        @Test
        @DisplayName("requiredSlots() lança UnsupportedOperationException ao tentar add")
        void requiredSlotsIsUnmodifiable() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH);
            List<MealCompositionSlot> slots = rule.requiredSlots();
            // when / then
            assertThatThrownBy(() -> slots.add(MealCompositionRules.MAIN_PROTEIN_SLOT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("allowedRoles() lança UnsupportedOperationException ao tentar add")
        void allowedRolesIsUnmodifiable() {
            // given
            MealCompositionRule rule = MealCompositionRules.forMealType(MealType.LUNCH);
            Set<FoodRole> roles = rule.allowedRoles();
            // when / then
            assertThatThrownBy(() -> roles.add(FoodRole.COMPLEMENT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("MealCompositionSlot.acceptedRoles() lança UnsupportedOperationException ao tentar add")
        void slotAcceptedRolesIsUnmodifiable() {
            // given
            MealCompositionSlot slot = MealCompositionRules.MAIN_PROTEIN_SLOT;
            Set<FoodRole> roles = slot.acceptedRoles();
            // when / then
            assertThatThrownBy(() -> roles.add(FoodRole.COMPLEMENT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("MealCompositionSlot rejeita Set vazio no construtor")
        void slotConstructorRejectsEmptySet() {
            // given / when / then
            assertThatThrownBy(() -> new MealCompositionSlot("empty", Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("MealCompositionSlot rejeita Set contendo UNSPECIFIED no construtor")
        void slotConstructorRejectsUnspecified() {
            // given / when / then
            assertThatThrownBy(() -> new MealCompositionSlot("bad",
                    Set.of(FoodRole.MAIN_PROTEIN, FoodRole.UNSPECIFIED)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // 7. GUARDA ANTI-DRIFT
    // =========================================================================

    @Nested
    @DisplayName("7. Guarda anti-drift — alinhamento com helpers de FoodRole")
    class AntiDriftTests {

        @Test
        @DisplayName("BREAKFAST allowedRoles() é exatamente o conjunto onde isBreakfastEligible() é true")
        void breakfastAllowedRolesMirrorBreakfastEligibleHelper() {
            // given
            Set<FoodRole> expectedByHelper = Arrays.stream(FoodRole.values())
                    .filter(FoodRole::isBreakfastEligible)
                    .collect(Collectors.toUnmodifiableSet());

            // when
            Set<FoodRole> actualAllowed = MealCompositionRules
                    .forMealType(MealType.BREAKFAST)
                    .allowedRoles();

            // then
            assertThat(actualAllowed).isEqualTo(expectedByHelper);
        }

        @Test
        @DisplayName("MORNING_SNACK allowedRoles() é exatamente o conjunto onde isSnackEligible() é true")
        void morningSnackAllowedRolesMirrorSnackEligibleHelper() {
            // given
            Set<FoodRole> expectedByHelper = Arrays.stream(FoodRole.values())
                    .filter(FoodRole::isSnackEligible)
                    .collect(Collectors.toUnmodifiableSet());

            // when
            Set<FoodRole> actualAllowed = MealCompositionRules
                    .forMealType(MealType.MORNING_SNACK)
                    .allowedRoles();

            // then
            assertThat(actualAllowed).isEqualTo(expectedByHelper);
        }

        @Test
        @DisplayName("AFTERNOON_SNACK allowedRoles() é exatamente o conjunto onde isSnackEligible() é true")
        void afternoonSnackAllowedRolesMirrorSnackEligibleHelper() {
            // given
            Set<FoodRole> expectedByHelper = Arrays.stream(FoodRole.values())
                    .filter(FoodRole::isSnackEligible)
                    .collect(Collectors.toUnmodifiableSet());

            // when
            Set<FoodRole> actualAllowed = MealCompositionRules
                    .forMealType(MealType.AFTERNOON_SNACK)
                    .allowedRoles();

            // then
            assertThat(actualAllowed).isEqualTo(expectedByHelper);
        }
    }
}
