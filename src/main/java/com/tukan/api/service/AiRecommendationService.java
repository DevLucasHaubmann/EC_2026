package com.tukan.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.dto.ai.AiRecommendationResponse;
import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.RecommendationFeedbackRepository;
import com.tukan.api.repository.RecommendationRepository;
import com.tukan.api.service.ai.AiPromptBuilder;
import com.tukan.api.service.ai.AiProviderClient;
import com.tukan.api.service.ai.AiProviderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final UserService userService;
    private final AiContextService aiContextService;
    private final AiPromptBuilder aiPromptBuilder;
    private final AiProviderClient aiProviderClient;
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

        AiRecommendationContext context = aiContextService.build(user.getId());

        String systemPrompt = aiPromptBuilder.buildSystemPrompt();
        String userPrompt = aiPromptBuilder.buildUserPrompt(context);

        AiProviderResult providerResult = aiProviderClient.send(systemPrompt, userPrompt);

        AiRecommendationResponse parsed = parseResponse(providerResult.content());

        Recommendation recommendation = toEntity(user, parsed, context, providerResult);
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

    private Recommendation toEntity(User user, AiRecommendationResponse response,
                                     AiRecommendationContext context, AiProviderResult providerResult) {
        Recommendation recommendation = new Recommendation();
        recommendation.setUser(user);
        recommendation.setSummary(response.summary());
        recommendation.setRecommendations(toJson(response.recommendations()));
        recommendation.setAlerts(toJson(response.alerts()));
        recommendation.setContextJson(toJson(context));
        recommendation.setProvider(providerResult.provider());
        recommendation.setModel(providerResult.model());
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

    private AiRecommendationResponse parseResponse(String rawResponse) {
        try {
            AiRecommendationResponse response = objectMapper.readValue(rawResponse, AiRecommendationResponse.class);
            validateResponse(response);
            return response;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("A resposta da IA não pôde ser interpretada. Tente novamente.", e);
        }
    }

    private void validateResponse(AiRecommendationResponse response) {
        if (response.summary() == null || response.summary().isBlank()) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'resumo' ausente.");
        }
        if (response.recommendations() == null || response.recommendations().isEmpty()) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'recomendacoes' ausente.");
        }
        if (response.alerts() == null) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'alertas' ausente.");
        }
    }
}
