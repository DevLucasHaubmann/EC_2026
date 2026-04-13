package com.tukan.api.service.ai;

public interface AiProviderClient {

    AiProviderResult send(String systemPrompt, String userPrompt);
}
