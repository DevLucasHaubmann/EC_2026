package com.tukan.api.service.mealplan;

import com.tukan.api.entity.Food;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Curadoria de qualidade dos alimentos.
 * Remove itens tecnicamente existentes na base mas inadequados para
 * recomendação alimentar real (aditivos, subprodutos, ingredientes
 * isolados, gorduras puras, itens industriais estranhos).
 */
@Service
public class FoodCurationService {

    /**
     * Termos que, se presentes no nome ou subcategoria do alimento,
     * indicam que o item não é adequado para recomendação direta.
     */
    private static final Set<String> BLOCKED_TERMS = Set.of(
            // Aditivos e agentes culinários
            "baking soda", "baking powder", "leavening", "fermento quimico",
            "bicarbonato", "cornstarch", "amido de milho isolado",

            // Gorduras puras e industriais
            "lard", "banha", "shortening", "margarine-like",
            "spread fat-free", "spread fat free", "filling fat",
            "gordura vegetal", "gordura hidrogenada", "hydrogenated",

            // Subprodutos e vísceras incomuns
            "by-product", "by product", "byproduct",
            "variety meat", "variety meats", "organ meat",
            "gizzard", "moela", "tripe", "tripa", "dobradinha",
            "tongue", "lingua de boi", "brain", "miolo", "mioleira",
            "kidney", "rim de boi", "spleen", "baco",
            "heart", "coracao de boi", "coracao de frango",
            "liver", "figado",

            // Itens industriais e técnicos
            "industrial", "concentrate", "isolate", "powder mix",
            "protein powder", "whey isolate", "casein powder",
            "bologna", "bolonha",

            // Termos de risco genéricos
            "additive", "aditivo", "flavoring", "aromatizante",
            "coloring", "corante", "preservative", "conservante",
            "thickener", "espessante", "emulsifier", "emulsificante",
            "stabilizer", "estabilizante",

            // Itens impróprios como refeição
            "fish oil capsule", "oleo de peixe capsula",
            "supplement", "suplemento",
            "vinegar", "vinagre",
            "soy sauce", "molho de soja", "shoyu",
            "mustard", "mostarda",
            "ketchup", "catchup",
            "hot sauce", "molho picante",
            "worcestershire"
    );

    /**
     * Categorias inteiras que devem ser excluídas da seleção de alimentos.
     */
    private static final Set<String> BLOCKED_CATEGORIES = Set.of(
            "ADITIVO", "CONDIMENTO", "TEMPERO", "MOLHO"
    );

    /**
     * Filtra a lista de alimentos, removendo itens inadequados para
     * recomendação alimentar real.
     */
    public List<Food> curate(List<Food> foods) {
        return foods.stream()
                .filter(this::isAcceptable)
                .collect(Collectors.toList());
    }

    boolean isAcceptable(Food food) {
        if (isBlockedCategory(food)) return false;
        if (containsBlockedTerm(food)) return false;
        return true;
    }

    private boolean isBlockedCategory(Food food) {
        String category = food.getCategory();
        if (category == null) return false;
        return BLOCKED_CATEGORIES.contains(category.toUpperCase().trim());
    }

    private boolean containsBlockedTerm(Food food) {
        String name = normalize(food.getName());
        String subcategory = normalize(food.getSubcategory());

        for (String term : BLOCKED_TERMS) {
            String normalizedTerm = normalize(term);
            if (name.contains(normalizedTerm)) return true;
            if (subcategory.contains(normalizedTerm)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value.toLowerCase().trim(),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}