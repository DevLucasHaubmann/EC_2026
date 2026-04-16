package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @JsonProperty("name")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String name,

        @Email(message = "E-mail inválido.")
        String email,

        @JsonProperty("type")
        User.UserType type,

        User.UserState status
) {
}
