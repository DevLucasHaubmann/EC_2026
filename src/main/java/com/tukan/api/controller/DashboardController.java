package com.tukan.api.controller;

import com.tukan.api.dto.DashboardResponse;
import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.service.MeService;
import com.tukan.api.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MeService meService;
    private final OnboardingService onboardingService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        var dados = meService.getDadosUsuarioAutenticado(authentication.getName());
        OnboardingStatus onboarding = onboardingService.verificarOnboarding(dados.getUser().getId());
        return ResponseEntity.ok(DashboardResponse.from(dados, onboarding));
    }
}