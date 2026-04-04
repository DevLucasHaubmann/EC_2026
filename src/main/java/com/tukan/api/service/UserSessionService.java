package com.tukan.api.service;

import com.tukan.api.entity.User;
import com.tukan.api.entity.UserSession;
import com.tukan.api.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserSessionRepository userSessionRepository;

    @Value("${jwt.refresh-token.expiration:604800}")
    private long refreshTokenExpiration;

    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 indisponível.", e);
        }
    }

    @Transactional
    public UserSession createSession(User usuario, String refreshToken, String dispositivo, String enderecoIp) {
        Instant now = Instant.now();

        UserSession session = new UserSession();
        session.setUsuario(usuario);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setCriadoEm(now);
        session.setExpiraEm(now.plusSeconds(refreshTokenExpiration));
        session.setUltimoUsoEm(now);
        session.setDispositivo(dispositivo);
        session.setEnderecoIp(enderecoIp);

        return userSessionRepository.save(session);
    }

    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        return userSessionRepository.findByRefreshTokenHash(hashToken(refreshToken));
    }

    @Transactional
    public void revokeSession(UserSession session) {
        session.setRevogadoEm(Instant.now());
        userSessionRepository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllSessions(Integer usuarioId) {
        return userSessionRepository.revokeAllByUsuarioId(usuarioId, Instant.now());
    }

    @Transactional
    public void deleteAllByUsuarioId(Integer usuarioId) {
        userSessionRepository.deleteAllByUsuarioId(usuarioId);
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
