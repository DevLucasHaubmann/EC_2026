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
    private Gemini gemini = new Gemini();

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey;
        @NotBlank
        private String model = "gemini-3.1-flash-lite-preview";
        @Positive
        private int maxTokens = 1024;
        private double temperature = 0.3;
        @Positive
        private int timeoutSeconds = 30;
    }
}
