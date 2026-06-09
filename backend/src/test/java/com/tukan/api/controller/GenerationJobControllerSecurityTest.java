package com.tukan.api.controller;

import com.tukan.api.dto.GenerationJobResponse;
import com.tukan.api.entity.GenerationJobStatus;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.AiRecommendationAsyncService;
import com.tukan.api.service.GenerationJobService;
import com.tukan.api.service.GenerationJobSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /ai/recommendations/jobs endpoints.
 * Only USER authority is permitted; ADMIN receives 403 and unauthenticated requests 401.
 */
@WebMvcTest(GenerationJobController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class GenerationJobControllerSecurityTest {

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

    private static final UUID SAMPLE_JOB_ID = UUID.randomUUID();

    @BeforeEach
    void stubServices() {
        GenerationJobResponse stubJob = new GenerationJobResponse(
                SAMPLE_JOB_ID, GenerationJobStatus.PENDING, null, null);
        when(generationJobService.getStatusForOwner(any(UUID.class), anyString()))
                .thenReturn(stubJob);
    }

    // -------------------------------------------------------------------------
    // USER ativo — acesso permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /ai/recommendations/jobs/{jobId} — USER ativo")
    class ActiveUser {

        @Test
        @DisplayName("USER ativo acessa status do job (200)")
        void userCanAccessJobStatus() throws Exception {
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}", SAMPLE_JOB_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // ADMIN — não permitido (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /ai/recommendations/jobs/{jobId} — ADMIN")
    class AdminForbidden {

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint exclusivo de USER")
        void adminReceives403OnUserEndpoint() throws Exception {
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}", SAMPLE_JOB_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Sem token — 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /ai/recommendations/jobs/{jobId} — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/ai/recommendations/jobs/{jobId}", SAMPLE_JOB_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
