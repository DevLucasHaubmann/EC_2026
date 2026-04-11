package com.tukan.api.service;

import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.repository.PerfilRepository;
import com.tukan.api.repository.TriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final PerfilRepository perfilRepository;
    private final TriagemRepository triagemRepository;

    @Transactional(readOnly = true)
    public OnboardingStatus checkOnboarding(Integer usuarioId) {
        boolean hasPerfil = perfilRepository.existsByUsuarioId(usuarioId);
        boolean hasTriagem = triagemRepository.existsByUsuarioId(usuarioId);

        boolean onboardingRequired = !hasPerfil || !hasTriagem;
        String nextStep = resolveNextStep(hasPerfil, hasTriagem);

        return new OnboardingStatus(onboardingRequired, nextStep);
    }

    private String resolveNextStep(boolean hasPerfil, boolean hasTriagem) {
        if (!hasPerfil) return "/perfil/primeiro-acesso";
        if (!hasTriagem) return "/triagem";
        return "/dashboard";
    }
}