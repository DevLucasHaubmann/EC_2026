package com.tukan.api.service;

import com.tukan.api.entity.Perfil;
import com.tukan.api.entity.Triagem;
import com.tukan.api.entity.User;
import com.tukan.api.repository.PerfilRepository;
import com.tukan.api.repository.TriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserService userService;
    private final PerfilRepository perfilRepository;
    private final TriagemRepository triagemRepository;

    @Transactional(readOnly = true)
    public DadosUsuarioAutenticado findAuthenticatedUserData(String email) {
        User user = userService.findByEmail(email);
        Perfil perfil = perfilRepository.findByUsuarioId(user.getId()).orElse(null);
        Triagem triagem = triagemRepository.findByUsuarioId(user.getId()).orElse(null);
        return new DadosUsuarioAutenticado(user, perfil, triagem);
    }

    public record DadosUsuarioAutenticado(User user, Perfil perfil, Triagem triagem) {
    }
}