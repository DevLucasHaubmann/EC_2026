package com.tukan.api.service;

import com.tukan.api.dto.AuthResponse;
import com.tukan.api.dto.LoginRequest;
import com.tukan.api.dto.RefreshRequest;
import com.tukan.api.dto.RegisterRequest;
import com.tukan.api.entity.User;
import com.tukan.api.entity.UserSession;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.UserRepository;
import com.tukan.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String dispositivo, String enderecoIp) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("E-mail já cadastrado.");
        }

        User user = new User();
        user.setNome(request.nome().trim());
        user.setEmail(normalizedEmail);
        user.setSenha(passwordEncoder.encode(request.senha()));
        user.setTipo(User.UserType.USER);
        user.setStatus(User.UserState.ATIVO);

        userRepository.save(user);

        Authentication authentication = toAuthentication(user);
        return buildAuthResponse(authentication, user, dispositivo, enderecoIp, "Usuário cadastrado com sucesso.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String dispositivo, String enderecoIp) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.email()),
                        request.senha()
                )
        );

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));

        return buildAuthResponse(authentication, user, dispositivo, enderecoIp, "Login realizado com sucesso.");
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request, String dispositivo, String enderecoIp) {
        UserSession session = userSessionService.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token inválido.", HttpStatus.UNAUTHORIZED));

        if (session.isRevoked()) {
            userSessionService.revokeAllSessions(session.getUsuario().getId());
            throw new BusinessException("Refresh token já utilizado. Todas as sessões foram revogadas por segurança.", HttpStatus.UNAUTHORIZED);
        }

        if (session.isExpired()) {
            throw new BusinessException("Refresh token expirado.", HttpStatus.UNAUTHORIZED);
        }

        User user = session.getUsuario();

        if (user.getStatus() != User.UserState.ATIVO) {
            userSessionService.revokeAllSessions(user.getId());
            throw new BusinessException("Usuário não está ativo.", HttpStatus.FORBIDDEN);
        }

        userSessionService.revokeSession(session);

        Authentication authentication = toAuthentication(user);
        return buildAuthResponse(authentication, user, dispositivo, enderecoIp, "Token renovado com sucesso.");
    }

    @Transactional
    public void logout(RefreshRequest request) {
        UserSession session = userSessionService.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token inválido.", HttpStatus.UNAUTHORIZED));

        if (!session.isRevoked()) {
            userSessionService.revokeSession(session);
        }
    }

    private AuthResponse buildAuthResponse(Authentication authentication, User user,
                                           String dispositivo, String enderecoIp, String message) {
        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = userSessionService.generateRefreshToken();

        userSessionService.createSession(user, refreshToken, dispositivo, enderecoIp);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpiration(),
                message
        );
    }

    private Authentication toAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority(user.getTipo().name()))
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
