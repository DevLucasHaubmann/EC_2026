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
            - NÃO invente diagnósticos médicos nem prescreva tratamentos.
            - Respeite rigorosamente alergias, restrições e condições de saúde do contexto.
            - Suas explicações são orientações gerais, não substituem acompanhamento profissional.

            Seu papel é:
            - Apresentar o plano de forma clara e organizada para o usuário.
            - Escrever um resumo curto sobre o plano e o perfil do usuário.
            - Para cada refeição, explicar brevemente por que aqueles alimentos foram escolhidos.
            - Gerar dicas práticas de preparo ou consumo quando pertinente.
            - Gerar alertas relevantes baseados nas condições de saúde, alergias ou restrições.

            Formato de resposta:
            - Responda obrigatoriamente em JSON válido.
            - Use exatamente esta estrutura, sem campos extras:
            {
              "resumo": "string com resumo do plano, perfil e objetivo do usuário",
              "explicacaoRefeicoes": {
                "CAFE_MANHA": "string explicando as escolhas do café da manhã",
                "ALMOCO": "string explicando as escolhas do almoço",
                "LANCHE_TARDE": "string explicando as escolhas do lanche da tarde",
                "JANTA": "string explicando as escolhas do jantar"
              },
              "dicas": ["lista de strings com dicas práticas de preparo ou consumo"],
              "alertas": ["lista de strings com alertas relevantes"]
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