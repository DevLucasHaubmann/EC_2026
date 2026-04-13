package com.tukan.api.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "stub", matchIfMissing = true)
public class StubAiProviderClient implements AiProviderClient {

    private static final String STUB_RESPONSE = """
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

    @Override
    public AiProviderResult send(String systemPrompt, String userPrompt) {
        return new AiProviderResult(STUB_RESPONSE, "stub", "stub");
    }
}
