package com.tukan.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    @NotBlank
    private String provider = "stub";

    @Valid
    private Claude claude = new Claude();

    @Getter
    @Setter
    public static class Claude {
        private String apiKey;
        @NotBlank
        private String baseUrl = "https://api.anthropic.com";
        @NotBlank
        private String model = "claude-sonnet-4-20250514";
        @Positive
        private int maxTokens = 1024;
        private double temperature = 0.4;
        @Positive
        private int timeoutSeconds = 30;
    }
}
