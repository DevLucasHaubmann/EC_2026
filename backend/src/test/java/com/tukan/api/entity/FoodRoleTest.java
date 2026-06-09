package com.tukan.api.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import static org.assertj.core.api.Assertions.assertThat;

class FoodRoleTest {

    @Test
    @DisplayName("Enum exposes exactly the 2.4B taxonomy values")
    void enumHasExpectedValues() {
        // given / when / then
        assertThat(FoodRole.values()).containsExactly(
                FoodRole.MAIN_PROTEIN,
                FoodRole.MAIN_CARBOHYDRATE,
                FoodRole.VEGETABLE_LEGUME,
                FoodRole.FRUIT,
                FoodRole.BREAKFAST_LIGHT,
                FoodRole.HEALTHY_SNACK,
                FoodRole.LIGHT_DAIRY,
                FoodRole.HEALTHY_FAT,
                FoodRole.COMPLEMENT,
                FoodRole.UNSPECIFIED);
    }

    @Test
    @DisplayName("Lunch/dinner pillars are the three main-meal components")
    void mainMealComponentsAreProteinCarbAndVegetable() {
        // given / when / then
        assertThat(FoodRole.MAIN_PROTEIN.isMainMealComponent()).isTrue();
        assertThat(FoodRole.MAIN_CARBOHYDRATE.isMainMealComponent()).isTrue();
        assertThat(FoodRole.VEGETABLE_LEGUME.isMainMealComponent()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FoodRole.class,
            names = {"FRUIT", "BREAKFAST_LIGHT", "HEALTHY_SNACK", "LIGHT_DAIRY",
                    "HEALTHY_FAT", "COMPLEMENT", "UNSPECIFIED"})
    @DisplayName("Non-pillar roles are not main-meal components")
    void nonPillarRolesAreNotMainMealComponents(FoodRole role) {
        // given / when / then
        assertThat(role.isMainMealComponent()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = FoodRole.class, names = {"HEALTHY_SNACK", "FRUIT"})
    @DisplayName("Snack is eligible only for HEALTHY_SNACK or FRUIT")
    void snackEligibleRoles(FoodRole role) {
        // given / when / then
        assertThat(role.isSnackEligible()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FoodRole.class, mode = Mode.EXCLUDE, names = {"HEALTHY_SNACK", "FRUIT"})
    @DisplayName("Every other role is not snack-eligible")
    void nonSnackRolesAreNotEligible(FoodRole role) {
        // given / when / then
        assertThat(role.isSnackEligible()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = FoodRole.class,
            names = {"BREAKFAST_LIGHT", "FRUIT", "LIGHT_DAIRY", "HEALTHY_SNACK"})
    @DisplayName("Breakfast accepts light items: breakfast staple, fruit, light dairy and healthy snack")
    void breakfastEligibleRoles(FoodRole role) {
        // given / when / then
        assertThat(role.isBreakfastEligible()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FoodRole.class, mode = Mode.EXCLUDE,
            names = {"BREAKFAST_LIGHT", "FRUIT", "LIGHT_DAIRY", "HEALTHY_SNACK"})
    @DisplayName("Every other role is not breakfast-eligible")
    void nonBreakfastRolesAreNotEligible(FoodRole role) {
        // given / when / then
        assertThat(role.isBreakfastEligible()).isFalse();
    }

    @Test
    @DisplayName("orUnspecified maps null to UNSPECIFIED and keeps a concrete role intact")
    void orUnspecifiedResolvesNull() {
        // given / when / then
        assertThat(FoodRole.orUnspecified(null)).isEqualTo(FoodRole.UNSPECIFIED);
        assertThat(FoodRole.orUnspecified(FoodRole.MAIN_PROTEIN)).isEqualTo(FoodRole.MAIN_PROTEIN);
    }
}
