package com.tukan.api.security;

import com.tukan.api.entity.User;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Replaces the claim-based authority converter.
 * The JWT proves identity; authorization comes from the current database state.
 * One query per authenticated request is acceptable at MVP scale (email is indexed).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseBackedJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getSubject();

        User user = loadUser(email);
        assertNotBlocked(user);

        var authority = new SimpleGrantedAuthority(user.getType().name());
        return new JwtAuthenticationToken(jwt, List.of(authority), email);
    }

    /**
     * Resolves the current user for the token subject. Any database access failure is translated
     * into an authentication failure so the request fails secure (401) — it must never authenticate
     * when the user's current state cannot be verified.
     */
    private User loadUser(String email) {
        try {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidBearerTokenException(
                            "Usuário não encontrado para o token informado."));
        } catch (DataAccessException ex) {
            log.warn("Authorization check failed: unable to load user state from database", ex);
            throw new InvalidBearerTokenException("Não foi possível validar o token no momento.");
        }
    }

    private void assertNotBlocked(User user) {
        if (user.getStatus() == User.UserState.BLOCKED) {
            throw new InvalidBearerTokenException("Conta bloqueada. Acesso negado.");
        }
        if (user.getStatus() == User.UserState.BANNED) {
            throw new InvalidBearerTokenException("Conta banida. Acesso negado.");
        }
    }
}
