package com.tukan.api.dto;

import com.tukan.api.entity.Perfil;

import java.time.Instant;
import java.time.LocalDate;

public record PerfilResponse(
        Integer id,
        Integer usuarioId,
        String usuarioNome,
        LocalDate dataNascimento,
        Perfil.Sexo sexo,
        Double pesoKg,
        Double alturaCm,
        Perfil.NivelAtividade nivelAtividade,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static PerfilResponse from(Perfil perfil) {
        return new PerfilResponse(
                perfil.getId(),
                perfil.getUsuario().getId(),
                perfil.getUsuario().getNome(),
                perfil.getDataNascimento(),
                perfil.getSexo(),
                perfil.getPesoKg(),
                perfil.getAlturaCm(),
                perfil.getNivelAtividade(),
                perfil.getCriadoEm(),
                perfil.getAtualizadoEm()
        );
    }
}
