package com.tukan.api.controller;

import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HTTP-layer authorization for /users endpoints.
 *
 * SecurityConfig is imported explicitly so that @EnableMethodSecurity is active
 * and @PreAuthorize("hasAuthority('ADMIN')") on UserController is enforced.
 * A test jwt.secret is provided via @TestPropertySource to satisfy SecurityConfig's
 * @Value("${jwt.secret}") without requiring a real datasource or profile.
 *
 * The converter is mocked so the context boots without a real UserRepository.
 * Using .with(jwt().authorities(...)) injects a pre-built JwtAuthenticationToken
 * directly — the converter unit test covers the DB-backed authority resolution.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    /*
     * DatabaseBackedJwtAuthenticationConverter is a @Component that Spring
     * Security wires into the resource-server during context startup.
     * It must be mocked in @WebMvcTest so the context boots without a real
     * UserRepository / DataSource.
     */
    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void stubUserService() {
        // Defensive stub: if @PreAuthorize does not block (e.g. missing @EnableMethodSecurity
        // in the slice context), findAll returns an empty page instead of null, so the
        // assertion on status still fails clearly rather than throwing NPE.
        when(userService.findAll(any(Pageable.class))).thenReturn(Page.empty());
    }

    // -------------------------------------------------------------------------
    // Happy path — active ADMIN
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /users — ADMIN ativo")
    class ActiveAdmin {

        @Test
        @DisplayName("ADMIN ativo com token válido acessa GET /users (200)")
        void activeAdminCanAccessUserList() throws Exception {
            // findAll stubbed to Page.empty() in @BeforeEach — enough for a 200 response
            mockMvc.perform(get("/users")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN"))))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // Downgraded admin — type changed to USER in DB, still ACTIVE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /users — admin rebaixado para USER")
    class DemotedAdmin {

        @Test
        @DisplayName("admin rebaixado (type=USER no banco) recebe 403 em GET /users")
        void demotedAdminReceives403() throws Exception {
            // Converter would return authority USER for this token; inject directly.
            mockMvc.perform(get("/users")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Unauthenticated — no token
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /users — sem token")
    class Unauthenticated {

        @Test
        @DisplayName("requisição sem token retorna 401")
        void unauthenticatedRequestReceives401() throws Exception {
            mockMvc.perform(get("/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // Regular USER — regression guard
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /users — usuário comum ativo")
    class RegularUser {

        @Test
        @DisplayName("usuário comum (USER) não acessa GET /users (403)")
        void regularUserCannotAccessAdminEndpoint() throws Exception {
            mockMvc.perform(get("/users")
                            .with(jwt().authorities(new SimpleGrantedAuthority("USER"))))
                    .andExpect(status().isForbidden());
        }
    }
}
