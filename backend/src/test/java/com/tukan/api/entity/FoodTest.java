package com.tukan.api.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodTest {

    @Test
    @DisplayName("getEffectiveRole returns UNSPECIFIED when foodRole is not yet classified (null)")
    void effectiveRoleDefaultsToUnspecifiedWhenNull() {
        // given
        Food food = new Food();

        // when / then
        assertThat(food.getFoodRole()).isNull();
        assertThat(food.getEffectiveRole()).isEqualTo(FoodRole.UNSPECIFIED);
    }

    @Test
    @DisplayName("getEffectiveRole returns the concrete role when one is set")
    void effectiveRoleReturnsConcreteRole() {
        // given
        Food food = new Food();
        food.setFoodRole(FoodRole.MAIN_PROTEIN);

        // when / then
        assertThat(food.getEffectiveRole()).isEqualTo(FoodRole.MAIN_PROTEIN);
    }
}
