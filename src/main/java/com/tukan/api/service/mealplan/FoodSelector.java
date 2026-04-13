package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Food;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Seleciona alimentos para compor opções de refeição.
 * Garante variedade entre categorias, respeita meta calórica (±10%)
 * e evita repetição de alimentos entre opções da mesma refeição.
 */
@Component
public class FoodSelector {

    private static final double CALORIE_TOLERANCE = 0.10;
    private static final int MAX_ITEMS_PER_OPTION = 5;

    // Categorias estruturais que definem a base de uma refeição
    private static final List<String> CORE_CATEGORIES = List.of(
            "PROTEINA", "CARBOIDRATO", "LEGUME_VERDURA", "FRUTA",
            "LEGUMINOSA", "LATICINIO", "GORDURA_BOA");

    private final Random random = new Random();

    /**
     * Monta 2 opções de refeição a partir dos alimentos disponíveis,
     * cada uma respeitando a meta calórica com margem de ±10%.
     */
    public List<MealOptionBuild> buildTwoOptions(List<Food> availableFoods, double calorieTarget) {
        if (availableFoods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Lista de alimentos não pode estar vazia para montar opções de refeição.");
        }

        List<Food> shuffled = new ArrayList<>(availableFoods);
        Collections.shuffle(shuffled, random);

        // Priorizar alimentos com tipo_refeicao_principal correspondente
        shuffled.sort(Comparator.comparing(
                (Food f) -> f.getPrimaryMealType() != null ? 0 : 1));

        Map<String, List<Food>> byCategory = shuffled.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCategory() != null ? f.getCategory() : "OUTROS",
                        LinkedHashMap::new,
                        Collectors.toList()));

        Set<Integer> usedInOption1 = new HashSet<>();
        List<Food> option1Items = selectItems(byCategory, calorieTarget, usedInOption1);

        // Tentativa normal: sem repetir alimentos da opção 1
        Set<Integer> excludedForOption2 = new HashSet<>(usedInOption1);
        List<Food> option2Items = selectItems(byCategory, calorieTarget, excludedForOption2);

        // Fallback: se opção 2 ficou vazia, relaxar restrição de repetição
        if (option2Items.isEmpty() && !option1Items.isEmpty()) {
            // Reshuffle para variar a ordem de seleção
            List<Food> reshuffled = new ArrayList<>(availableFoods);
            Collections.shuffle(reshuffled, random);
            reshuffled.sort(Comparator.comparing(
                    (Food f) -> f.getPrimaryMealType() != null ? 0 : 1));

            Map<String, List<Food>> reshuffledByCategory = reshuffled.stream()
                    .collect(Collectors.groupingBy(
                            f -> f.getCategory() != null ? f.getCategory() : "OUTROS",
                            LinkedHashMap::new,
                            Collectors.toList()));

            option2Items = selectItems(reshuffledByCategory, calorieTarget, new HashSet<>());
        }

        return List.of(
                buildOption(1, option1Items),
                buildOption(2, option2Items)
        );
    }

    private List<Food> selectItems(Map<String, List<Food>> byCategory,
                                    double calorieTarget, Set<Integer> excluded) {
        List<Food> selected = new ArrayList<>();
        double accumulated = 0.0;
        double maxCalories = calorieTarget * (1 + CALORIE_TOLERANCE);

        // Primeiro: tentar pegar 1 alimento de cada categoria core
        for (String category : CORE_CATEGORIES) {
            if (selected.size() >= MAX_ITEMS_PER_OPTION) break;
            if (accumulated >= maxCalories) break;

            List<Food> candidates = byCategory.getOrDefault(category, List.of());
            for (Food food : candidates) {
                if (excluded.contains(food.getId())) continue;

                double foodCalories = caloriesForPortion(food);
                if (accumulated + foodCalories <= maxCalories) {
                    selected.add(food);
                    excluded.add(food.getId());
                    accumulated += foodCalories;
                    break;
                }
            }
        }

        // Segundo: preencher até atingir meta mínima ou MAX_ITEMS
        double minCalories = calorieTarget * (1 - CALORIE_TOLERANCE);
        if (accumulated < minCalories) {
            for (var entry : byCategory.entrySet()) {
                if (selected.size() >= MAX_ITEMS_PER_OPTION) break;
                if (accumulated >= minCalories) break;

                for (Food food : entry.getValue()) {
                    if (excluded.contains(food.getId())) continue;
                    if (selected.size() >= MAX_ITEMS_PER_OPTION) break;

                    double foodCalories = caloriesForPortion(food);
                    if (accumulated + foodCalories <= maxCalories) {
                        selected.add(food);
                        excluded.add(food.getId());
                        accumulated += foodCalories;
                        if (accumulated >= minCalories) break;
                    }
                }
            }
        }

        return selected;
    }

    private double caloriesForPortion(Food food) {
        BigDecimal portion = food.getReferencePortionGrams();
        BigDecimal cal100g = food.getCaloriesPer100g();
        if (portion == null || cal100g == null) return 0.0;
        return cal100g.multiply(portion).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private MealOptionBuild buildOption(int number, List<Food> items) {
        double totalCal = 0, totalProt = 0, totalCarbs = 0, totalFat = 0;

        List<FoodPortionBuild> portions = new ArrayList<>();
        for (Food food : items) {
            BigDecimal portion = food.getReferencePortionGrams() != null
                    ? food.getReferencePortionGrams() : BigDecimal.valueOf(100);
            double factor = portion.doubleValue() / 100.0;

            double cal = toDouble(food.getCaloriesPer100g()) * factor;
            double prot = toDouble(food.getProteinPer100g()) * factor;
            double carbs = toDouble(food.getCarbsPer100g()) * factor;
            double fat = toDouble(food.getFatPer100g()) * factor;
            double fiber = toDouble(food.getFiberPer100g()) * factor;

            totalCal += cal;
            totalProt += prot;
            totalCarbs += carbs;
            totalFat += fat;

            portions.add(new FoodPortionBuild(
                    food.getId(), food.getName(), food.getCategory(),
                    portion, round(cal), round(prot), round(carbs), round(fat), round(fiber)));
        }

        return new MealOptionBuild(number, portions,
                round(totalCal), round(totalProt), round(totalCarbs), round(totalFat));
    }

    private MealOptionBuild emptyOption(int number) {
        return new MealOptionBuild(number, List.of(), 0, 0, 0, 0);
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // Estruturas internas para passagem de dados ao Engine
    public record MealOptionBuild(
            int optionNumber,
            List<FoodPortionBuild> items,
            double totalCalories,
            double totalProtein,
            double totalCarbs,
            double totalFat
    ) {}

    public record FoodPortionBuild(
            Integer foodId,
            String name,
            String category,
            BigDecimal portionGrams,
            double calories,
            double protein,
            double carbs,
            double fat,
            double fiber
    ) {}
}
