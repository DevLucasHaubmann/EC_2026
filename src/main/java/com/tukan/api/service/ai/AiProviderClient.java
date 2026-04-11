package com.tukan.api.service.ai;

public interface AiProviderClient {

    String send(String systemPrompt, String userPrompt);
}
