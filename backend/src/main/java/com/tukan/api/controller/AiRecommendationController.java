package com.tukan.api.controller;

import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.dto.FeedbackResponse;
import com.tukan.api.dto.RecommendationResponse;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.entity.Recommendation;
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

    @PostMapping("/ai/recommendations/me")
    public ResponseEntity<RecommendationResponse> generateRecommendation(Authentication authentication) {
        Recommendation recommendation = aiRecommendationService.generateAndSave(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecommendationResponse.from(recommendation));
    }

    @GetMapping("/ai/recommendations/me/latest")
    public ResponseEntity<RecommendationResponse> getLatest(Authentication authentication) {
        Recommendation recommendation = aiRecommendationService.findLatest(authentication.getName());
        return ResponseEntity.ok(RecommendationResponse.from(recommendation));
    }

    @GetMapping("/ai/recommendations/me")
    public ResponseEntity<List<RecommendationResponse>> getAll(Authentication authentication) {
        List<RecommendationResponse> responses = aiRecommendationService.findAllByUser(authentication.getName())
                .stream()
                .map(RecommendationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/ai/recommendations/{id}")
    public ResponseEntity<RecommendationResponse> getById(@PathVariable Integer id, Authentication authentication) {
        Recommendation recommendation = aiRecommendationService.findById(id, authentication.getName());
        return ResponseEntity.ok(RecommendationResponse.from(recommendation));
    }

    @PatchMapping("/ai/recommendations/{id}/viewed")
    public ResponseEntity<RecommendationResponse> markAsViewed(@PathVariable Integer id, Authentication authentication) {
        Recommendation recommendation = aiRecommendationService.markAsViewed(id, authentication.getName());
        return ResponseEntity.ok(RecommendationResponse.from(recommendation));
    }

    @PostMapping("/ai/recommendations/{id}/feedback")
    public ResponseEntity<FeedbackResponse> addFeedback(@PathVariable Integer id,
                                                         @Valid @RequestBody FeedbackRequest request,
                                                         Authentication authentication) {
        RecommendationFeedback feedback = aiRecommendationService.addFeedback(id, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.from(feedback));
    }

    @PatchMapping("/ai/recommendations/{id}/archive")
    public ResponseEntity<RecommendationResponse> archive(@PathVariable Integer id, Authentication authentication) {
        Recommendation recommendation = aiRecommendationService.archive(id, authentication.getName());
        return ResponseEntity.ok(RecommendationResponse.from(recommendation));
    }
}