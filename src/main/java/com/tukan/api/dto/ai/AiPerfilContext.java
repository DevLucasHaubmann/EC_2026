package com.tukan.api.dto.ai;

public record AiPerfilContext(
        String sexo,
        int idade,
        double pesoKg,
        double alturaCm,
        String nivelAtividade
) {
}
