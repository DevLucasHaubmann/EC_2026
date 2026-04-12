package com.tukan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tukan.api.entity.NutritionalProfile;

import java.time.Instant;
import java.time.LocalDate;

public record PerfilResponse(
        Integer id,

        @JsonProperty("usuarioId")
        Integer userId,

        @JsonProperty("usuarioNome")
        String userName,

        @JsonProperty("dataNascimento")
        LocalDate dateOfBirth,

        @JsonProperty("sexo")
        NutritionalProfile.Gender gender,

        Double pesoKg,
        Double alturaCm,

        @JsonProperty("nivelAtividade")
        NutritionalProfile.ActivityLevel activityLevel,

        @JsonProperty("criadoEm")
        Instant createdAt,

        @JsonProperty("atualizadoEm")
        Instant updatedAt
) {

    public static PerfilResponse from(NutritionalProfile profile) {
        return new PerfilResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getWeightKg(),
                profile.getHeightCm(),
                profile.getActivityLevel(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}