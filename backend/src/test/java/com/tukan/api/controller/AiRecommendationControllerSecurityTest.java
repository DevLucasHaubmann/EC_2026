package com.tukan.api.controller;

import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.AiRecommendationService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /ai/recommendations endpoints.
 * Only USER authority is permitted; ADMIN receives 403 and unauthenticated requests 401.
 */
@WebMvcTest(AiRecommendationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class AiRecommendationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiRecommendationService aiRecommendationService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void stubServices() {
        when(aiRecommendationService.findAllByUser(anyString())).thenReturn(List.of());
    }

    // -------------------------------------------------------------------------
    // USER ativo — acesso permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /ai/recommendations/me — USER ativo")
    class ActiveUser {

        @Test
        @DisplayName("USER ativo lista recomendações (200)")
        void userCanListRecommendations() throws Exception {
            mockMvc.perform(get("/ai/recommendations/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // ADMIN — não permitido (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /ai/recommendations/me — ADMIN")
    class AdminForbidden {

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint exclusivo de USER")
        void adminReceives403OnUserEndpoint() throws Exception {
            mockMvc.perform(get("/ai/recommendations/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Sem token — 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /ai/recommendations/me — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/ai/recommendations/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
