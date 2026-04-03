package com.tukan.api.service;

import com.tukan.api.dto.AuthResponse;
import com.tukan.api.dto.LoginRequest;
import com.tukan.api.dto.RegisterRequest;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.UserRepository;
import com.tukan.api.security.JwtService;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority(user.getTipo().name()))
        );

        String token = jwtService.generateToken(authentication);

        return new AuthResponse(
                token,
                "Bearer",
                "Usuário cadastrado com sucesso."
        );
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.email()),
                        request.senha()
                )
        );

        String token = jwtService.generateToken(authentication);

        return new AuthResponse(
                token,
                "Bearer",
                "Login realizado com sucesso."
        );
    }

    String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}