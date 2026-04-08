package com.tukan.api.dto;

import com.tukan.api.entity.Triagem;
import jakarta.validation.constraints.Size;

public record AdminUpdateTriagemRequest(

        Triagem.ObjetivoNutricional objetivo,

        @Size(max = 500, message = "Restrições alimentares devem ter no máximo 500 caracteres.")
        String restricoesAlimentares,

        @Size(max = 500, message = "Alergias devem ter no máximo 500 caracteres.")
        String alergias,

        @Size(max = 500, message = "Condições de saúde devem ter no máximo 500 caracteres.")
        String condicoesSaude
) {
}
