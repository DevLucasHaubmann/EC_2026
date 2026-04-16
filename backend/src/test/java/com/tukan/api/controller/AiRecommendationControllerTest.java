package com.tukan.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.FeedbackRequest;
import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.entity.User;
import com.tukan.api.exception.AiProviderException;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.exception.IncompleteProfileException;
import com.tukan.api.service.AiRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiRecommendationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class AiRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiRecommendationService aiRecommendationService;

    @Autowired
    private ObjectMapper objectMapper;

    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1);
        user.setName("Lucas");
        user.setEmail("lucas@email.com");

        recommendation = new Recommendation();
        recommendation.setId(1);
        recommendation.setUser(user);
        recommendation.setSummary("Resumo do perfil.");
        recommendation.setPlanJson("{\"dailyCalorieTarget\":2000,\"goal\":\"MAINTENANCE\",\"meals\":[]}");
        recommendation.setMealExplanationsJson("{\"BREAKFAST\":\"Café energético.\"}");
        recommendation.setTipsJson("[\"Beba água\"]");
        recommendation.setAlertsJson("[\"Alerta 1\"]");
        recommendation.setContextJson("{}");
        recommendation.setProvider("stub");
        recommendation.setModel("gemini-2.0-flash");
        recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);
        recommendation.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("POST /ai/recommendations/me")
    class GenerateRecommendation {

        @Test
        @DisplayName("should return 201 with persisted recommendation on success")
        void shouldReturn201OnSuccess() throws Exception {
            when(aiRecommendationService.generateAndSave(anyString())).thenReturn(recommendation);

            mockMvc.perform(post("/ai/recommendations/me")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.summary").value("Resumo do perfil."))
                    .andExpect(jsonPath("$.plan.goal").value("MAINTENANCE"))
                    .andExpect(jsonPath("$.tips[0]").value("Beba água"))
                    .andExpect(jsonPath("$.alerts[0]").value("Alerta 1"))
                    .andExpect(jsonPath("$.provider").value("stub"))
                    .andExpect(jsonPath("$.status").value("GENERATED"));
        }

        @Test
        @DisplayName("should return 422 when profile is incomplete")
        void shouldReturn422OnIncompleteProfile() throws Exception {
            when(aiRecommendationService.generateAndSave(anyString()))
                    .thenThrow(new IncompleteProfileException("perfil", "Perfil nutricional não encontrado para o usuário."));

            mockMvc.perform(post("/ai/recommendations/me")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.message").value("Perfil nutricional não encontrado para o usuário."));
        }

        @Test
        @DisplayName("should return 502 when AI provider fails")
        void shouldReturn502OnAiFailure() throws Exception {
            when(aiRecommendationService.generateAndSave(anyString()))
                    .thenThrow(new AiProviderException("A resposta da IA não pôde ser interpretada. Tente novamente."));

            mockMvc.perform(post("/ai/recommendations/me")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message").value("A resposta da IA não pôde ser interpretada. Tente novamente."));
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/ai/recommendations/me"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /ai/recommendations/me/latest")
    class GetLatest {

        @Test
        @DisplayName("should return latest recommendation")
        void shouldReturnLatestRecommendation() throws Exception {
            when(aiRecommendationService.findLatest(anyString())).thenReturn(recommendation);

            mockMvc.perform(get("/ai/recommendations/me/latest")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.summary").value("Resumo do perfil."));
        }

        @Test
        @DisplayName("should return 404 when no recommendation exists")
        void shouldReturn404WhenNoRecommendation() throws Exception {
            when(aiRecommendationService.findLatest(anyString()))
                    .thenThrow(new BusinessException("Nenhuma recomendação encontrada para este usuário.", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/ai/recommendations/me/latest")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/ai/recommendations/me/latest"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /ai/recommendations/me")
    class GetAll {

        @Test
        @DisplayName("should return list of recommendations")
        void shouldReturnListOfRecommendations() throws Exception {
            when(aiRecommendationService.findAllByUser(anyString())).thenReturn(List.of(recommendation));

            mockMvc.perform(get("/ai/recommendations/me")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should return empty list when user has no recommendations")
        void shouldReturnEmptyList() throws Exception {
            when(aiRecommendationService.findAllByUser(anyString())).thenReturn(List.of());

            mockMvc.perform(get("/ai/recommendations/me")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/ai/recommendations/me"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /ai/recommendations/{id}")
    class GetById {

        @Test
        @DisplayName("should return specific recommendation")
        void shouldReturnRecommendationById() throws Exception {
            when(aiRecommendationService.findById(eq(1), anyString())).thenReturn(recommendation);

            mockMvc.perform(get("/ai/recommendations/1")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("should return 404 when recommendation does not belong to user")
        void shouldReturn404WhenOwnershipFails() throws Exception {
            when(aiRecommendationService.findById(eq(1), anyString()))
                    .thenThrow(new BusinessException("Recomendação não encontrada.", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/ai/recommendations/1")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/ai/recommendations/1"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("PATCH /ai/recommendations/{id}/viewed")
    class MarkAsViewed {

        @Test
        @DisplayName("should return 200 with status VIEWED when marking as viewed")
        void shouldReturnViewedStatus() throws Exception {
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);
            when(aiRecommendationService.markAsViewed(eq(1), anyString())).thenReturn(recommendation);

            mockMvc.perform(patch("/ai/recommendations/1/viewed")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VIEWED"));
        }

        @Test
        @DisplayName("should return 200 when already VIEWED (idempotent)")
        void shouldReturn200WhenAlreadyViewed() throws Exception {
            recommendation.setStatus(Recommendation.RecommendationStatus.VIEWED);
            when(aiRecommendationService.markAsViewed(eq(1), anyString())).thenReturn(recommendation);

            mockMvc.perform(patch("/ai/recommendations/1/viewed")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VIEWED"));
        }

        @Test
        @DisplayName("should return 409 when recommendation is ARCHIVED")
        void shouldReturn409WhenArchived() throws Exception {
            when(aiRecommendationService.markAsViewed(eq(1), anyString()))
                    .thenThrow(new BusinessException("Recomendações arquivadas não podem ser marcadas como visualizadas.", HttpStatus.CONFLICT));

            mockMvc.perform(patch("/ai/recommendations/1/viewed")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when recommendation does not belong to user")
        void shouldReturn404WhenOwnershipFails() throws Exception {
            when(aiRecommendationService.markAsViewed(eq(1), anyString()))
                    .thenThrow(new BusinessException("Recomendação não encontrada.", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/ai/recommendations/1/viewed")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(patch("/ai/recommendations/1/viewed"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("POST /ai/recommendations/{id}/feedback")
    class AddFeedback {

        @Test
        @DisplayName("should return 201 with created feedback")
        void shouldReturn201WithFeedback() throws Exception {
            RecommendationFeedback feedback = new RecommendationFeedback();
            feedback.setId(1);
            feedback.setRecommendation(recommendation);
            feedback.setRating(RecommendationFeedback.Rating.LIKED);
            feedback.setReason("Muito útil");
            feedback.setCreatedAt(Instant.now());

            when(aiRecommendationService.addFeedback(eq(1), anyString(), any(FeedbackRequest.class)))
                    .thenReturn(feedback);

            String body = objectMapper.writeValueAsString(
                    new FeedbackRequest(RecommendationFeedback.Rating.LIKED, "Muito útil", null));

            mockMvc.perform(post("/ai/recommendations/1/feedback")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.rating").value("LIKED"))
                    .andExpect(jsonPath("$.reason").value("Muito útil"));
        }

        @Test
        @DisplayName("should return 409 when feedback already exists")
        void shouldReturn409WhenFeedbackAlreadyExists() throws Exception {
            when(aiRecommendationService.addFeedback(eq(1), anyString(), any(FeedbackRequest.class)))
                    .thenThrow(new BusinessException("Esta recomendação já possui feedback registrado.", HttpStatus.CONFLICT));

            String body = objectMapper.writeValueAsString(
                    new FeedbackRequest(RecommendationFeedback.Rating.LIKED, null, null));

            mockMvc.perform(post("/ai/recommendations/1/feedback")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when rating is absent")
        void shouldReturn400WhenRatingAbsent() throws Exception {
            String body = "{\"reason\": \"teste\"}";

            mockMvc.perform(post("/ai/recommendations/1/feedback")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when recommendation does not belong to user")
        void shouldReturn404WhenOwnershipFails() throws Exception {
            when(aiRecommendationService.addFeedback(eq(1), anyString(), any(FeedbackRequest.class)))
                    .thenThrow(new BusinessException("Recomendação não encontrada.", HttpStatus.NOT_FOUND));

            String body = objectMapper.writeValueAsString(
                    new FeedbackRequest(RecommendationFeedback.Rating.LIKED, null, null));

            mockMvc.perform(post("/ai/recommendations/1/feedback")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            String body = objectMapper.writeValueAsString(
                    new FeedbackRequest(RecommendationFeedback.Rating.LIKED, null, null));

            mockMvc.perform(post("/ai/recommendations/1/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("PATCH /ai/recommendations/{id}/archive")
    class Archive {

        @Test
        @DisplayName("should return 200 with status ARCHIVED")
        void shouldReturnArchivedStatus() throws Exception {
            recommendation.setStatus(Recommendation.RecommendationStatus.ARCHIVED);
            when(aiRecommendationService.archive(eq(1), anyString())).thenReturn(recommendation);

            mockMvc.perform(patch("/ai/recommendations/1/archive")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));
        }

        @Test
        @DisplayName("should return 409 when already archived")
        void shouldReturn409WhenAlreadyArchived() throws Exception {
            when(aiRecommendationService.archive(eq(1), anyString()))
                    .thenThrow(new BusinessException("Esta recomendação já está arquivada.", HttpStatus.CONFLICT));

            mockMvc.perform(patch("/ai/recommendations/1/archive")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when recommendation does not belong to user")
        void shouldReturn404WhenOwnershipFails() throws Exception {
            when(aiRecommendationService.archive(eq(1), anyString()))
                    .thenThrow(new BusinessException("Recomendação não encontrada.", HttpStatus.NOT_FOUND));

            mockMvc.perform(patch("/ai/recommendations/1/archive")
                            .with(jwt().jwt(j -> j.subject("lucas@email.com"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(patch("/ai/recommendations/1/archive"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
