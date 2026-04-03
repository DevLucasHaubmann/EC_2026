package com.tukan.api.dto;

import com.tukan.api.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @Email(message = "E-mail inválido.")
        String email,

        User.UserType tipo,

        User.UserState status
) {
}
