package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.NutritionalProfile;
import org.springframework.stereotype.Component;

/**
 * Calcula TMB (Harris-Benedict revisada) e meta calórica diária
 * com base no perfil e objetivo do usuário.
 */
@Component
public class CalorieCalculator {

    public double calculateDailyCalorieTarget(NutritionalProfile profile, Assessment assessment, int age) {
        double bmr = calculateBmr(profile, age);
        double tdee = bmr * getActivityMultiplier(profile.getActivityLevel());
        return applyGoalAdjustment(tdee, assessment.getGoal());
    }

    double calculateBmr(NutritionalProfile profile, int age) {
        double weight = profile.getWeightKg();
        double height = profile.getHeightCm();

        return switch (profile.getGender()) {
            case MALE -> 88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age);
            case FEMALE -> 447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age);
        };
    }

    private double getActivityMultiplier(NutritionalProfile.ActivityLevel level) {
        return switch (level) {
            case SEDENTARY -> 1.2;
            case LIGHT -> 1.375;
            case MODERATE -> 1.55;
            case INTENSE -> 1.725;
            case VERY_INTENSE -> 1.9;
        };
    }

    private double applyGoalAdjustment(double tdee, Assessment.NutritionalGoal goal) {
        return switch (goal) {
            case WEIGHT_LOSS -> tdee * 0.80;
            case MUSCLE_GAIN -> tdee * 1.15;
            case MAINTENANCE, DIETARY_REEDUCATION -> tdee;
            case SPORTS_PERFORMANCE -> tdee * 1.10;
        };
    }
}
