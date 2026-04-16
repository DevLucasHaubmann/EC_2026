package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.NutritionalProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalorieCalculatorTest {

    private CalorieCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalorieCalculator();
    }

    private NutritionalProfile createProfile(NutritionalProfile.Gender gender,
                                              double weightKg, double heightCm,
                                              NutritionalProfile.ActivityLevel level) {
        NutritionalProfile p = new NutritionalProfile();
        p.setGender(gender);
        p.setWeightKg(weightKg);
        p.setHeightCm(heightCm);
        p.setActivityLevel(level);
        return p;
    }

    private Assessment createAssessment(Assessment.NutritionalGoal goal) {
        Assessment a = new Assessment();
        a.setGoal(goal);
        return a;
    }

    @Nested
    @DisplayName("TMB - Harris-Benedict Revisada")
    class Tmb {

        @Test
        @DisplayName("Calcula TMB masculino corretamente")
        void tmbMasculino() {
            NutritionalProfile profile = createProfile(
                    NutritionalProfile.Gender.MALE, 80, 175,
                    NutritionalProfile.ActivityLevel.MODERATE);

            double bmr = calculator.calculateBmr(profile, 30);

            // 88.362 + (13.397 * 80) + (4.799 * 175) - (5.677 * 30) = 1829.637
            assertThat(bmr).isCloseTo(1829.637, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("Calcula TMB feminino corretamente")
        void tmbFeminino() {
            NutritionalProfile profile = createProfile(
                    NutritionalProfile.Gender.FEMALE, 60, 165,
                    NutritionalProfile.ActivityLevel.LIGHT);

            double bmr = calculator.calculateBmr(profile, 25);

            // 447.593 + (9.247 * 60) + (3.098 * 165) - (4.330 * 25) = 1405.333
            assertThat(bmr).isCloseTo(1405.333, org.assertj.core.data.Offset.offset(0.1));
        }
    }

    @Nested
    @DisplayName("Meta calórica com objetivo")
    class MetaCalorica {

        @Test
        @DisplayName("Perda de peso reduz TDEE em 20%")
        void perdaDePeso() {
            NutritionalProfile profile = createProfile(
                    NutritionalProfile.Gender.MALE, 80, 175,
                    NutritionalProfile.ActivityLevel.SEDENTARY);
            Assessment assessment = createAssessment(Assessment.NutritionalGoal.WEIGHT_LOSS);

            double target = calculator.calculateDailyCalorieTarget(profile, assessment, 30);

            // TMB=1829.637, TDEE=1829.637*1.2=2195.564, meta=2195.564*0.8=1756.452
            assertThat(target).isCloseTo(1756.452, org.assertj.core.data.Offset.offset(1.0));
        }

        @Test
        @DisplayName("Ganho de massa aumenta TDEE em 15%")
        void ganhoDeMassa() {
            NutritionalProfile profile = createProfile(
                    NutritionalProfile.Gender.MALE, 80, 175,
                    NutritionalProfile.ActivityLevel.SEDENTARY);
            Assessment assessment = createAssessment(Assessment.NutritionalGoal.MUSCLE_GAIN);

            double target = calculator.calculateDailyCalorieTarget(profile, assessment, 30);

            // TDEE=2195.564, meta=2195.564*1.15=2524.899
            assertThat(target).isCloseTo(2524.899, org.assertj.core.data.Offset.offset(1.0));
        }

        @Test
        @DisplayName("Manutenção mantém TDEE inalterado")
        void manutencao() {
            NutritionalProfile profile = createProfile(
                    NutritionalProfile.Gender.FEMALE, 60, 165,
                    NutritionalProfile.ActivityLevel.MODERATE);
            Assessment assessment = createAssessment(Assessment.NutritionalGoal.MAINTENANCE);

            double target = calculator.calculateDailyCalorieTarget(profile, assessment, 25);

            // TMB=1405.333, TDEE=1405.333*1.55=2178.266
            assertThat(target).isCloseTo(2178.266, org.assertj.core.data.Offset.offset(1.0));
        }
    }
}
