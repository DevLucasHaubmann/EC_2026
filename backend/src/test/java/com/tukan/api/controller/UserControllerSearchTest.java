package com.tukan.api.controller;

import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.security.SecurityConfig;
import com.tukan.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the routing decision of GET /users introduced in Sprint 6:
 * a blank/absent {@code q} preserves the unfiltered listing, a real term hits the
 * search path, and pagination is forwarded unchanged.
 *
 * Authorization is reused from the security slice setup (ADMIN jwt) so the focus
 * here is purely on how the controller delegates to {@link UserService}.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=TestSecretKeyForWebMvcTestOnly32Chars!")
class UserControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void stubServiceReturns() {
        when(userService.findAll(any(Pageable.class))).thenReturn(Page.empty());
        when(userService.search(any(String.class), any(Pageable.class))).thenReturn(Page.empty());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ADMIN"));
    }

    @Test
    @DisplayName("GET /users sem q mantém o listing não filtrado (findAll, nunca search)")
    void withoutQueryUsesUnfilteredListing() throws Exception {
        mockMvc.perform(get("/users").with(admin()))
                .andExpect(status().isOk());

        verify(userService).findAll(any(Pageable.class));
        verify(userService, never()).search(any(), any());
    }

    @Test
    @DisplayName("GET /users?q= (em branco) mantém o listing não filtrado (findAll, nunca search)")
    void blankQueryUsesUnfilteredListing() throws Exception {
        mockMvc.perform(get("/users").param("q", "   ").with(admin()))
                .andExpect(status().isOk());

        verify(userService).findAll(any(Pageable.class));
        verify(userService, never()).search(any(), any());
    }

    @Test
    @DisplayName("GET /users?q=termo aciona a busca por nome/email (search, nunca findAll)")
    void realTermTriggersSearch() throws Exception {
        mockMvc.perform(get("/users").param("q", "ali").with(admin()))
                .andExpect(status().isOk());

        verify(userService).search(eq("ali"), any(Pageable.class));
        verify(userService, never()).findAll(any());
    }

    @Test
    @DisplayName("GET /users?q=termo preserva a paginação solicitada")
    void searchPreservesPagination() throws Exception {
        mockMvc.perform(get("/users")
                        .param("q", "ali")
                        .param("page", "2")
                        .param("size", "5")
                        .with(admin()))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).search(eq("ali"), pageableCaptor.capture());

        Pageable forwarded = pageableCaptor.getValue();
        assertThat(forwarded.getPageNumber()).isEqualTo(2);
        assertThat(forwarded.getPageSize()).isEqualTo(5);
    }
}
