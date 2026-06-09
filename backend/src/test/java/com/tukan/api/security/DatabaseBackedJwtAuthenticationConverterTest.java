package com.tukan.api.security;

import com.tukan.api.entity.User;
import com.tukan.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseBackedJwtAuthenticationConverterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseBackedJwtAuthenticationConverter converter;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("user@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    private User buildUser(User.UserType type, User.UserState status) {
        User user = new User();
        user.setId(1);
        user.setEmail("user@example.com");
        user.setName("Test User");
        user.setType(type);
        user.setStatus(status);
        return user;
    }

    @Nested
    @DisplayName("Active user")
    class ActiveUser {

        @Test
        @DisplayName("ADMIN ativo recebe authority ADMIN")
        void activeAdminReceivesAdminAuthority() {
            // given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(buildUser(User.UserType.ADMIN, User.UserState.ACTIVE)));

            // when
            AbstractAuthenticationToken token = converter.convert(jwt);

            // then
            assertThat(token).isNotNull();
            assertThat(token.getName()).isEqualTo("user@example.com");
            assertThat(token.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ADMIN");
        }

        @Test
        @DisplayName("USER ativo recebe authority USER")
        void activeUserReceivesUserAuthority() {
            // given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(buildUser(User.UserType.USER, User.UserState.ACTIVE)));

            // when
            AbstractAuthenticationToken token = converter.convert(jwt);

            // then
            assertThat(token).isNotNull();
            assertThat(token.getAuthorities())
                    .extracting("authority")
                    .containsExactly("USER");
        }

        @Test
        @DisplayName("admin rebaixado para USER retorna authority USER (não ADMIN)")
        void demotedAdminReceivesCurrentTypeAuthority() {
            // given — token antigo ainda diz ADMIN, mas banco já tem type=USER
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(buildUser(User.UserType.USER, User.UserState.ACTIVE)));

            // when
            AbstractAuthenticationToken token = converter.convert(jwt);

            // then — authority vem do banco, não do claim roles do JWT
            assertThat(token.getAuthorities())
                    .extracting("authority")
                    .containsExactly("USER")
                    .doesNotContain("ADMIN");
        }
    }

    @Nested
    @DisplayName("Blocked or banned user")
    class BlockedOrBannedUser {

        @Test
        @DisplayName("usuário BLOQUEADO lança InvalidBearerTokenException")
        void blockedUserThrowsException() {
            // given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(buildUser(User.UserType.USER, User.UserState.BLOCKED)));

            // when / then
            assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(InvalidBearerTokenException.class)
                    .hasMessageContaining("bloqueada");
        }

        @Test
        @DisplayName("usuário BANIDO lança InvalidBearerTokenException")
        void bannedUserThrowsException() {
            // given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(buildUser(User.UserType.USER, User.UserState.BANNED)));

            // when / then
            assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(InvalidBearerTokenException.class)
                    .hasMessageContaining("banida");
        }

        @Test
        @DisplayName("admin BLOQUEADO também lança InvalidBearerTokenException")
        void blockedAdminThrowsException() {
            // given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(buildUser(User.UserType.ADMIN, User.UserState.BLOCKED)));

            // when / then
            assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(InvalidBearerTokenException.class);
        }
    }

    @Nested
    @DisplayName("User not found")
    class UserNotFound {

        @Test
        @DisplayName("usuário inexistente no banco lança InvalidBearerTokenException")
        void unknownSubjectThrowsException() {
            // given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(InvalidBearerTokenException.class);
        }
    }

    @Nested
    @DisplayName("Database failure")
    class DatabaseFailure {

        @Test
        @DisplayName("falha de banco resulta em InvalidBearerTokenException (fail-secure, não autentica)")
        void databaseFailureFailsSecure() {
            // given — o banco está indisponível durante a verificação de autorização
            when(userRepository.findByEmail("user@example.com"))
                    .thenThrow(new DataAccessResourceFailureException("db down"));

            // when / then — nunca autentica quando o estado do usuário não pode ser verificado
            assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(InvalidBearerTokenException.class);
        }
    }
}
