package com.tukan.api.dto;

import com.tukan.api.entity.User;

public record MeResponse(
        Integer id,
        String nome,
        String email,
        User.UserType tipo,
        User.UserState status,
        PerfilResponse perfil,
        TriagemResponse triagem
) {

    public static MeResponse from(User user, PerfilResponse perfil, TriagemResponse triagem) {
        return new MeResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getTipo(),
                user.getStatus(),
                perfil,
                triagem
        );
    }
}