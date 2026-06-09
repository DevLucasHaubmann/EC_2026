package com.tukan.api.config;

import com.tukan.api.controller.UserController;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the explicit CORS policy (Sprint 6.3) at the security-filter level.
 *
 * CorsConfig is imported alongside SecurityConfig so that {@code http.cors(...)} resolves
 * the {@code corsConfigurationSource} bean and processes preflight requests in the filter
 * chain. The allowed origin is pinned via @TestPropertySource so assertions are
 * deterministic regardless of the default. The converter and UserService are mocked so the
 * slice boots without a real datasource — preflight short-circuits before the controller.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!",
        "tukan.cors.allowed-origins=http://localhost:5173"
})
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    @DisplayName("preflight da origem local permitida é aceito com os headers de CORS")
    void preflightFromAllowedLocalOriginIsPermitted() throws Exception {
        mockMvc.perform(options("/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
    }

    @Test
    @DisplayName("não usa wildcard de credenciais: Allow-Credentials não é enviado")
    void preflightDoesNotAllowCredentials() throws Exception {
        mockMvc.perform(options("/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("preflight de origem não listada é rejeitado (403)")
    void preflightFromDisallowedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/users")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
