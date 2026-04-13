package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Filtra o universo de alimentos com base nas restrições, alergias
 * e condições de saúde do usuário. Retorna apenas os elegíveis.
 */
@Service
@RequiredArgsConstructor
public class FoodFilterService {

    private final FoodRepository foodRepository;

    private static final Map<String, String> ALLERGY_TO_FLAG;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("lactose", "containsLactose");
        map.put("leite", "containsLactose");
        map.put("intolerancia a lactose", "containsLactose");
        map.put("intolerante a lactose", "containsLactose");
        map.put("alergia a leite", "containsLactose");
        map.put("gluten", "containsGluten");
        map.put("trigo", "containsGluten");
        map.put("doenca celiaca", "containsGluten");
        map.put("celiaco", "containsGluten");
        map.put("celiaca", "containsGluten");
        map.put("ovo", "containsEgg");
        map.put("ovos", "containsEgg");
        map.put("alergia a ovo", "containsEgg");
        ALLERGY_TO_FLAG = Collections.unmodifiableMap(map);
    }

    private static final Set<String> VEGETARIAN_KEYWORDS = Set.of(
            "vegetariano", "vegetariana", "vegetarianismo", "dieta vegetariana");
    private static final Set<String> VEGAN_KEYWORDS = Set.of(
            "vegano", "vegana", "veganismo", "dieta vegana");

    // Categorias a evitar por condição de saúde
    private static final Map<String, Set<String>> HEALTH_CONDITION_BLOCKED_CATEGORIES;
    static {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("diabetes", Set.of("BEBIDA"));
        map.put("diabetico", Set.of("BEBIDA"));
        map.put("diabetica", Set.of("BEBIDA"));
        map.put("diabetes tipo 2", Set.of("BEBIDA"));
        map.put("diabetes tipo 1", Set.of("BEBIDA"));
        HEALTH_CONDITION_BLOCKED_CATEGORIES = Collections.unmodifiableMap(map);
    }

    // Categorias com alto teor de açúcar/carboidrato simples — limitadas para diabéticos
    private static final Set<String> DIABETES_KEYWORDS = Set.of(
            "diabetes", "diabetico", "diabetica", "diabetes tipo 1", "diabetes tipo 2");

    public List<Food> findEligibleFoods(Assessment assessment) {
        List<Food> allActive = foodRepository.findByActiveTrue();

        Set<String> allergies = normalizeSet(assessment.getAllergies());
        Set<String> restrictions = normalizeSet(assessment.getDietaryRestrictions());
        Set<String> healthConditions = normalizeSet(assessment.getHealthConditions());

        Set<String> allFilters = new HashSet<>();
        allFilters.addAll(allergies);
        allFilters.addAll(restrictions);

        boolean requireVegetarian = allFilters.stream().anyMatch(VEGETARIAN_KEYWORDS::contains);
        boolean requireVegan = allFilters.stream().anyMatch(VEGAN_KEYWORDS::contains);
        Set<String> allergyFlags = resolveAllergyFlags(allFilters);
        Set<String> blockedCategories = resolveBlockedCategories(healthConditions);
        boolean hasDiabetes = healthConditions.stream().anyMatch(DIABETES_KEYWORDS::contains);

        return allActive.stream()
                .filter(food -> passesAllergyFilter(food, allergyFlags))
                .filter(food -> passesDietFilter(food, requireVegetarian, requireVegan))
                .filter(food -> passesCategoryFilter(food, blockedCategories))
                .filter(food -> passesDiabetesFilter(food, hasDiabetes))
                .collect(Collectors.toList());
    }

    public Map<String, List<Food>> groupByMealType(List<Food> foods) {
        Map<String, List<Food>> grouped = new LinkedHashMap<>();
        grouped.put("CAFE_MANHA", new ArrayList<>());
        grouped.put("ALMOCO", new ArrayList<>());
        grouped.put("LANCHE_TARDE", new ArrayList<>());
        grouped.put("JANTA", new ArrayList<>());

        for (Food food : foods) {
            String suitable = food.getSuitableMeals();
            if (suitable == null || suitable.isBlank()) {
                String primary = food.getPrimaryMealType();
                if (primary != null && grouped.containsKey(primary)) {
                    grouped.get(primary).add(food);
                }
                continue;
            }
            for (String meal : suitable.split(",")) {
                String key = meal.trim();
                if (grouped.containsKey(key)) {
                    grouped.get(key).add(food);
                }
            }
        }
        return grouped;
    }

    private Set<String> resolveAllergyFlags(Set<String> filters) {
        Set<String> flags = new HashSet<>();
        for (String filter : filters) {
            String flag = ALLERGY_TO_FLAG.get(filter);
            if (flag != null) {
                flags.add(flag);
            }
        }
        return flags;
    }

    private Set<String> resolveBlockedCategories(Set<String> healthConditions) {
        Set<String> blocked = new HashSet<>();
        for (String condition : healthConditions) {
            Set<String> categories = HEALTH_CONDITION_BLOCKED_CATEGORIES.get(condition);
            if (categories != null) {
                blocked.addAll(categories);
            }
        }
        return blocked;
    }

    private boolean passesAllergyFilter(Food food, Set<String> allergyFlags) {
        if (allergyFlags.contains("containsLactose") && food.isContainsLactose()) return false;
        if (allergyFlags.contains("containsGluten") && food.isContainsGluten()) return false;
        if (allergyFlags.contains("containsEgg") && food.isContainsEgg()) return false;
        return true;
    }

    private boolean passesDietFilter(Food food, boolean requireVegetarian, boolean requireVegan) {
        if (requireVegan && !food.isVegan()) return false;
        if (requireVegetarian && !food.isVegetarian()) return false;
        return true;
    }

    private boolean passesCategoryFilter(Food food, Set<String> blockedCategories) {
        if (blockedCategories.isEmpty()) return true;
        return !blockedCategories.contains(food.getCategory());
    }

    private boolean passesDiabetesFilter(Food food, boolean hasDiabetes) {
        if (!hasDiabetes) return true;
        BigDecimal carbs = food.getCarbsPer100g();
        // Alimentos com mais de 60g de carboidrato por 100g são excluídos para diabéticos
        return carbs == null || carbs.doubleValue() <= 60.0;
    }

    Set<String> normalizeSet(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split("[,;]+"))
                .map(String::trim)
                .map(String::toLowerCase)
                .map(this::removeAccents)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String removeAccents(String input) {
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}
