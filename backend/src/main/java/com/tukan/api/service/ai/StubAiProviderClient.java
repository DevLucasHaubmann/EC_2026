package com.tukan.api.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "stub", matchIfMissing = true)
public class StubAiProviderClient implements AiProviderClient {

    private static final String RECOMMENDATION_STUB = """
            {
              "resumo": "Recomendação gerada com base no perfil e triagem do usuário.",
              "recomendacoes": [
                "Priorizar alimentos naturais e minimamente processados",
                "Manter uma boa hidratação ao longo do dia",
                "Distribuir as refeições de forma equilibrada"
              ],
              "alertas": [
                "Esta é uma recomendação inicial gerada automaticamente. Consulte um profissional de saúde para orientações personalizadas."
              ]
            }
            """;

    private static final String MEAL_PLAN_STUB = """
            {
              "resumo": "Plano alimentar gerado com base no seu perfil, objetivo e restrições. As refeições foram distribuídas para atender sua meta calórica diária.",
              "explicacaoRefeicoes": {
                "CAFE_MANHA": "O café da manhã foi montado para fornecer energia inicial com equilíbrio entre carboidratos e proteínas.",
                "ALMOCO": "O almoço concentra a maior parte das calorias, com foco em proteína e variedade de nutrientes.",
                "LANCHE_TARDE": "O lanche da tarde é leve e ajuda a manter a energia até o jantar.",
                "JANTA": "O jantar fecha o dia com uma refeição completa e de fácil digestão."
              },
              "dicas": [
                "Beba água ao longo do dia para manter uma boa hidratação",
                "Prepare os alimentos com temperos naturais para melhor sabor",
                "Tente manter horários regulares para as refeições"
              ],
              "alertas": [
                "Este plano é uma sugestão inicial gerada automaticamente. Consulte um nutricionista para acompanhamento personalizado."
              ]
            }
            """;

    @Override
    public AiProviderResult send(String systemPrompt, String userPrompt) {
        String response = isMealPlanPrompt(systemPrompt) ? MEAL_PLAN_STUB : RECOMMENDATION_STUB;
        return new AiProviderResult(response, "stub", "stub");
    }

    private boolean isMealPlanPrompt(String systemPrompt) {
        return systemPrompt != null && systemPrompt.contains("plano alimentar já montado pelo sistema");
    }
}
