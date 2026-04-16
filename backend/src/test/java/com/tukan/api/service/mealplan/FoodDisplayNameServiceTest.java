package com.tukan.api.service.mealplan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodDisplayNameServiceTest {

    private FoodDisplayNameService displayNameService;

    @BeforeEach
    void setUp() {
        displayNameService = new FoodDisplayNameService();
    }

    @Nested
    @DisplayName("Tradução direta")
    class TraducaoDireta {

        @Test
        @DisplayName("Traduz nomes conhecidos em inglês")
        void traduzNomesConhecidos() {
            assertThat(displayNameService.generateDisplayName("chicken breast")).isEqualTo("Peito de Frango");
            assertThat(displayNameService.generateDisplayName("brown rice")).isEqualTo("Arroz Integral");
            assertThat(displayNameService.generateDisplayName("sweet potato")).isEqualTo("Batata-Doce");
            assertThat(displayNameService.generateDisplayName("greek yogurt")).isEqualTo("Iogurte Grego");
            assertThat(displayNameService.generateDisplayName("peanut butter")).isEqualTo("Pasta de Amendoim");
        }

        @Test
        @DisplayName("Traduz com sufixo entre parênteses")
        void traduzComSufixo() {
            assertThat(displayNameService.generateDisplayName("chicken breast (grilled)")).isEqualTo("Peito de Frango");
            assertThat(displayNameService.generateDisplayName("brown rice, cooked")).isEqualTo("Arroz Integral");
        }
    }

    @Nested
    @DisplayName("Limpeza de nomes")
    class LimpezaNomes {

        @Test
        @DisplayName("Remove conteúdo entre parênteses")
        void removeParenteses() {
            String result = displayNameService.generateDisplayName("Arroz (polido, tipo 1)");
            assertThat(result).doesNotContain("(").doesNotContain(")");
        }

        @Test
        @DisplayName("Remove conteúdo entre colchetes")
        void removeColchetes() {
            String result = displayNameService.generateDisplayName("Frango [sem pele]");
            assertThat(result).doesNotContain("[").doesNotContain("]");
        }

        @Test
        @DisplayName("Remove NFS e variantes")
        void removeNFS() {
            String result = displayNameService.generateDisplayName("Beans NFS");
            assertThat(result).doesNotContain("NFS");
        }

        @Test
        @DisplayName("Remove NS as to")
        void removeNSasto() {
            String result = displayNameService.generateDisplayName("Milk, NS as to fat content");
            assertThat(result).doesNotContain("NS as to");
        }

        @Test
        @DisplayName("Remove sufixos como raw, cooked, boiled")
        void removeSufixos() {
            assertThat(displayNameService.generateDisplayName("Banana, raw"))
                    .isEqualTo("Banana");
            assertThat(displayNameService.generateDisplayName("Broccoli, steamed"))
                    .isEqualTo("Broccoli");
        }
    }

    @Nested
    @DisplayName("Capitalização")
    class Capitalizacao {

        @Test
        @DisplayName("Capitaliza palavras corretamente")
        void capitalizaPalavras() {
            assertThat(displayNameService.generateDisplayName("arroz integral"))
                    .isEqualTo("Arroz Integral");
        }

        @Test
        @DisplayName("Mantém preposições em minúscula")
        void mantemPreposicoes() {
            assertThat(displayNameService.generateDisplayName("peito de frango"))
                    .isEqualTo("Peito de Frango");
            assertThat(displayNameService.generateDisplayName("azeite de oliva"))
                    .isEqualTo("Azeite de Oliva");
        }
    }

    @Nested
    @DisplayName("Casos extremos")
    class CasosExtremos {

        @Test
        @DisplayName("Null retorna null")
        void nullRetornaNull() {
            assertThat(displayNameService.generateDisplayName(null)).isNull();
        }

        @Test
        @DisplayName("String vazia retorna vazia")
        void vaziaRetornaVazia() {
            assertThat(displayNameService.generateDisplayName("  ")).isEqualTo("  ");
        }

        @Test
        @DisplayName("Nome já limpo não é alterado significativamente")
        void nomeJaLimpoNaoAltera() {
            assertThat(displayNameService.generateDisplayName("Banana")).isEqualTo("Banana");
            assertThat(displayNameService.generateDisplayName("Arroz Integral")).isEqualTo("Arroz Integral");
        }
    }
}
