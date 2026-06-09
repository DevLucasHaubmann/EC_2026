package com.tukan.api.service.ai;

import com.tukan.api.domain.feedback.FeedbackAiContext;
import com.tukan.api.domain.feedback.FeedbackAvoidanceSignal;
import com.tukan.api.domain.feedback.FeedbackPreferenceSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackAiContextFormatterTest {

    private FeedbackAiContextFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new FeedbackAiContextFormatter();
    }

    // ------------------------------------------------------------------
    // Contexto vazio
    // ------------------------------------------------------------------

    @Nested
    class ContextoVazio {

        @Test
        void contextoNuloRetornaStringVazia() {
            assertThat(formatter.format(null)).isEmpty();
        }

        @Test
        void contextoPuroVazioRetornaStringVazia() {
            assertThat(formatter.format(FeedbackAiContext.empty())).isEmpty();
        }

        @Test
        void listasVaziasRetornaStringVazia() {
            FeedbackAiContext ctx = new FeedbackAiContext(List.of(), List.of());
            assertThat(formatter.format(ctx)).isEmpty();
        }

        @Test
        void listasNulasRetornaStringVazia() {
            FeedbackAiContext ctx = new FeedbackAiContext(null, null);
            assertThat(formatter.format(ctx)).isEmpty();
        }
    }

    // ------------------------------------------------------------------
    // Preferências positivas
    // ------------------------------------------------------------------

    @Nested
    class PreferenciasPositivas {

        @Test
        void preferenciasGeramOrientacaoPositiva() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("refeição leve no jantar")),
                    List.of());

            String result = formatter.format(ctx);

            assertThat(result)
                    .contains("Preferências observadas do usuário")
                    .contains("refeição leve no jantar");
        }

        @Test
        void multiplasPreferenciasJuntasPorVirgula() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(
                            new FeedbackPreferenceSignal("frango grelhado"),
                            new FeedbackPreferenceSignal("arroz integral")),
                    List.of());

            String result = formatter.format(ctx);

            assertThat(result)
                    .contains("frango grelhado")
                    .contains("arroz integral");
        }

        @Test
        void naoIncluiAvoidancePart() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("proteína no almoço")),
                    List.of());

            String result = formatter.format(ctx);

            assertThat(result).doesNotContain("Evitar");
        }
    }

    // ------------------------------------------------------------------
    // Preferências negativas
    // ------------------------------------------------------------------

    @Nested
    class PreferenciasNegativas {

        @Test
        void preferenciasNegativasGeramRestricao() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(),
                    List.of(new FeedbackAvoidanceSignal("feijão preto")));

            String result = formatter.format(ctx);

            assertThat(result)
                    .contains("Evitar com base em feedback anterior")
                    .contains("feijão preto");
        }

        @Test
        void naoIncluiPreferencePart() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(),
                    List.of(new FeedbackAvoidanceSignal("macarrão")));

            String result = formatter.format(ctx);

            assertThat(result).doesNotContain("Preferências");
        }
    }

    // ------------------------------------------------------------------
    // Sinais mistos
    // ------------------------------------------------------------------

    @Nested
    class SinaisMistos {

        @Test
        void positivoENegativosContemAmbasSecoes() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("salada no almoço")),
                    List.of(new FeedbackAvoidanceSignal("carne vermelha")));

            String result = formatter.format(ctx);

            assertThat(result)
                    .contains("Preferências observadas do usuário")
                    .contains("Evitar com base em feedback anterior");
        }
    }

    // ------------------------------------------------------------------
    // Sanitização
    // ------------------------------------------------------------------

    @Nested
    class Sanitizacao {

        @Test
        void labelLongaETruncada() {
            String labelLonga = "x".repeat(200);
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal(labelLonga)),
                    List.of());

            String result = formatter.format(ctx);

            assertThat(result.length()).isLessThanOrEqualTo(FeedbackAiContextFormatter.MAX_OUTPUT_LENGTH);
        }

        @Test
        void labelComQuebraDeLinhaESanitizada() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("refeição\nleve")),
                    List.of());

            String result = formatter.format(ctx);

            assertThat(result).doesNotContain("\n");
        }

        @Test
        void labelComCharControleESanitizada() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("refeiçãoleve")),
                    List.of());

            String result = formatter.format(ctx);

            assertThat(result).doesNotContain("");
        }

        @Test
        void labelBrancoEIgnorado() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("   ")),
                    List.of());

            assertThat(formatter.format(ctx)).isEmpty();
        }

        @Test
        void labelNuloEIgnorado() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal(null)),
                    List.of());

            assertThat(formatter.format(ctx)).isEmpty();
        }

        @Test
        void saidaTotalLimitadaAMaxOutputLength() {
            String labelMax = "a".repeat(FeedbackAiContextFormatter.MAX_LABEL_LENGTH);
            List<FeedbackPreferenceSignal> signals = IntStream.range(0, 5)
                    .mapToObj(i -> new FeedbackPreferenceSignal(labelMax))
                    .toList();

            FeedbackAiContext ctx = new FeedbackAiContext(signals, List.of());

            String result = formatter.format(ctx);

            assertThat(result.length()).isLessThanOrEqualTo(FeedbackAiContextFormatter.MAX_OUTPUT_LENGTH);
        }

        @Test
        void maisDeMaxSignalsELimitado() {
            List<FeedbackPreferenceSignal> signals = IntStream.range(0, 10)
                    .mapToObj(i -> new FeedbackPreferenceSignal("sinal-" + i))
                    .toList();

            FeedbackAiContext ctx = new FeedbackAiContext(signals, List.of());

            String result = formatter.format(ctx);

            // The 6th distinct label must not appear in the output
            assertThat(result).doesNotContain("sinal-5");
        }
    }

    // ------------------------------------------------------------------
    // Segurança de dados
    // ------------------------------------------------------------------

    @Nested
    class SegurancaDeDados {

        @Test
        void formatadorNaoInjetaDadosBiometricos() {
            FeedbackAiContext ctx = new FeedbackAiContext(
                    List.of(new FeedbackPreferenceSignal("variedade de proteínas")),
                    List.of(new FeedbackAvoidanceSignal("excesso de gordura")));

            String result = formatter.format(ctx);

            assertThat(result.toLowerCase())
                    .doesNotContain("kg")
                    .doesNotContain("peso")
                    .doesNotContain("altura")
                    .doesNotContain("imc");
        }

        @Test
        void formatadorNaoInjetaObservacaoBruta() {
            // FeedbackPreferenceSignal only holds a label — there is no observation field.
            // The formatter output must be derived exclusively from the label.
            String label = "proteína no café da manhã";
            FeedbackPreferenceSignal signal = new FeedbackPreferenceSignal(label);
            FeedbackAiContext ctx = new FeedbackAiContext(List.of(signal), List.of());

            String result = formatter.format(ctx);

            // The record has no observation field, so the only user-supplied text that can
            // appear in the output is the label itself.
            assertThat(result).contains(label);
            assertThat(result).doesNotContain("observation");
        }
    }
}
