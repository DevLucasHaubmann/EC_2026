package com.tukan.api.controller;

import com.tukan.api.dto.ai.AiRecommendationResponse;
import com.tukan.api.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/recommendation")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/me")
    public ResponseEntity<AiRecommendationResponse> generateRecommendation(Authentication authentication) {
        AiRecommendationResponse response = aiRecommendationService.generateRecommendation(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
