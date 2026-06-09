package com.tukan.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.MeResponse;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.MeService;
import com.tukan.api.service.MeService.AuthenticatedUserData;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer RBAC for GET /me and PATCH /me/avatar.
 * PATCH /me/avatar is USER-only; GET /me is accessible to any authenticated principal.
 * Also verifies that @Valid rejects invalid payloads with 400.
 */
@WebMvcTest(MeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class MeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MeService meService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String VALID_AVATAR_BODY = "{\"avatarUrl\":\"https://example.com/a.png\"}";

    private static AuthenticatedUserData stubData() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setType(User.UserType.USER);
        user.setStatus(User.UserState.ACTIVE);
        return new AuthenticatedUserData(user, null, null);
    }

    @BeforeEach
    void stubService() {
        AuthenticatedUserData data = stubData();
        when(meService.findAuthenticatedUserData(anyString())).thenReturn(data);
        when(meService.updateAvatar(anyString(), any())).thenReturn(data);
    }

    // -------------------------------------------------------------------------
    // GET /me — acessível a USER e ADMIN (endpoint compartilhado)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /me — acesso compartilhado")
    class GetMe {

        @Test
        @DisplayName("USER recebe 200")
        void user_returns200() throws Exception {
            mockMvc.perform(get("/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN recebe 200")
        void admin_returns200() throws Exception {
            mockMvc.perform(get("/me")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requisição sem token retorna 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /me/avatar — USER ativo (200)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /me/avatar — USER ativo")
    class PatchAvatarAsUser {

        @Test
        @DisplayName("USER com payload válido recebe 200")
        void user_validPayload_returns200() throws Exception {
            mockMvc.perform(patch("/me/avatar")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_AVATAR_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER com avatarUrl acima de 512 chars recebe 400")
        void user_avatarUrlTooLong_returns400() throws Exception {
            String longUrl = "https://example.com/" + "a".repeat(494);
            String body = "{\"avatarUrl\":\"" + longUrl + "\"}";

            mockMvc.perform(patch("/me/avatar")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("USER com avatarUrl de scheme inválido recebe 400 (rejeição propagada pelo controller)")
        void user_unsafeScheme_returns400() throws Exception {
            // Proves the HTTP layer (controller + GlobalExceptionHandler) translates the
            // service's scheme rejection into 400. The scheme-parsing logic itself is
            // unit-tested in MeServiceAvatarTest; here the service is mocked to reject.
            String unsafeUrl = "javascript:alert(1)";
            String body = "{\"avatarUrl\":\"" + unsafeUrl + "\"}";
            when(meService.updateAvatar(anyString(), eq(unsafeUrl)))
                    .thenThrow(new BusinessException("URL do avatar inválida.", HttpStatus.BAD_REQUEST));

            mockMvc.perform(patch("/me/avatar")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /me/avatar — ADMIN (403)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /me/avatar — ADMIN")
    class PatchAvatarAsAdmin {

        @Test
        @DisplayName("ADMIN recebe 403 em endpoint exclusivo de USER")
        void admin_returns403() throws Exception {
            mockMvc.perform(patch("/me/avatar")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_AVATAR_BODY))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /me/avatar — sem token (401)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /me/avatar — sem token")
    class PatchAvatarUnauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(patch("/me/avatar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_AVATAR_BODY))
                    .andExpect(status().isUnauthorized());
        }
    }
}
