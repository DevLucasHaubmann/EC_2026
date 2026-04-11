package com.tukan.api.dto;

import com.tukan.api.entity.Perfil;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record CreatePerfilRequest(

        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate dataNascimento,

        @NotNull(message = "O sexo é obrigatório.")
        Perfil.Sexo sexo,

        @NotNull(message = "O peso é obrigatório.")
        @DecimalMin(value = "20.0", message = "O peso mínimo aceito é 20 kg.")
        @DecimalMax(value = "500.0", message = "O peso máximo aceito é 500 kg.")
        Double pesoKg,

        @NotNull(message = "A altura é obrigatória.")
        @DecimalMin(value = "50.0", message = "A altura mínima aceita é 50 cm.")
        @DecimalMax(value = "300.0", message = "A altura máxima aceita é 300 cm.")
        Double alturaCm,

        @NotNull(message = "O nível de atividade é obrigatório.")
        Perfil.NivelAtividade nivelAtividade
) {
}
