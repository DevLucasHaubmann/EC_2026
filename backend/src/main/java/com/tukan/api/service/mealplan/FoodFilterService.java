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
    private static final Set<String> PESCATARIAN_KEYWORDS = Set.of(
            "pescatariano", "pescatariana", "dieta pescatariana");

    private static final double LOW_CARB_MAX_CARBS_PER_100G = 20.0;

    // CETOGENICA (Task 2.8E): stricter than LOW_CARB — caps carbs harder and demands a fat floor,
    // so the eligible pool is fat-dense and very low in carbohydrate.
    private static final double KETO_MAX_CARBS_PER_100G = 10.0;
    private static final double KETO_MIN_FAT_PER_100G = 15.0;

    // Subcategoria usada para identificar peixes e frutos do mar na tabela food
    private static final String FISH_SEAFOOD_SUBCATEGORY = "PEIXES_E_FRUTOS_DO_MAR";

    // CARNIVORA (Task 2.8F): animal origin is identified by category/subcategory, never by name.
    // Whole meats, poultry, fish/seafood, plus eggs/dairy (LATICINIOS_E_OVOS + DAIRY category).
    // Excluded on purpose: EMBUTIDOS (processed meat) and GORDURAS_E_OLEOS (mixes animal fats with
    // vegetable oils — not separable with the current schema), and every plant-based category.
    private static final String PROTEIN_CATEGORY = "PROTEIN";
    private static final String DAIRY_CATEGORY = "DAIRY";
    private static final Set<String> CARNIVORA_ANIMAL_SUBCATEGORIES = Set.of(
            "CARNES_BOVINAS", "CARNES_SUINAS", "AVES", "OUTRAS_CARNES",
            FISH_SEAFOOD_SUBCATEGORY, "LATICINIOS_E_OVOS");

    // Categorias a evitar por condição de saúde
    private static final Map<String, Set<String>> HEALTH_CONDITION_BLOCKED_CATEGORIES;
    static {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("diabetes", Set.of("BEVERAGE"));
        map.put("diabetico", Set.of("BEVERAGE"));
        map.put("diabetica", Set.of("BEVERAGE"));
        map.put("diabetes tipo 2", Set.of("BEVERAGE"));
        map.put("diabetes tipo 1", Set.of("BEVERAGE"));
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

        boolean requireVegetarian;
        boolean requireVegan;
        boolean requirePescatarian;
        boolean requireLowCarb;
        boolean requireKeto;
        boolean requireCarnivora;

        Assessment.DietType dietType = assessment.getDietType();
        if (dietType != null) {
            // FLEXITARIANA is intentionally absent from every require* flag: it is eligibility-equivalent
            // to ONIVORA (no diet exclusion). Plant preference is applied later as a selection ordering,
            // not as a hard filter, so meats remain in the pool as fallback.
            requireVegan = dietType == Assessment.DietType.VEGANA;
            requireVegetarian = dietType == Assessment.DietType.VEGETARIANA || requireVegan;
            requirePescatarian = dietType == Assessment.DietType.PESCATARIANA;
            requireLowCarb = dietType == Assessment.DietType.LOW_CARB;
            requireKeto = dietType == Assessment.DietType.CETOGENICA;
            requireCarnivora = dietType == Assessment.DietType.CARNIVORA;
        } else {
            Set<String> combined = new HashSet<>();
            combined.addAll(allergies);
            combined.addAll(restrictions);
            requireVegetarian = combined.stream().anyMatch(VEGETARIAN_KEYWORDS::contains);
            requireVegan = combined.stream().anyMatch(VEGAN_KEYWORDS::contains);
            requirePescatarian = combined.stream().anyMatch(PESCATARIAN_KEYWORDS::contains);
            requireLowCarb = false;
            requireKeto = false;
            requireCarnivora = false;
        }

        Set<String> allFilters = new HashSet<>();
        allFilters.addAll(allergies);
        allFilters.addAll(restrictions);
        Set<String> allergyFlags = resolveAllergyFlags(allFilters);
        Set<String> blockedCategories = resolveBlockedCategories(healthConditions);
        boolean hasDiabetes = healthConditions.stream().anyMatch(DIABETES_KEYWORDS::contains);

        return allActive.stream()
                .filter(food -> passesAllergyFilter(food, allergyFlags))
                .filter(food -> passesDietFilter(food, requireVegetarian, requireVegan))
                .filter(food -> passesPescatarianFilter(food, requirePescatarian))
                .filter(food -> passesCategoryFilter(food, blockedCategories))
                .filter(food -> passesDiabetesFilter(food, hasDiabetes))
                .filter(food -> !requireLowCarb || passesLowCarbFilter(food))
                .filter(food -> !requireKeto || passesKetoFilter(food))
                .filter(food -> !requireCarnivora || passesCarnivoraFilter(food))
                .collect(Collectors.toList());
    }

    public Map<String, List<Food>> groupByMealType(List<Food> foods) {
        Map<String, List<Food>> grouped = new LinkedHashMap<>();
        grouped.put("BREAKFAST", new ArrayList<>());
        grouped.put("MORNING_SNACK", new ArrayList<>());
        grouped.put("LUNCH", new ArrayList<>());
        grouped.put("AFTERNOON_SNACK", new ArrayList<>());
        grouped.put("DINNER", new ArrayList<>());

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

    private boolean passesPescatarianFilter(Food food, boolean requirePescatarian) {
        if (!requirePescatarian) return true;
        if (food.isVegetarian()) return true;
        return FISH_SEAFOOD_SUBCATEGORY.equalsIgnoreCase(food.getSubcategory());
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

    private boolean passesLowCarbFilter(Food food) {
        BigDecimal carbs = food.getCarbsPer100g();
        // Sem dado de carboidrato não é possível garantir que o alimento é low carb
        if (carbs == null) return false;
        return carbs.doubleValue() <= LOW_CARB_MAX_CARBS_PER_100G;
    }

    /**
     * Animal-origin gate for CARNIVORA: allows only foods whose category/subcategory marks them
     * as animal (whole meats, poultry, fish/seafood, eggs and dairy). Everything else — vegetables,
     * fruits, grains, legumes, beverages, seasonings, nuts/seeds, processed meats (EMBUTIDOS) and
     * the mixed-origin GORDURAS_E_OLEOS subcategory — is rejected. Origin is never inferred from the
     * food name, which is unreliable due to the partially-translated dataset.
     */
    private boolean passesCarnivoraFilter(Food food) {
        String category = food.getCategory();
        if (DAIRY_CATEGORY.equalsIgnoreCase(category)) {
            return true;
        }
        if (!PROTEIN_CATEGORY.equalsIgnoreCase(category)) {
            return false;
        }
        String subcategory = food.getSubcategory();
        return subcategory != null
                && CARNIVORA_ANIMAL_SUBCATEGORIES.contains(subcategory.toUpperCase());
    }

    private boolean passesKetoFilter(Food food) {
        BigDecimal carbs = food.getCarbsPer100g();
        BigDecimal fat = food.getFatPer100g();
        // Missing carb or fat data cannot guarantee keto suitability — exclude conservatively.
        if (carbs == null || fat == null) return false;
        return carbs.doubleValue() <= KETO_MAX_CARBS_PER_100G
                && fat.doubleValue() >= KETO_MIN_FAT_PER_100G;
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
