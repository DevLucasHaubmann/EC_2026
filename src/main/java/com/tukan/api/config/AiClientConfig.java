package com.tukan.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    @Bean
    public RestClient claudeRestClient(AiProperties properties) {
        AiProperties.Claude claude = properties.getClaude();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(claude.getTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(claude.getTimeoutSeconds()));

        return RestClient.builder()
                .baseUrl(claude.getBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
