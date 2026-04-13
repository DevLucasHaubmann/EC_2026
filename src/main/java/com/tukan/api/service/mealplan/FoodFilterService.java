package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Assessment;
import com.tukan.api.entity.Food;
import com.tukan.api.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private static final Map<String, String> ALLERGY_TO_FLAG = Map.of(
            "lactose", "containsLactose",
            "leite", "containsLactose",
            "gluten", "containsGluten",
            "trigo", "containsGluten",
            "ovo", "containsEgg",
            "ovos", "containsEgg"
    );

    private static final Set<String> VEGETARIAN_KEYWORDS = Set.of("vegetariano", "vegetariana", "vegetarianismo");
    private static final Set<String> VEGAN_KEYWORDS = Set.of("vegano", "vegana", "veganismo");

    public List<Food> findEligibleFoods(Assessment assessment) {
        List<Food> allActive = foodRepository.findByActiveTrue();

        Set<String> allergies = normalizeSet(assessment.getAllergies());
        Set<String> restrictions = normalizeSet(assessment.getDietaryRestrictions());
        Set<String> allFilters = new HashSet<>();
        allFilters.addAll(allergies);
        allFilters.addAll(restrictions);

        boolean requireVegetarian = allFilters.stream().anyMatch(VEGETARIAN_KEYWORDS::contains);
        boolean requireVegan = allFilters.stream().anyMatch(VEGAN_KEYWORDS::contains);
        Set<String> allergyFlags = resolveAllergyFlags(allFilters);

        return allActive.stream()
                .filter(food -> passesAllergyFilter(food, allergyFlags))
                .filter(food -> passesDietFilter(food, requireVegetarian, requireVegan))
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

    private Set<String> normalizeSet(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split("[,;]+"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
