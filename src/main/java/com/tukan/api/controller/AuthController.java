package com.tukan.api.controller;

import com.tukan.api.dto.AuthResponse;
import com.tukan.api.dto.LoginRequest;
import com.tukan.api.dto.RefreshRequest;
import com.tukan.api.dto.RegisterRequest;
import com.tukan.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(
                request,
                httpRequest.getHeader("User-Agent"),
                resolveClientIp(httpRequest)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(
                request,
                httpRequest.getHeader("User-Agent"),
                resolveClientIp(httpRequest)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request,
                                                HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(
                request,
                httpRequest.getHeader("User-Agent"),
                resolveClientIp(httpRequest)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
