package com.tukan.api.service.mealplan;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Distribui a meta calórica diária entre as refeições (3, 4 ou 5)
 * usando proporções fixas em ordem natural brasileira.
 */
@Component
public class MealDistributor {

    // Ordem natural: café → (lanche manhã) → almoço → lanche tarde → jantar
    private static final Map<String, Double> PROPORTIONS_3 = new LinkedHashMap<>();
    private static final Map<String, Double> PROPORTIONS_4 = new LinkedHashMap<>();
    private static final Map<String, Double> PROPORTIONS_5 = new LinkedHashMap<>();

    static {
        PROPORTIONS_3.put("BREAKFAST",       0.25);
        PROPORTIONS_3.put("LUNCH",           0.40);
        PROPORTIONS_3.put("DINNER",          0.35);

        PROPORTIONS_4.put("BREAKFAST",       0.20);
        PROPORTIONS_4.put("LUNCH",           0.35);
        PROPORTIONS_4.put("AFTERNOON_SNACK", 0.15);
        PROPORTIONS_4.put("DINNER",          0.30);

        PROPORTIONS_5.put("BREAKFAST",       0.20);
        PROPORTIONS_5.put("MORNING_SNACK",   0.10);
        PROPORTIONS_5.put("LUNCH",           0.35);
        PROPORTIONS_5.put("AFTERNOON_SNACK", 0.10);
        PROPORTIONS_5.put("DINNER",          0.25);
    }

    public Map<String, Double> distribute(double dailyCalories, int mealsPerDay) {
        Map<String, Double> proportions = switch (mealsPerDay) {
            case 3 -> PROPORTIONS_3;
            case 5 -> PROPORTIONS_5;
            default -> PROPORTIONS_4; // 4 é o padrão seguro
        };

        Map<String, Double> distribution = new LinkedHashMap<>();
        for (var entry : proportions.entrySet()) {
            distribution.put(entry.getKey(), Math.round(dailyCalories * entry.getValue() * 10.0) / 10.0);
        }
        return distribution;
    }
}
