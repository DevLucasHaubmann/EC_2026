package com.tukan.api.dto.ai;

import java.util.List;

public record AiTriagemContext(
        String objetivo,
        List<String> restricoesAlimentares,
        List<String> alergias,
        List<String> condicoesSaude
) {
}
