package com.tukan.api.service.mealplan;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Distribui a meta calórica diária entre as 4 refeições
 * usando proporções fixas baseadas em referências nutricionais.
 */
@Component
public class MealDistributor {

    private static final Map<String, Double> MEAL_PROPORTIONS = Map.of(
            "CAFE_MANHA", 0.25,
            "ALMOCO", 0.35,
            "LANCHE_TARDE", 0.15,
            "JANTA", 0.25
    );

    public Map<String, Double> distribute(double dailyCalories) {
        Map<String, Double> distribution = new LinkedHashMap<>();
        for (var entry : MEAL_PROPORTIONS.entrySet()) {
            distribution.put(entry.getKey(), Math.round(dailyCalories * entry.getValue() * 10.0) / 10.0);
        }
        return distribution;
    }

    public static Map<String, Double> getProportions() {
        return MEAL_PROPORTIONS;
    }
}
