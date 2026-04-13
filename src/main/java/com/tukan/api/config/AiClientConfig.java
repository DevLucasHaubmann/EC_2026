package com.tukan.api.config;

import com.google.genai.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
    public Client geminiClient(AiProperties properties) {
        return Client.builder()
                .apiKey(properties.getGemini().getApiKey())
                .build();
    }
}
