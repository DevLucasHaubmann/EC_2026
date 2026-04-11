package com.tukan.api.service;

import com.tukan.api.dto.MeResponse;
import com.tukan.api.dto.PerfilResponse;
import com.tukan.api.dto.TriagemResponse;
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
    public MeResponse getMe(String email) {
        User user = userService.findByEmail(email);

        PerfilResponse perfil = perfilRepository.findByUsuarioId(user.getId())
                .map(PerfilResponse::from)
                .orElse(null);

        TriagemResponse triagem = triagemRepository.findByUsuarioId(user.getId())
                .map(TriagemResponse::from)
                .orElse(null);

        return MeResponse.from(user, perfil, triagem);
    }
}