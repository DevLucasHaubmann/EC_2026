package com.tukan.api.service.mealplan;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Gera nomes amigáveis para exibição ao usuário final.
 * Limpa termos técnicos, padroniza capitalização e
 * traduz fragmentos comuns de inglês para português.
 */
@Service
public class FoodDisplayNameService {

    private static final Map<String, String> TRANSLATIONS = Map.ofEntries(
            Map.entry("chicken breast", "Peito de Frango"),
            Map.entry("chicken thigh", "Coxa de Frango"),
            Map.entry("chicken wing", "Asa de Frango"),
            Map.entry("ground beef", "Carne Moída"),
            Map.entry("beef steak", "Bife"),
            Map.entry("pork chop", "Costeleta de Porco"),
            Map.entry("brown rice", "Arroz Integral"),
            Map.entry("white rice", "Arroz Branco"),
            Map.entry("sweet potato", "Batata-Doce"),
            Map.entry("black beans", "Feijão Preto"),
            Map.entry("pinto beans", "Feijão Carioca"),
            Map.entry("kidney beans", "Feijão Vermelho"),
            Map.entry("whole wheat bread", "Pão Integral"),
            Map.entry("white bread", "Pão Branco"),
            Map.entry("rolled oats", "Aveia em Flocos"),
            Map.entry("steel cut oats", "Aveia em Grãos"),
            Map.entry("greek yogurt", "Iogurte Grego"),
            Map.entry("plain yogurt", "Iogurte Natural"),
            Map.entry("cottage cheese", "Queijo Cottage"),
            Map.entry("cream cheese", "Cream Cheese"),
            Map.entry("cheddar cheese", "Queijo Cheddar"),
            Map.entry("mozzarella cheese", "Queijo Mussarela"),
            Map.entry("whole milk", "Leite Integral"),
            Map.entry("skim milk", "Leite Desnatado"),
            Map.entry("olive oil", "Azeite de Oliva"),
            Map.entry("coconut oil", "Óleo de Coco"),
            Map.entry("peanut butter", "Pasta de Amendoim"),
            Map.entry("almond butter", "Pasta de Amêndoa"),
            Map.entry("scrambled eggs", "Ovos Mexidos"),
            Map.entry("boiled egg", "Ovo Cozido"),
            Map.entry("fried egg", "Ovo Frito"),
            Map.entry("hard boiled egg", "Ovo Cozido"),
            Map.entry("salmon fillet", "Filé de Salmão"),
            Map.entry("tuna steak", "Filé de Atum"),
            Map.entry("canned tuna", "Atum em Lata"),
            Map.entry("toasted bread", "Torrada")
    );

    /**
     * Fragmentos em inglês a serem removidos ou substituídos.
     */
    private static final List<CleanupRule> CLEANUP_RULES = List.of(
            // Remover indicações técnicas entre parênteses
            new CleanupRule(Pattern.compile("\\(.*?\\)"), ""),
            // Remover indicações entre colchetes
            new CleanupRule(Pattern.compile("\\[.*?]"), ""),
            // Remover "NFS" (Not Further Specified)
            new CleanupRule(Pattern.compile("(?i)\\bNFS\\b"), ""),
            // Remover "NS as to" e variantes
            new CleanupRule(Pattern.compile("(?i)\\bNS\\s+as\\s+to\\b.*"), ""),
            // Remover ", raw", ", cooked", ", boiled", etc. no final
            new CleanupRule(Pattern.compile("(?i),\\s*(raw|cooked|boiled|baked|roasted|grilled|steamed|fried|dried|fresh|canned|frozen)\\s*$"), ""),
            // Remover vírgulas duplas e espaços extras
            new CleanupRule(Pattern.compile(",\\s*,"), ","),
            new CleanupRule(Pattern.compile("\\s{2,}"), " ")
    );

    /**
     * Gera um nome amigável para exibição ao usuário.
     * Prioriza tradução direta; caso contrário, aplica limpeza.
     */
    public String generateDisplayName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return originalName;
        }

        String trimmed = originalName.trim();

        // Tentar tradução direta (case-insensitive)
        String lower = trimmed.toLowerCase();
        for (var entry : TRANSLATIONS.entrySet()) {
            if (lower.equals(entry.getKey()) || lower.startsWith(entry.getKey() + ",") || lower.startsWith(entry.getKey() + " (")) {
                return entry.getValue();
            }
        }

        // Aplicar regras de limpeza
        String cleaned = trimmed;
        for (CleanupRule rule : CLEANUP_RULES) {
            cleaned = rule.pattern.matcher(cleaned).replaceAll(rule.replacement);
        }
        cleaned = cleaned.trim();

        // Remover vírgula final
        if (cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        // Capitalizar cada palavra
        cleaned = capitalize(cleaned);

        return cleaned;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;

        // Preposições que não capitalizam (exceto no início)
        var lowerWords = Set.of("de", "do", "da", "dos", "das", "com", "em", "e",
                "of", "the", "and", "with", "in", "to", "for", "a", "an");

        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i > 0 && lowerWords.contains(word.toLowerCase())) {
                sb.append(word.toLowerCase());
            } else if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
            if (i < words.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private record CleanupRule(Pattern pattern, String replacement) {}
}