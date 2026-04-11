package com.tukan.api.dto;

import com.tukan.api.entity.Perfil;
import com.tukan.api.entity.Triagem;
import com.tukan.api.service.MeService.DadosUsuarioAutenticado;

import java.time.LocalDate;
import java.time.Period;

public record DashboardResponse(
        String nome,
        ResumoPerfilResponse perfil,
        ResumoTriagemResponse triagem,
        OnboardingStatus onboarding
) {

    public static DashboardResponse from(DadosUsuarioAutenticado dados, OnboardingStatus onboarding) {
        return new DashboardResponse(
                dados.getUser().getNome(),
                dados.getPerfil() != null ? ResumoPerfilResponse.from(dados.getPerfil()) : null,
                dados.getTriagem() != null ? ResumoTriagemResponse.from(dados.getTriagem()) : null,
                onboarding
        );
    }

    public record ResumoPerfilResponse(
            Double pesoKg,
            Double alturaCm,
            Double imc,
            String classificacaoImc,
            Perfil.NivelAtividade nivelAtividade,
            Perfil.Sexo sexo,
            Integer idade
    ) {

        public static ResumoPerfilResponse from(Perfil perfil) {
            double imc = calcularImc(perfil.getPesoKg(), perfil.getAlturaCm());
            int idade = Period.between(perfil.getDataNascimento(), LocalDate.now()).getYears();

            return new ResumoPerfilResponse(
                    perfil.getPesoKg(),
                    perfil.getAlturaCm(),
                    Math.round(imc * 100.0) / 100.0,
                    classificar(imc),
                    perfil.getNivelAtividade(),
                    perfil.getSexo(),
                    idade
            );
        }

        private static double calcularImc(double pesoKg, double alturaCm) {
            double alturaM = alturaCm / 100.0;
            return pesoKg / (alturaM * alturaM);
        }

        private static String classificar(double imc) {
            if (imc < 18.5) return "Abaixo do peso";
            if (imc < 25.0) return "Peso normal";
            if (imc < 30.0) return "Sobrepeso";
            if (imc < 35.0) return "Obesidade grau I";
            if (imc < 40.0) return "Obesidade grau II";
            return "Obesidade grau III";
        }
    }

    public record ResumoTriagemResponse(
            Triagem.ObjetivoNutricional objetivo,
            boolean possuiRestricoes,
            boolean possuiAlergias,
            boolean possuiCondicoesSaude
    ) {

        public static ResumoTriagemResponse from(Triagem triagem) {
            return new ResumoTriagemResponse(
                    triagem.getObjetivo(),
                    temConteudo(triagem.getRestricoesAlimentares()),
                    temConteudo(triagem.getAlergias()),
                    temConteudo(triagem.getCondicoesSaude())
            );
        }

        private static boolean temConteudo(String valor) {
            return valor != null && !valor.isBlank();
        }
    }
}
