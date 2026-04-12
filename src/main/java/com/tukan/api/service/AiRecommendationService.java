package com.tukan.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.config.AiProperties;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.dto.ai.AiRecommendationResponse;
import com.tukan.api.entity.Recomendacao;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.RecomendacaoRepository;
import com.tukan.api.service.ai.AiPromptBuilder;
import com.tukan.api.service.ai.AiProviderClient;
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
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public Recomendacao generateAndSave(String authenticatedEmail) {
        User usuario = userService.findByEmail(authenticatedEmail);

        AiRecommendationContext contexto = aiContextService.build(usuario.getId());

        String systemPrompt = aiPromptBuilder.buildSystemPrompt();
        String userPrompt = aiPromptBuilder.buildUserPrompt(contexto);

        String rawResponse = aiProviderClient.send(systemPrompt, userPrompt);

        AiRecommendationResponse parsed = parseResponse(rawResponse);

        Recomendacao recomendacao = toEntity(usuario, parsed, contexto);
        return recomendacaoRepository.save(recomendacao);
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
        Integer usuarioId = userService.findByEmail(authenticatedEmail).getId();
        Recomendacao recomendacao = recomendacaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Recomendação não encontrada.", HttpStatus.NOT_FOUND));

        if (!recomendacao.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Acesso negado a esta recomendação.", HttpStatus.FORBIDDEN);
        }

        return recomendacao;
    }

    private Recomendacao toEntity(User usuario, AiRecommendationResponse response,
                                   AiRecommendationContext contexto) {
        Recomendacao recomendacao = new Recomendacao();
        recomendacao.setUsuario(usuario);
        recomendacao.setResumo(response.resumo());
        recomendacao.setRecomendacoes(toJson(response.recomendacoes()));
        recomendacao.setAlertas(toJson(response.alertas()));
        recomendacao.setContextoJson(toJson(contexto));
        recomendacao.setProvider(aiProperties.getProvider());
        recomendacao.setModel(aiProperties.getClaude().getModel());
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
