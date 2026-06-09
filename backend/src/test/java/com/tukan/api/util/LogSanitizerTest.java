package com.tukan.api.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void deveRetornarPlaceholderParaNulo() {
        // given / when / then
        assertThat(LogSanitizer.sanitize(null)).isEqualTo("n/a");
    }

    @Test
    void deveRetornarPlaceholderParaTextoEmBranco() {
        // given / when / then
        assertThat(LogSanitizer.sanitize("   \n  ")).isEqualTo("n/a");
    }

    @Test
    void deveColapsarQuebrasDeLinhaEEspacos() {
        // given
        String input = "linha um\nlinha dois\t  fim";

        // when
        String result = LogSanitizer.sanitize(input);

        // then
        assertThat(result).isEqualTo("linha um linha dois fim");
        assertThat(result).doesNotContain("\n", "\t");
    }

    @Test
    void deveTruncarQuandoExcedeOLimite() {
        // given
        String input = "x".repeat(300);

        // when
        String result = LogSanitizer.sanitize(input, 10);

        // then
        assertThat(result).isEqualTo("xxxxxxxxxx...");
        assertThat(result).hasSize(13); // 10 chars + "..."
    }

    @Test
    void naoDeveTruncarQuandoDentroDoLimite() {
        // given / when / then
        assertThat(LogSanitizer.sanitize("curto", 10)).isEqualTo("curto");
    }

    @Test
    void fingerprintNaoDeveExporConteudoESerDeterministico() {
        // given
        String prompt = "conteúdo sensível do prompt";

        // when
        String first = LogSanitizer.fingerprint(prompt);
        String second = LogSanitizer.fingerprint(prompt);

        // then
        assertThat(first)
                .isEqualTo(second)
                .startsWith("len=" + prompt.length())
                .doesNotContain("sensível", "prompt");
    }

    @Test
    void fingerprintDeveTratarNuloEVazio() {
        // given / when / then
        assertThat(LogSanitizer.fingerprint(null)).isEqualTo("len=0");
        assertThat(LogSanitizer.fingerprint("")).isEqualTo("len=0");
    }
}
