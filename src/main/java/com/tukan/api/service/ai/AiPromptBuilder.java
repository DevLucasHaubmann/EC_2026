package com.tukan.api.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.exception.AiProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiPromptBuilder {

    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de apoio nutricional do sistema Tukan.

            Regras obrigatórias:
            - Gere apenas recomendações iniciais gerais com base no perfil e na triagem do usuário.
            - Não invente diagnósticos médicos nem prescreva tratamentos.
            - Respeite rigorosamente alergias, restrições alimentares e condições de saúde informadas.
            - Não recomende alimentos que conflitem com as alergias ou restrições listadas.
            - Considere o objetivo nutricional do usuário ao priorizar sugestões.
            - Suas recomendações são orientações gerais, não substituem acompanhamento profissional.

            Formato de resposta:
            - Responda obrigatoriamente em JSON válido.
            - Use exatamente esta estrutura, sem campos extras:
            {
              "resumo": "string com uma frase resumindo o perfil e objetivo do usuário",
              "recomendacoes": ["lista de strings com recomendações práticas"],
              "alertas": ["lista de strings com alertas relevantes baseados em condições de saúde, alergias ou restrições"]
            }
            - Não inclua texto fora do JSON.
            - Não use markdown, blocos de código ou explicações adicionais.
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(AiRecommendationContext contexto) {
        try {
            String contextJson = objectMapper.writeValueAsString(contexto);
            return "Analise o seguinte contexto nutricional e gere a recomendação inicial:\n\n" + contextJson;
        } catch (JsonProcessingException e) {
            throw new AiProviderException("Erro ao serializar contexto da IA.", e);
        }
    }
}
