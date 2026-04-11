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
                dados.getUser().getId(),
                dados.getUser().getNome(),
                dados.getUser().getEmail(),
                dados.getUser().getTipo(),
                dados.getUser().getStatus(),
                dados.getPerfil() != null ? PerfilResponse.from(dados.getPerfil()) : null,
                dados.getTriagem() != null ? TriagemResponse.from(dados.getTriagem()) : null
        );
    }
}
