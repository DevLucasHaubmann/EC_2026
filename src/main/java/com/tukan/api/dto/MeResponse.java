package com.tukan.api.dto;

import com.tukan.api.entity.User;
import com.tukan.api.service.MeService.DadosUsuarioAutenticado;

public record MeResponse(
        Integer id,
        String nome,
        String email,
        User.UserType tipo,
        User.UserState status,
        PerfilResponse perfil,
        TriagemResponse triagem
) {

    public static MeResponse from(DadosUsuarioAutenticado dados) {
        return new MeResponse(
                dados.user().getId(),
                dados.user().getNome(),
                dados.user().getEmail(),
                dados.user().getTipo(),
                dados.user().getStatus(),
                dados.perfil() != null ? PerfilResponse.from(dados.perfil()) : null,
                dados.triagem() != null ? TriagemResponse.from(dados.triagem()) : null
        );
    }
}