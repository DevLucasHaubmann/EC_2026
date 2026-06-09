package com.tukan.api.controller;

import com.tukan.api.dto.ProfileResponse;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.NutritionalProfileService;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for /profiles endpoints.
 *
 * Two authorization groups:
 *  - Self-service (/me): USER permitted, ADMIN forbidden (403).
 *  - Admin by userId: ADMIN permitted, USER forbidden (403).
 *  - Unauthenticated: 401 on any endpoint.
 */
@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class ProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NutritionalProfileService nutritionalProfileService;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    private static final ProfileResponse STUB_PROFILE = new ProfileResponse(
            1, 1, "Test User", LocalDate.of(1990, 1, 1), null,
            70.0, 175.0, null, Instant.EPOCH, Instant.EPOCH);

    @BeforeEach
    void stubServices() {
        com.tukan.api.entity.NutritionalProfile stubEntity =
                Mockito.mock(com.tukan.api.entity.NutritionalProfile.class);
        when(nutritionalProfileService.findOwn(anyString())).thenReturn(stubEntity);
        when(nutritionalProfileService.findByUserId(any(Integer.class))).thenReturn(stubEntity);
        when(nutritionalProfileService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        com.tukan.api.entity.User stubUser = Mockito.mock(com.tukan.api.entity.User.class);
        when(stubUser.getId()).thenReturn(1);
        when(stubUser.getName()).thenReturn("Test User");
        when(stubEntity.getId()).thenReturn(1);
        when(stubEntity.getUser()).thenReturn(stubUser);
        when(stubEntity.getDateOfBirth()).thenReturn(LocalDate.of(1990, 1, 1));
        when(stubEntity.getCreatedAt()).thenReturn(Instant.EPOCH);
        when(stubEntity.getUpdatedAt()).thenReturn(Instant.EPOCH);
    }

    // -------------------------------------------------------------------------
    // Self-service /me — USER permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /profiles/me — USER ativo (self-service)")
    class SelfServiceUser {

        @Test
        @DisplayName("USER ativo acessa próprio perfil (200)")
        void userCanAccessOwnProfile() throws Exception {
            mockMvc.perform(get("/profiles/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint self-service de USER")
        void adminReceives403OnSelfServiceEndpoint() throws Exception {
            mockMvc.perform(get("/profiles/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Admin por userId — ADMIN permitido
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /profiles/users/{userId} — ADMIN")
    class AdminByUserId {

        @Test
        @DisplayName("ADMIN acessa perfil de qualquer usuário por userId (200)")
        void adminCanAccessProfileByUserId() throws Exception {
            mockMvc.perform(get("/profiles/users/{userId}", 42)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER recebe 403 em endpoint admin por userId")
        void userReceives403OnAdminEndpoint() throws Exception {
            mockMvc.perform(get("/profiles/users/{userId}", 42)
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Sem token — 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /profiles/me — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/profiles/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
