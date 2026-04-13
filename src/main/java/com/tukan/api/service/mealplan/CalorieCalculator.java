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
            case MASCULINO -> 88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age);
            case FEMININO -> 447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age);
        };
    }

    private double getActivityMultiplier(NutritionalProfile.ActivityLevel level) {
        return switch (level) {
            case SEDENTARIO -> 1.2;
            case LEVE -> 1.375;
            case MODERADO -> 1.55;
            case INTENSO -> 1.725;
            case MUITO_INTENSO -> 1.9;
        };
    }

    private double applyGoalAdjustment(double tdee, Assessment.NutritionalGoal goal) {
        return switch (goal) {
            case PERDA_DE_PESO -> tdee * 0.80;
            case GANHO_DE_MASSA -> tdee * 1.15;
            case MANUTENCAO, REEDUCACAO_ALIMENTAR -> tdee;
            case PERFORMANCE_ESPORTIVA -> tdee * 1.10;
        };
    }
}
