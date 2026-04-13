package com.tukan.api.service.mealplan;

import com.tukan.api.dto.ai.AiPerfilContext;
import com.tukan.api.dto.ai.AiTriagemContext;
import com.tukan.api.dto.mealplan.MealPlanContext;
import com.tukan.api.dto.mealplan.MealPlanContext.EligibleFoodSummary;
import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.entity.NutritionalProfile;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.exception.IncompleteProfileException;
import com.tukan.api.repository.AssessmentRepository;
import com.tukan.api.repository.NutritionalProfileRepository;
import com.tukan.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orquestra a geração do plano alimentar diário.
 *
 * Responsabilidades:
 * 1. Carregar perfil e triagem do usuário
 * 2. Calcular meta calórica via CalorieCalculator
 * 3. Distribuir kcal entre refeições via MealDistributor
 * 4. Filtrar alimentos elegíveis via FoodFilterService
 * 5. Agrupar alimentos por refeição
 * 6. Montar o MealPlanContext pronto para consumo (IA ou direto)
 */
@Service
@RequiredArgsConstructor
public class MealPlanEngine {

    private final UserService userService;
    private final NutritionalProfileRepository profileRepository;
    private final AssessmentRepository assessmentRepository;
    private final CalorieCalculator calorieCalculator;
    private final FoodFilterService foodFilterService;
    private final MealDistributor mealDistributor;

    private static final int MAX_FOODS_PER_MEAL = 30;

    @Transactional(readOnly = true)
    public MealPlanContext buildContext(String authenticatedEmail) {
        User user = userService.findByEmail(authenticatedEmail);

        NutritionalProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "perfil",
                        "Perfil nutricional não encontrado. Complete seu perfil antes de gerar um plano alimentar."));

        Assessment assessment = assessmentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "triagem",
                        "Triagem não encontrada. Complete sua triagem antes de gerar um plano alimentar."));

        int age = profile.calculateAge(LocalDate.now());
        if (age < 1) {
            throw new BusinessException("Idade inválida para cálculo nutricional.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        double dailyCalories = calorieCalculator.calculateDailyCalorieTarget(profile, assessment, age);
        Map<String, Double> distribution = mealDistributor.distribute(dailyCalories);

        List<Food> eligible = foodFilterService.findEligibleFoods(assessment);
        if (eligible.isEmpty()) {
            throw new BusinessException(
                    "Nenhum alimento compatível encontrado com as restrições informadas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, List<Food>> grouped = foodFilterService.groupByMealType(eligible);
        Map<String, List<EligibleFoodSummary>> foodsByMeal = buildFoodSummaries(grouped, distribution);

        AiPerfilContext profileCtx = new AiPerfilContext(
                profile.getGender().name(),
                age,
                profile.getWeightKg(),
                profile.getHeightCm(),
                profile.getActivityLevel().name()
        );

        AiTriagemContext assessmentCtx = buildTriagemContext(assessment);

        return new MealPlanContext(profileCtx, assessmentCtx, Math.round(dailyCalories), distribution, foodsByMeal);
    }

    private Map<String, List<EligibleFoodSummary>> buildFoodSummaries(
            Map<String, List<Food>> grouped, Map<String, Double> distribution) {

        Map<String, List<EligibleFoodSummary>> result = new LinkedHashMap<>();

        for (var entry : grouped.entrySet()) {
            String mealType = entry.getKey();
            List<Food> foods = entry.getValue();

            List<EligibleFoodSummary> summaries = foods.stream()
                    .sorted(Comparator.comparing(
                            (Food f) -> f.getCategory() == null ? "" : f.getCategory()))
                    .limit(MAX_FOODS_PER_MEAL)
                    .map(this::toSummary)
                    .collect(Collectors.toList());

            result.put(mealType, summaries);
        }

        return result;
    }

    private EligibleFoodSummary toSummary(Food food) {
        return new EligibleFoodSummary(
                food.getId(),
                food.getName(),
                food.getCategory(),
                toDouble(food.getCaloriesPer100g()),
                toDouble(food.getProteinPer100g()),
                toDouble(food.getCarbsPer100g()),
                toDouble(food.getFatPer100g()),
                toDouble(food.getReferencePortionGrams())
        );
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private AiTriagemContext buildTriagemContext(Assessment assessment) {
        return new AiTriagemContext(
                assessment.getGoal().name(),
                normalizeList(assessment.getDietaryRestrictions()),
                normalizeList(assessment.getAllergies()),
                normalizeList(assessment.getHealthConditions())
        );
    }

    private List<String> normalizeList(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.stream(value.split("[,;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
