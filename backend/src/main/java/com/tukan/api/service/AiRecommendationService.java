package com.tukan.api.service;

import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.RecommendationFeedbackRepository;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.service.mealplan.MealPlanAiService;
import com.tukan.api.service.mealplan.MealPlanGenerationResult;
import com.tukan.api.service.mealplan.MealPlanPreparation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final UserService userService;
    private final MealPlanReadinessValidator readinessValidator;
    private final MealPlanAiService mealPlanAiService;
    private final RecommendationPersistenceService recommendationPersistenceService;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;

    /**
     * Orchestrates the synchronous generation flow in three decoupled phases so the
     * external AI call never runs inside a database transaction:
     * <ol>
     *   <li>prepare: deterministic plan/context in a short read-only transaction;</li>
     *   <li>enrich: external AI call with no transaction held;</li>
     *   <li>persist: archive + save in a short write transaction.</li>
     * </ol>
     * Intentionally NOT {@code @Transactional}: each phase owns its own transactional
     * boundary through its bean proxy.
     *
     * <p>The same readiness gate used by the asynchronous flow runs first, so an incomplete
     * profile/assessment is rejected with a clear triage error before the engine is ever reached.
     */
    public Recommendation generateAndSave(String authenticatedEmail) {
        Integer userId = userService.findByEmail(authenticatedEmail).getId();
        readinessValidator.validate(userId);
        MealPlanPreparation preparation = mealPlanAiService.prepare(authenticatedEmail);
        MealPlanGenerationResult result = mealPlanAiService.enrich(preparation);
        return recommendationPersistenceService.persist(authenticatedEmail, result);
    }

    @Transactional(readOnly = true)
    public Recommendation findLatest(String authenticatedEmail) {
        Integer userId = userService.findByEmail(authenticatedEmail).getId();
        return recommendationRepository
                .findFirstByUserIdAndStatusInOrderByCreatedAtDesc(userId, Recommendation.ACTIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma dieta ativa encontrada para este usuário.", HttpStatus.NOT_FOUND));
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

        if (recommendation.getStatus() == Recommendation.RecommendationStatus.ARCHIVED) {
            throw new BusinessException(
                    "Recomendações arquivadas não podem ser marcadas como visualizadas.",
                    HttpStatus.CONFLICT);
        }

        if (recommendation.getStatus() == Recommendation.RecommendationStatus.VIEWED) {
            return recommendation;
        }

        recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);
        return recommendationRepository.save(recommendation);
    }

    @Transactional
    public RecommendationFeedback upsertFeedback(Integer id, String authenticatedEmail, FeedbackRequest request) {
        Recommendation recommendation = findByIdAndOwner(id, authenticatedEmail);

        if (recommendation.getStatus() == Recommendation.RecommendationStatus.ARCHIVED) {
            throw new BusinessException(
                    "Não é possível enviar feedback para recomendações arquivadas.",
                    HttpStatus.CONFLICT);
        }

        RecommendationFeedback feedback = recommendationFeedbackRepository
                .findByRecommendationId(id)
                .orElseGet(RecommendationFeedback::new);

        feedback.setRecommendation(recommendation);
        feedback.setRating(request.rating());
        feedback.setLikedTags(request.likedTags() != null ? request.likedTags() : List.of());
        feedback.setDislikedTags(request.dislikedTags() != null ? request.dislikedTags() : List.of());
        feedback.setComment(request.comment());

        if (recommendation.getStatus() == Recommendation.RecommendationStatus.GENERATED) {
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);
            recommendationRepository.save(recommendation);
        }

        return recommendationFeedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public Optional<RecommendationFeedback> getFeedback(Integer id, String authenticatedEmail) {
        findByIdAndOwner(id, authenticatedEmail);
        return recommendationFeedbackRepository.findByRecommendationId(id);
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
}
