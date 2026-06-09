package com.tukan.api.controller;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.service.AiRecommendationAsyncService;
import com.tukan.api.service.GenerationJobService;
import com.tukan.api.service.GenerationJobSseService;
import com.tukan.api.service.JobStartResult;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GenerationJobController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class GenerationJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerationJobService generationJobService;

    @MockitoBean
    private GenerationJobSseService generationJobSseService;

    @MockitoBean
    private AiRecommendationAsyncService aiRecommendationAsyncService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    private final UUID jobId = UUID.randomUUID();
    private static final String EMAIL = "lucas@email.com";

    private GenerationJobResponse jobResponse(GenerationJobStatus status) {
        return new GenerationJobResponse(jobId, status, null, null);
    }

    @Nested
    @DisplayName("POST /ai/recommendations/jobs")
    class StartJob {

        @Test
        @DisplayName("returns 202 and dispatches async generation when a new job is created")
        void shouldReturn202AndDispatchAsyncWhenJobCreated() throws Exception {
            // given
            when(generationJobService.startJob(anyString()))
                    .thenReturn(JobStartResult.created(jobResponse(GenerationJobStatus.PENDING)));

            // when / then
            mockMvc.perform(post("/ai/recommendations/jobs")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(aiRecommendationAsyncService).generateForJob(jobId);
        }

        @Test
        @DisplayName("returns 409 with the active job and does not dispatch async when one already runs")
        void shouldReturn409AndNotDispatchWhenJobAlreadyActive() throws Exception {
            // given
            when(generationJobService.startJob(anyString()))
                    .thenReturn(JobStartResult.alreadyActive(jobResponse(GenerationJobStatus.GENERATING_WITH_AI)));

            // when / then
            mockMvc.perform(post("/ai/recommendations/jobs")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                    .andExpect(jsonPath("$.status").value("GENERATING_WITH_AI"));

            verify(aiRecommendationAsyncService, never()).generateForJob(any());
        }

        @Test
        @DisplayName("returns 422 and never dispatches async when profile or assessment is incomplete")
        void shouldReturn422AndNotDispatchWhenProfileOrAssessmentIncomplete() throws Exception {
            // given
            when(generationJobService.startJob(anyString()))
                    .thenThrow(new BusinessException(
                            "Triagem não encontrada. Complete sua triagem antes de gerar um plano alimentar.",
                            HttpStatus.UNPROCESSABLE_ENTITY));

            // when / then
            mockMvc.perform(post("/ai/recommendations/jobs")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isUnprocessableEntity());

            verify(aiRecommendationAsyncService, never()).generateForJob(any());
        }

        @Test
        @DisplayName("rejects unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/ai/recommendations/jobs"))
                    .andExpect(status().is4xxClientError());

            verify(aiRecommendationAsyncService, never()).generateForJob(any());
        }
    }

    @Nested
    @DisplayName("GET /ai/recommendations/jobs/{jobId}")
    class GetStatus {

        @Test
        @DisplayName("returns 200 with the current job state for the owner")
        void shouldReturnStatusForOwner() throws Exception {
            // given
            when(generationJobService.getStatusForOwner(eq(jobId), anyString()))
                    .thenReturn(jobResponse(GenerationJobStatus.SAVING_RECOMMENDATION));

            // when / then
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}", jobId)
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                    .andExpect(jsonPath("$.status").value("SAVING_RECOMMENDATION"));
        }

        @Test
        @DisplayName("returns 404 when the job does not belong to the user or does not exist")
        void shouldReturn404WhenOwnershipFails() throws Exception {
            // given
            when(generationJobService.getStatusForOwner(eq(jobId), anyString()))
                    .thenThrow(new BusinessException("Job de geração não encontrado.", HttpStatus.NOT_FOUND));

            // when / then
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}", jobId)
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("rejects unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}", jobId))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /ai/recommendations/jobs/{jobId}/events")
    class StreamEvents {

        @Test
        @DisplayName("returns 200 text/event-stream and subscribes with the validated current state")
        void shouldReturnEventStreamForOwner() throws Exception {
            // given
            GenerationJobResponse state = jobResponse(GenerationJobStatus.PENDING);
            when(generationJobService.getStatusForOwner(eq(jobId), anyString())).thenReturn(state);
            SseEmitter emitter = new SseEmitter();
            emitter.complete(); // complete so asyncDispatch can render the response deterministically
            when(generationJobSseService.subscribe(eq(state), any())).thenReturn(emitter);

            // when
            MvcResult result = mockMvc.perform(get("/ai/recommendations/jobs/{jobId}/events", jobId)
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // then
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

            verify(generationJobService).getStatusForOwner(jobId, EMAIL);
            verify(generationJobSseService).subscribe(eq(state), any());
        }

        @Test
        @DisplayName("returns 404 and never opens an emitter when ownership validation fails")
        void shouldReturn404AndNotSubscribeWhenOwnershipFails() throws Exception {
            // given
            when(generationJobService.getStatusForOwner(eq(jobId), anyString()))
                    .thenThrow(new BusinessException("Job de geração não encontrado.", HttpStatus.NOT_FOUND));

            // when / then
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}/events", jobId)
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isNotFound());

            verify(generationJobSseService, never()).subscribe(any(), any());
        }

        @Test
        @DisplayName("rejects unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}/events", jobId))
                    .andExpect(status().is4xxClientError());

            verify(generationJobSseService, never()).subscribe(any(), any());
        }
    }
}
