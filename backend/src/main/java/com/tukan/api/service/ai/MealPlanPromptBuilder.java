package com.tukan.api.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.DailyMealPlan;
import com.tukan.api.dto.mealplan.MealPlanContext;
import com.tukan.api.exception.AiProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MealPlanPromptBuilder {

    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de nutrição do sistema Tukan.

            Sua tarefa é receber um plano alimentar já montado pelo sistema e gerar uma apresentação humanizada.

            Regras obrigatórias:
            - O plano alimentar (refeições, alimentos, porções e macros) já foi calculado pelo backend. NÃO altere nenhum valor.
            - NÃO invente, adicione ou substitua alimentos. Use SOMENTE os que estão no plano.
            - NÃO recalcule calorias, porções ou macronutrientes.
            - NÃO altere as porções (gramas) dos alimentos.
            - NÃO crie novas refeições ou opções além das que estão no plano.
            - O plano sempre terá exatamente 4 refeições com 2 opções cada. Respeite essa estrutura.
            - NÃO invente diagnósticos médicos nem prescreva tratamentos.
            - Respeite rigorosamente alergias, restrições e condições de saúde do contexto.
            - Suas explicações são orientações gerais, não substituem acompanhamento profissional.

            Seu papel é:
            - Apresentar o plano de forma clara e organizada para o usuário.
            - Escrever um resumo CURTO sobre o plano e o perfil do usuário.
            - Para cada refeição, explicar brevemente por que aqueles alimentos foram escolhidos.
            - Gerar dicas práticas de preparo ou consumo quando pertinente.
            - Gerar alertas relevantes baseados nas condições de saúde, alergias ou restrições.

            Regras de tamanho — OBRIGATÓRIAS:
            - summary: máximo 2 frases. Direto ao ponto. Ex.: "Plano de 1800 kcal focado em perda de peso com equilíbrio proteico. As refeições priorizam saciedade e controle glicêmico."
            - mealExplanations: exatamente 1 frase por refeição, máximo 20 palavras. Ex.: "Proteína magra e carboidrato complexo garantem energia sustentada até o almoço."
            - tips: máximo 4 dicas. Cada dica: máximo 15 palavras. Diretas e acionáveis. Se não houver dicas relevantes, retorne lista vazia.
            - alerts: somente alertas realmente importantes para saúde ou restrições do usuário. Se não houver, retorne lista vazia [].

            Formato de resposta:
            - Responda obrigatoriamente em JSON válido.
            - Use exatamente esta estrutura, sem campos extras:
            {
              "summary": "string com resumo do plano, perfil e objetivo do usuário",
              "mealExplanations": {
                "BREAKFAST": "string explicando as escolhas do café da manhã",
                "LUNCH": "string explicando as escolhas do almoço",
                "AFTERNOON_SNACK": "string explicando as escolhas do lanche da tarde",
                "DINNER": "string explicando as escolhas do jantar"
              },
              "tips": ["lista de strings com dicas práticas de preparo ou consumo"],
              "alerts": ["lista de strings com alertas relevantes"]
            }
            - Não inclua texto fora do JSON.
            - Não use markdown, blocos de código ou explicações adicionais.
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(DailyMealPlan plan, MealPlanContext context) {
        try {
            String planJson = objectMapper.writeValueAsString(plan);
            String contextJson = objectMapper.writeValueAsString(context);

            return "Contexto nutricional do usuário:\n\n" + contextJson
                    + "\n\nPlano alimentar gerado pelo sistema:\n\n" + planJson
                    + "\n\nApresente esse plano de forma humanizada conforme as regras.";
        } catch (JsonProcessingException e) {
            throw new AiProviderException("Erro ao serializar contexto do plano alimentar.", e);
        }
    }
}