package com.tukan.api.controller;

import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.AssessmentService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /assessments endpoints.
 *
 * Two authorization groups:
 *  - Self-service (/me): USER permitted, ADMIN forbidden (403).
 *  - Admin by userId: ADMIN permitted, USER forbidden (403).
 *  - Unauthenticated: 401 on any endpoint.
 */
@WebMvcTest(AssessmentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class AssessmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void stubServices() {
        com.tukan.api.entity.Assessment stubEntity =
                Mockito.mock(com.tukan.api.entity.Assessment.class);
        com.tukan.api.entity.User stubUser = Mockito.mock(com.tukan.api.entity.User.class);

        when(stubUser.getId()).thenReturn(1);
        when(stubUser.getName()).thenReturn("Test User");
        when(stubEntity.getId()).thenReturn(1);
        when(stubEntity.getUser()).thenReturn(stubUser);
        when(stubEntity.getCreatedAt()).thenReturn(Instant.EPOCH);
        when(stubEntity.getUpdatedAt()).thenReturn(Instant.EPOCH);

        when(assessmentService.findOwn(anyString())).thenReturn(stubEntity);
        when(assessmentService.findByUserId(any(Integer.class))).thenReturn(stubEntity);
        when(assessmentService.findAll(any(Pageable.class))).thenReturn(Page.empty());
    }

    // -------------------------------------------------------------------------
    // Self-service /me — USER permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /assessments/me — USER ativo (self-service)")
    class SelfServiceUser {

        @Test
        @DisplayName("USER ativo acessa própria triagem (200)")
        void userCanAccessOwnAssessment() throws Exception {
            mockMvc.perform(get("/assessments/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint self-service de USER")
        void adminReceives403OnSelfServiceEndpoint() throws Exception {
            mockMvc.perform(get("/assessments/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Admin por userId — ADMIN permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /assessments/users/{userId} — ADMIN")
    class AdminByUserId {

        @Test
        @DisplayName("ADMIN acessa triagem de qualquer usuário por userId (200)")
        void adminCanAccessAssessmentByUserId() throws Exception {
            mockMvc.perform(get("/assessments/users/{userId}", 42)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER recebe 403 em endpoint admin por userId")
        void userReceives403OnAdminEndpoint() throws Exception {
            mockMvc.perform(get("/assessments/users/{userId}", 42)
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Sem token — 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /assessments/me — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/assessments/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
