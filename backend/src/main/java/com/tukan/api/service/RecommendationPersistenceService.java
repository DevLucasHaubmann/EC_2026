package com.tukan.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.mealplan.MealPlanRecommendationResponse;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.service.mealplan.MealPlanGenerationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phase 3 — Persistence. Persists a freshly generated recommendation in a single
 * short write transaction, decoupled from the long external AI call. Archiving the
 * previously active recommendation and saving the new one are atomic.
 *
 * <p>Lives in a dedicated bean so the transactional boundary is applied through the
 * Spring proxy (no self-invocation), and so both the synchronous flow and the future
 * asynchronous flow can reuse it as an independent transaction.
 */
@Service
@RequiredArgsConstructor
public class RecommendationPersistenceService {

    private final UserService userService;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    private static final List<Recommendation.RecommendationStatus> ACTIVE_STATUSES = List.of(
            Recommendation.RecommendationStatus.GENERATED,
            Recommendation.RecommendationStatus.VIEWED
    );

    @Transactional
    public Recommendation persist(String authenticatedEmail, MealPlanGenerationResult result) {
        User user = userService.findByEmail(authenticatedEmail);

        archiveActiveRecommendations(user.getId());

        String contextJson = toJson(result.context());
        Recommendation recommendation = toEntity(user, result.response(), contextJson);
        return recommendationRepository.save(recommendation);
    }

    private void archiveActiveRecommendations(Integer userId) {
        List<Recommendation> activeRecommendations = recommendationRepository
                .findByUserIdAndStatusIn(userId, ACTIVE_STATUSES);

        for (Recommendation rec : activeRecommendations) {
            rec.setStatus(Recommendation.RecommendationStatus.ARCHIVED);
        }

        recommendationRepository.saveAll(activeRecommendations);
    }

    private Recommendation toEntity(User user, MealPlanRecommendationResponse response, String contextJson) {
        Recommendation recommendation = new Recommendation();
        recommendation.setUser(user);
        recommendation.setSummary(response.summary());
        recommendation.setPlanJson(toJson(response.plan()));
        recommendation.setMealExplanationsJson(toJson(response.mealExplanations()));
        recommendation.setTipsJson(toJson(response.tips()));
        recommendation.setAlertsJson(toJson(response.alerts()));
        recommendation.setContextJson(contextJson);
        recommendation.setProvider(response.provider());
        recommendation.setModel(response.model());
        recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);
        return recommendation;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AiProviderException("Erro ao serializar dados da recomendação.", e);
        }
    }
}
