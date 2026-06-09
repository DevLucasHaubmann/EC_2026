package com.tukan.api.controller;

import com.tukan.api.dto.evolution.DailyEvolutionMetrics;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.EvolutionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /evolution endpoints.
 * Only USER authority is permitted; ADMIN receives 403 and unauthenticated requests 401.
 */
@WebMvcTest(EvolutionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class EvolutionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvolutionService evolutionService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void stubServices() {
        // getDailyMetrics with start==end must return a list with at least one element
        // to avoid IndexOutOfBoundsException in the controller when auth succeeds.
        DailyEvolutionMetrics stubMetrics = new DailyEvolutionMetrics(
                java.time.LocalDate.now(), 0.0, 0.0, 0.0, 0.0, 0, 0, null);
        when(evolutionService.getDailyMetrics(anyString(), any(), any()))
                .thenReturn(List.of(stubMetrics));
    }

    // -------------------------------------------------------------------------
    // USER ativo — acesso permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /evolution/daily — USER ativo")
    class ActiveUser {

        @Test
        @DisplayName("USER ativo acessa métricas diárias (200)")
        void userCanAccessDailyEvolution() throws Exception {
            mockMvc.perform(get("/evolution/daily")
                            .param("date", "2025-01-01")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // ADMIN — não permitido (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /evolution/daily — ADMIN")
    class AdminForbidden {

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint exclusivo de USER")
        void adminReceives403OnUserEndpoint() throws Exception {
            mockMvc.perform(get("/evolution/daily")
                            .param("date", "2025-01-01")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Sem token — 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /evolution/daily — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/evolution/daily")
                            .param("date", "2025-01-01"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
