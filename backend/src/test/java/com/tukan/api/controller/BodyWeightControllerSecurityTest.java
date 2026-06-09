package com.tukan.api.controller;

import com.tukan.api.dto.bodyweight.BodyWeightLogResponse;
import com.tukan.api.dto.bodyweight.BodyWeightSummaryResponse;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.BodyWeightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /me/body-weight endpoints.
 * Only USER authority is permitted; ADMIN receives 403 and unauthenticated requests 401.
 * Also verifies that @Valid rejects invalid payloads with 400.
 */
@WebMvcTest(BodyWeightController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class BodyWeightControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BodyWeightService bodyWeightService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    private static final BodyWeightLogResponse STUB_LOG = new BodyWeightLogResponse(
            1L, new BigDecimal("75.00"), LocalDate.now(), Instant.now());

    private static final BodyWeightSummaryResponse STUB_SUMMARY = new BodyWeightSummaryResponse(
            new BigDecimal("75.00"), new BigDecimal("80.00"), new BigDecimal("-5.00"),
            "LOSS", 2, List.of(STUB_LOG));

    @BeforeEach
    void stubService() {
        when(bodyWeightService.register(anyString(), any())).thenReturn(STUB_LOG);
        when(bodyWeightService.getHistory(anyString())).thenReturn(List.of(STUB_LOG));
        when(bodyWeightService.getSummary(anyString())).thenReturn(STUB_SUMMARY);
    }

    // -------------------------------------------------------------------------
    // POST /me/body-weight — USER
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /me/body-weight — USER ativo")
    class PostAsUser {

        @Test
        @DisplayName("USER com payload válido recebe 200")
        void user_validPayload_returns200() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weightKg\": 75.00}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER com weightKg abaixo do mínimo recebe 400")
        void user_weightTooLow_returns400() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weightKg\": 5.00}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("USER sem campo weightKg recebe 400")
        void user_missingWeightKg_returns400() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("USER com weightKg com 3 casas decimais recebe 400")
        void user_weightThreeDecimalPlaces_returns400() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weightKg\": 75.999}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("USER com weightKg com 2 casas decimais válidas recebe 200")
        void user_weightTwoDecimalPlaces_returns200() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weightKg\": 75.99}"))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // POST /me/body-weight — ADMIN (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /me/body-weight — ADMIN")
    class PostAsAdmin {

        @Test
        @DisplayName("ADMIN recebe 403")
        void admin_returns403() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weightKg\": 75.00}"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /me/body-weight — sem token (401)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /me/body-weight — sem token")
    class PostUnauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(post("/me/body-weight")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weightKg\": 75.00}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /me/body-weight — USER
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /me/body-weight — USER ativo")
    class GetHistoryAsUser {

        @Test
        @DisplayName("USER recebe histórico com 200")
        void user_returns200() throws Exception {
            mockMvc.perform(get("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // GET /me/body-weight — ADMIN (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /me/body-weight — ADMIN")
    class GetHistoryAsAdmin {

        @Test
        @DisplayName("ADMIN recebe 403")
        void admin_returns403() throws Exception {
            mockMvc.perform(get("/me/body-weight")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // GET /me/body-weight — sem token (401)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /me/body-weight — sem token")
    class GetHistoryUnauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/me/body-weight"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /me/body-weight/summary — USER
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /me/body-weight/summary — USER ativo")
    class GetSummaryAsUser {

        @Test
        @DisplayName("USER recebe resumo com 200")
        void user_returns200() throws Exception {
            mockMvc.perform(get("/me/body-weight/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // GET /me/body-weight/summary — ADMIN (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /me/body-weight/summary — ADMIN")
    class GetSummaryAsAdmin {

        @Test
        @DisplayName("ADMIN recebe 403")
        void admin_returns403() throws Exception {
            mockMvc.perform(get("/me/body-weight/summary")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }
}
