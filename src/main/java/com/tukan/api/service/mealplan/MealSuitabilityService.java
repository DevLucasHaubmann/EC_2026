package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Food;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Regras de adequação semântica por tipo de refeição.
 * Evita absurdos como ostras fritas no café da manhã ou
 * peixe empanado no lanche da tarde.
 *
 * Funciona em duas frentes:
 * 1. Blacklist: bloqueia itens absurdos por refeição
 * 2. Priorização: itens com keywords preferidos ficam no topo
 */
@Service
public class MealSuitabilityService {

    // === BLACKLIST POR REFEIÇÃO ===
    // Termos que tornam o alimento INACEITÁVEL para aquela refeição

    private static final Set<String> CAFE_BLACKLIST = Set.of(
            "ostra", "oyster", "camarao empanado", "fried shrimp",
            "peixe frito", "fried fish", "peixe empanado", "breaded fish",
            "feijoada", "dobradinha", "rabada", "mocoto",
            "lasanha", "pizza", "hamburger", "hamburguer",
            "carne de porco", "pork chop", "costela", "rib",
            "bacon", "linguica", "sausage", "salsicha",
            "steak", "bife", "churrasco", "barbecue"
    );

    private static final Set<String> LANCHE_BLACKLIST = Set.of(
            "ostra", "oyster", "feijoada", "dobradinha", "rabada",
            "lasanha", "pizza", "peixe frito", "fried fish",
            "peixe empanado", "breaded fish", "camarao empanado",
            "mocoto", "costela", "rib", "steak",
            "churrasco", "barbecue", "carne de porco", "pork chop"
    );

    // Almoço e janta são mais permissivos — bloqueiam apenas absurdos
    private static final Set<String> ALMOCO_BLACKLIST = Set.of(
            "cereal matinal", "breakfast cereal", "granola bar",
            "barra de cereal"
    );

    private static final Set<String> JANTA_BLACKLIST = Set.of(
            "cereal matinal", "breakfast cereal"
    );

    // === KEYWORDS DE PRIORIZAÇÃO POR REFEIÇÃO ===
    // Alimentos que contêm esses termos são priorizados (pontuação extra)

    private static final Set<String> CAFE_PREFERRED = Set.of(
            "pao", "bread", "aveia", "oat", "granola",
            "fruta", "fruit", "banana", "maca", "mamao", "morango",
            "ovo", "egg", "iogurte", "yogurt",
            "queijo", "cheese", "tapioca", "crepioca",
            "leite", "milk", "cafe", "coffee",
            "mel", "honey", "geleia", "jam",
            "cereal", "muesli", "torrada", "toast"
    );

    private static final Set<String> LANCHE_PREFERRED = Set.of(
            "fruta", "fruit", "banana", "maca", "morango", "uva",
            "iogurte", "yogurt", "castanha", "nut", "amendoa", "almond",
            "nozes", "walnut", "aveia", "oat", "granola",
            "pao", "bread", "torrada", "toast",
            "queijo", "cheese", "barra", "bar",
            "biscoito integral", "cracker"
    );

    private static final Set<String> ALMOCO_PREFERRED = Set.of(
            "arroz", "rice", "feijao", "bean", "lentilha", "lentil",
            "frango", "chicken", "carne", "beef", "peixe", "fish",
            "legume", "vegetable", "verdura", "salada", "salad",
            "batata", "potato", "mandioca", "cassava",
            "massa", "pasta", "macarrao", "tofu", "tempeh",
            "brocolis", "broccoli", "cenoura", "carrot",
            "abobrinha", "zucchini", "espinafre", "spinach"
    );

    private static final Set<String> JANTA_PREFERRED = Set.of(
            "arroz", "rice", "feijao", "bean", "lentilha", "lentil",
            "frango", "chicken", "carne", "beef", "peixe", "fish",
            "legume", "vegetable", "verdura", "salada", "salad",
            "sopa", "soup", "caldo", "broth",
            "omelete", "omelette", "ovo", "egg",
            "batata", "potato", "massa", "pasta",
            "tofu", "tempeh", "brocolis", "espinafre"
    );

    private static final Map<String, Set<String>> BLACKLISTS = Map.of(
            "CAFE_MANHA", CAFE_BLACKLIST,
            "ALMOCO", ALMOCO_BLACKLIST,
            "LANCHE_TARDE", LANCHE_BLACKLIST,
            "JANTA", JANTA_BLACKLIST
    );

    private static final Map<String, Set<String>> PREFERRED = Map.of(
            "CAFE_MANHA", CAFE_PREFERRED,
            "ALMOCO", ALMOCO_PREFERRED,
            "LANCHE_TARDE", LANCHE_PREFERRED,
            "JANTA", JANTA_PREFERRED
    );

    /**
     * Filtra e reordena alimentos para uma refeição específica.
     * Remove itens da blacklist e prioriza itens com keywords preferidos.
     */
    public List<Food> filterAndPrioritize(List<Food> foods, String mealType) {
        Set<String> blacklist = BLACKLISTS.getOrDefault(mealType, Set.of());
        Set<String> preferred = PREFERRED.getOrDefault(mealType, Set.of());

        return foods.stream()
                .filter(food -> !isBlacklisted(food, blacklist))
                .sorted(Comparator.comparingInt((Food f) -> preferenceScore(f, preferred)).reversed())
                .collect(Collectors.toList());
    }

    boolean isBlacklisted(Food food, Set<String> blacklist) {
        String name = normalize(food.getName());
        for (String term : blacklist) {
            if (name.contains(normalize(term))) return true;
        }
        return false;
    }

    private int preferenceScore(Food food, Set<String> preferred) {
        String name = normalize(food.getName());
        int score = 0;
        for (String term : preferred) {
            if (name.contains(normalize(term))) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value.toLowerCase().trim(),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}