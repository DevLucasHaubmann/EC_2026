package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.User;
import com.tukan.api.service.MeService.AuthenticatedUserData;

public record MeResponse(
        Integer id,

        @JsonProperty("nome")
        String name,

        String email,

        @JsonProperty("tipo")
        User.UserType type,

        User.UserState status,

        @JsonProperty("perfil")
        PerfilResponse profile,

        @JsonProperty("triagem")
        TriagemResponse assessment
) {

    public static MeResponse from(AuthenticatedUserData data) {
        return new MeResponse(
                data.user().getId(),
                data.user().getName(),
                data.user().getEmail(),
                data.user().getType(),
                data.user().getStatus(),
                data.profile() != null ? PerfilResponse.from(data.profile()) : null,
                data.assessment() != null ? TriagemResponse.from(data.assessment()) : null
        );
    }
}
