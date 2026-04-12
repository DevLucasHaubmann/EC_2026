package com.tukan.api.controller;

import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.dto.FeedbackResponse;
import com.tukan.api.dto.RecomendacaoResponse;
import com.tukan.api.entity.FeedbackRecomendacao;
import com.tukan.api.entity.Recomendacao;
import com.tukan.api.service.AiRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/ai/recommendation/me")
    public ResponseEntity<RecomendacaoResponse> generateRecommendation(Authentication authentication) {
        Recomendacao recomendacao = aiRecommendationService.generateAndSave(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecomendacaoResponse.from(recomendacao));
    }

    @GetMapping("/ai/recommendation/me/latest")
    public ResponseEntity<RecomendacaoResponse> getLatest(Authentication authentication) {
        Recomendacao recomendacao = aiRecommendationService.findLatest(authentication.getName());
        return ResponseEntity.ok(RecomendacaoResponse.from(recomendacao));
    }

    @GetMapping("/ai/recommendations/me")
    public ResponseEntity<List<RecomendacaoResponse>> getAll(Authentication authentication) {
        List<RecomendacaoResponse> responses = aiRecommendationService.findAllByUser(authentication.getName())
                .stream()
                .map(RecomendacaoResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/ai/recommendations/{id}")
    public ResponseEntity<RecomendacaoResponse> getById(@PathVariable Integer id, Authentication authentication) {
        Recomendacao recomendacao = aiRecommendationService.findById(id, authentication.getName());
        return ResponseEntity.ok(RecomendacaoResponse.from(recomendacao));
    }

    @PatchMapping("/ai/recommendations/{id}/viewed")
    public ResponseEntity<RecomendacaoResponse> markAsViewed(@PathVariable Integer id, Authentication authentication) {
        Recomendacao recomendacao = aiRecommendationService.markAsViewed(id, authentication.getName());
        return ResponseEntity.ok(RecomendacaoResponse.from(recomendacao));
    }

    @PostMapping("/ai/recommendations/{id}/feedback")
    public ResponseEntity<FeedbackResponse> addFeedback(@PathVariable Integer id,
                                                         @Valid @RequestBody FeedbackRequest request,
                                                         Authentication authentication) {
        FeedbackRecomendacao feedback = aiRecommendationService.addFeedback(id, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.from(feedback));
    }

    @PatchMapping("/ai/recommendations/{id}/archive")
    public ResponseEntity<RecomendacaoResponse> archive(@PathVariable Integer id, Authentication authentication) {
        Recomendacao recomendacao = aiRecommendationService.archive(id, authentication.getName());
        return ResponseEntity.ok(RecomendacaoResponse.from(recomendacao));
    }
}
