package com.tukan.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.dto.mealplan.MealPlanRecommendationResponse;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.RecommendationFeedbackRepository;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.service.mealplan.MealPlanAiService;
import com.tukan.api.service.mealplan.MealPlanEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final UserService userService;
    private final MealPlanAiService mealPlanAiService;
    private final MealPlanEngine mealPlanEngine;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final ObjectMapper objectMapper;

    private static final List<Recommendation.RecommendationStatus> ACTIVE_STATUSES = List.of(
            Recommendation.RecommendationStatus.GENERATED,
            Recommendation.RecommendationStatus.VIEWED
    );

    @Transactional
    public Recommendation generateAndSave(String authenticatedEmail) {
        User user = userService.findByEmail(authenticatedEmail);

        archiveActiveRecommendations(user.getId());

        MealPlanRecommendationResponse response = mealPlanAiService.generate(authenticatedEmail);
        String contextJson = toJson(mealPlanEngine.buildContext(authenticatedEmail));

        Recommendation recommendation = toEntity(user, response, contextJson);
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

    @Transactional(readOnly = true)
    public Recommendation findLatest(String authenticatedEmail) {
        Integer userId = userService.findByEmail(authenticatedEmail).getId();
        return recommendationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma recomendação encontrada para este usuário.", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Recommendation> findAllByUser(String authenticatedEmail) {
        Integer userId = userService.findByEmail(authenticatedEmail).getId();
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Recommendation findById(Integer id, String authenticatedEmail) {
        return findByIdAndOwner(id, authenticatedEmail);
    }

    @Transactional
    public Recommendation markAsViewed(Integer id, String authenticatedEmail) {
        Recommendation recommendation = findByIdAndOwner(id, authenticatedEmail);

        if (recommendation.getStatus() != Recommendation.RecommendationStatus.GENERATED) {
            throw new BusinessException(
                    "Apenas recomendações com status GERADA podem ser marcadas como visualizadas.",
                    HttpStatus.CONFLICT);
        }

        recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);
        return recommendationRepository.save(recommendation);
    }

    @Transactional
    public RecommendationFeedback addFeedback(Integer id, String authenticatedEmail, FeedbackRequest request) {
        Recommendation recommendation = findByIdAndOwner(id, authenticatedEmail);

        if (recommendationFeedbackRepository.existsByRecommendationId(id)) {
            throw new BusinessException(
                    "Esta recomendação já possui feedback registrado.", HttpStatus.CONFLICT);
        }

        if (recommendation.getStatus() == Recommendation.RecommendationStatus.GENERATED) {
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);
            recommendationRepository.save(recommendation);
        }

        RecommendationFeedback feedback = new RecommendationFeedback();
        feedback.setRecommendation(recommendation);
        feedback.setRating(request.rating());
        feedback.setReason(request.reason());
        feedback.setObservation(request.observation());
        return recommendationFeedbackRepository.save(feedback);
    }

    @Transactional
    public Recommendation archive(Integer id, String authenticatedEmail) {
        Recommendation recommendation = findByIdAndOwner(id, authenticatedEmail);

        if (recommendation.getStatus() == Recommendation.RecommendationStatus.ARCHIVED) {
            throw new BusinessException(
                    "Esta recomendação já está arquivada.", HttpStatus.CONFLICT);
        }

        recommendation.setStatus(Recommendation.RecommendationStatus.ARCHIVED);
        return recommendationRepository.save(recommendation);
    }

    private Recommendation findByIdAndOwner(Integer id, String authenticatedEmail) {
        Integer userId = userService.findByEmail(authenticatedEmail).getId();
        return recommendationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(
                        "Recomendação não encontrada.", HttpStatus.NOT_FOUND));
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