package com.tukan.api.service;

import com.tukan.api.entity.Assessment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentCompletenessPolicyTest {

    private final AssessmentCompletenessPolicy policy = new AssessmentCompletenessPolicy();

    @Test
    void shouldReturnFalseForNullAssessment() {
        // given / when / then
        assertThat(policy.isComplete(null)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenAllRequiredFieldsArePresent() {
        // given
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.WEIGHT_LOSS);
        assessment.setDietType(Assessment.DietType.ONIVORA);
        assessment.setMealsPerDay(4);

        // when / then
        assertThat(policy.isComplete(assessment)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenGoalIsNull() {
        // given
        Assessment assessment = new Assessment();
        assessment.setDietType(Assessment.DietType.ONIVORA);
        assessment.setMealsPerDay(3);

        // when / then
        assertThat(policy.isComplete(assessment)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDietTypeIsNull() {
        // given
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MAINTENANCE);
        assessment.setMealsPerDay(3);

        // when / then
        assertThat(policy.isComplete(assessment)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenMealsPerDayIsNull() {
        // given
        Assessment assessment = new Assessment();
        assessment.setGoal(Assessment.NutritionalGoal.MUSCLE_GAIN);
        assessment.setDietType(Assessment.DietType.VEGANA);

        // when / then
        assertThat(policy.isComplete(assessment)).isFalse();
    }
}
