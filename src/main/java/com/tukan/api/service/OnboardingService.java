package com.tukan.api.service;

import com.tukan.api.dto.OnboardingStatus;
import com.tukan.api.repository.PerfilRepository;
import com.tukan.api.repository.TriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final PerfilRepository perfilRepository;
    private final TriagemRepository triagemRepository;

    public OnboardingStatus verificarOnboarding(Integer usuarioId) {
        boolean temPerfil = perfilRepository.existsByUsuarioId(usuarioId);
        boolean temTriagem = triagemRepository.existsByUsuarioId(usuarioId);

        boolean onboardingRequired = !temPerfil || !temTriagem;
        String nextStep = calcularProximoPasso(temPerfil, temTriagem);

        return new OnboardingStatus(onboardingRequired, nextStep);
    }

    private String calcularProximoPasso(boolean temPerfil, boolean temTriagem) {
        if (!temPerfil) return "/perfil/primeiro-acesso";
        if (!temTriagem) return "/triagem";
        return "/dashboard";
    }
}
