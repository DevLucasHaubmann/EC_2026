package com.tukan.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.dto.ai.AiRecommendationResponse;
import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.entity.FeedbackRecomendacao;
import com.tukan.api.entity.Recomendacao;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.FeedbackRecomendacaoRepository;
import com.tukan.api.repository.RecomendacaoRepository;
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
    private final RecomendacaoRepository recomendacaoRepository;
    private final FeedbackRecomendacaoRepository feedbackRecomendacaoRepository;
    private final ObjectMapper objectMapper;

    private static final List<Recomendacao.StatusRecomendacao> ACTIVE_STATUSES = List.of(
            Recomendacao.StatusRecomendacao.GERADA,
            Recomendacao.StatusRecomendacao.VISUALIZADA
    );

    @Transactional
    public Recomendacao generateAndSave(String authenticatedEmail) {
        User usuario = userService.findByEmail(authenticatedEmail);

        archiveActiveRecommendations(usuario.getId());

        AiRecommendationContext contexto = aiContextService.build(usuario.getId());

        String systemPrompt = aiPromptBuilder.buildSystemPrompt();
        String userPrompt = aiPromptBuilder.buildUserPrompt(contexto);

        AiProviderResult providerResult = aiProviderClient.send(systemPrompt, userPrompt);

        AiRecommendationResponse parsed = parseResponse(providerResult.content());

        Recomendacao recomendacao = toEntity(usuario, parsed, contexto, providerResult);
        return recomendacaoRepository.save(recomendacao);
    }

    private void archiveActiveRecommendations(Integer usuarioId) {
        List<Recomendacao> activeRecommendations = recomendacaoRepository
                .findByUsuarioIdAndStatusIn(usuarioId, ACTIVE_STATUSES);

        for (Recomendacao rec : activeRecommendations) {
            rec.setStatus(Recomendacao.StatusRecomendacao.ARQUIVADA);
        }

        recomendacaoRepository.saveAll(activeRecommendations);
    }

    @Transactional(readOnly = true)
    public Recomendacao findLatest(String authenticatedEmail) {
        Integer usuarioId = userService.findByEmail(authenticatedEmail).getId();
        return recomendacaoRepository.findFirstByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma recomendação encontrada para este usuário.", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Recomendacao> findAllByUser(String authenticatedEmail) {
        Integer usuarioId = userService.findByEmail(authenticatedEmail).getId();
        return recomendacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public Recomendacao findById(Integer id, String authenticatedEmail) {
        return findByIdAndOwner(id, authenticatedEmail);
    }

    @Transactional
    public Recomendacao markAsViewed(Integer id, String authenticatedEmail) {
        Recomendacao recomendacao = findByIdAndOwner(id, authenticatedEmail);

        if (recomendacao.getStatus() != Recomendacao.StatusRecomendacao.GERADA) {
            throw new BusinessException(
                    "Apenas recomendações com status GERADA podem ser marcadas como visualizadas.",
                    HttpStatus.CONFLICT);
        }

        recomendacao.setStatus(Recomendacao.StatusRecomendacao.VISUALIZADA);
        return recomendacaoRepository.save(recomendacao);
    }

    @Transactional
    public FeedbackRecomendacao addFeedback(Integer id, String authenticatedEmail, FeedbackRequest request) {
        Recomendacao recomendacao = findByIdAndOwner(id, authenticatedEmail);

        if (feedbackRecomendacaoRepository.existsByRecomendacaoId(id)) {
            throw new BusinessException(
                    "Esta recomendação já possui feedback registrado.", HttpStatus.CONFLICT);
        }

        FeedbackRecomendacao feedback = new FeedbackRecomendacao();
        feedback.setRecomendacao(recomendacao);
        feedback.setAvaliacao(request.avaliacao());
        feedback.setMotivo(request.motivo());
        feedback.setObservacao(request.observacao());
        return feedbackRecomendacaoRepository.save(feedback);
    }

    @Transactional
    public Recomendacao archive(Integer id, String authenticatedEmail) {
        Recomendacao recomendacao = findByIdAndOwner(id, authenticatedEmail);

        if (recomendacao.getStatus() == Recomendacao.StatusRecomendacao.ARQUIVADA) {
            throw new BusinessException(
                    "Esta recomendação já está arquivada.", HttpStatus.CONFLICT);
        }

        recomendacao.setStatus(Recomendacao.StatusRecomendacao.ARQUIVADA);
        return recomendacaoRepository.save(recomendacao);
    }

    private Recomendacao findByIdAndOwner(Integer id, String authenticatedEmail) {
        Integer usuarioId = userService.findByEmail(authenticatedEmail).getId();
        return recomendacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new BusinessException(
                        "Recomendação não encontrada.", HttpStatus.NOT_FOUND));
    }

    private Recomendacao toEntity(User usuario, AiRecommendationResponse response,
                                   AiRecommendationContext contexto, AiProviderResult providerResult) {
        Recomendacao recomendacao = new Recomendacao();
        recomendacao.setUsuario(usuario);
        recomendacao.setResumo(response.resumo());
        recomendacao.setRecomendacoes(toJson(response.recomendacoes()));
        recomendacao.setAlertas(toJson(response.alertas()));
        recomendacao.setContextoJson(toJson(contexto));
        recomendacao.setProvider(providerResult.provider());
        recomendacao.setModel(providerResult.model());
        recomendacao.setStatus(Recomendacao.StatusRecomendacao.GERADA);
        return recomendacao;
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
        if (response.resumo() == null || response.resumo().isBlank()) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'resumo' ausente.");
        }
        if (response.recomendacoes() == null || response.recomendacoes().isEmpty()) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'recomendacoes' ausente.");
        }
        if (response.alertas() == null) {
            throw new AiProviderException("A resposta da IA está incompleta: campo 'alertas' ausente.");
        }
    }
}
