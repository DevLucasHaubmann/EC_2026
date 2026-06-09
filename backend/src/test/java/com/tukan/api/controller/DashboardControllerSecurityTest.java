package com.tukan.api.controller;

import com.tukan.api.dto.DashboardResponse;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.MeService;
import com.tukan.api.service.MeService.AuthenticatedUserData;
import com.tukan.api.service.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /dashboard endpoints.
 * Only USER authority is permitted; ADMIN receives 403 and unauthenticated requests 401.
 */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeService meService;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void stubServices() {
        // Build a minimal AuthenticatedUserData with a mocked User that has a non-null id,
        // so DashboardResponse.from does not NPE when auth succeeds.
        com.tukan.api.entity.User stubUser = Mockito.mock(com.tukan.api.entity.User.class);
        when(stubUser.getId()).thenReturn(1);
        when(stubUser.getName()).thenReturn("Test User");

        AuthenticatedUserData stubData = new AuthenticatedUserData(stubUser, null, null);
        when(meService.findAuthenticatedUserData(anyString())).thenReturn(stubData);

        OnboardingStatus stubOnboarding = new OnboardingStatus(false, "/dashboard");
        when(onboardingService.checkOnboarding(any(Integer.class))).thenReturn(stubOnboarding);
    }

    // -------------------------------------------------------------------------
    // USER ativo — acesso permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /dashboard — USER ativo")
    class ActiveUser {

        @Test
        @DisplayName("USER ativo acessa o dashboard (200)")
        void userCanAccessDashboard() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // ADMIN — não permitido (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /dashboard — ADMIN")
    class AdminForbidden {

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint exclusivo de USER")
        void adminReceives403OnUserEndpoint() throws Exception {
            mockMvc.perform(get("/dashboard")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Sem token — 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /dashboard — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
