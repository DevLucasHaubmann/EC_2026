package com.tukan.api.service;

import com.tukan.api.dto.AuthResponse;
import com.tukan.api.dto.LoginRequest;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.dto.RefreshRequest;
import com.tukan.api.dto.RegisterRequest;
import com.tukan.api.entity.User;
import com.tukan.api.entity.UserSession;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.util.EmailUtils;
import com.tukan.api.repository.UserRepository;
import com.tukan.api.security.JwtService;
import com.tukan.api.security.UserPrincipal;
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


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserSessionService userSessionService;
    private final OnboardingService onboardingService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String device, String ipAddress) {
        String normalizedEmail = EmailUtils.normalize(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("E-mail já cadastrado.");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setType(User.UserType.USER);
        user.setStatus(User.UserState.ACTIVE);

        userRepository.save(user);

        Authentication authentication = toAuthentication(user);
        return buildAuthResponse(authentication, user, device, ipAddress, "Usuário cadastrado com sucesso.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String device, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        EmailUtils.normalize(request.email()),
                        request.password()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.user();

        return buildAuthResponse(authentication, user, device, ipAddress, "Login realizado com sucesso.");
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse refresh(RefreshRequest request, String device, String ipAddress) {
        UserSession session = userSessionService.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token inválido.", HttpStatus.UNAUTHORIZED));

        if (session.isRevoked()) {
            userSessionService.revokeAllSessions(session.getUser().getId());
            throw new BusinessException("Refresh token já utilizado. Todas as sessões foram revogadas por segurança.", HttpStatus.UNAUTHORIZED);
        }

        if (session.isExpired()) {
            throw new BusinessException("Refresh token expirado.", HttpStatus.UNAUTHORIZED);
        }

        User user = session.getUser();

        if (user.getStatus() != User.UserState.ACTIVE) {
            userSessionService.revokeAllSessions(user.getId());
            throw new BusinessException("Usuário não está ativo.", HttpStatus.FORBIDDEN);
        }

        userSessionService.revokeSession(session);

        Authentication authentication = toAuthentication(user);
        return buildAuthResponse(authentication, user, device, ipAddress, "Token renovado com sucesso.");
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
                                           String device, String ipAddress, String message) {
        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = userSessionService.generateRefreshToken();

        userSessionService.createSession(user, refreshToken, device, ipAddress);

        OnboardingStatus onboarding = onboardingService.checkOnboarding(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpiration(),
                message,
                onboarding.onboardingRequired(),
                onboarding.nextStep(),
                user.getType().name()
        );
    }

    private Authentication toAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority(user.getType().name()))
        );
    }

}