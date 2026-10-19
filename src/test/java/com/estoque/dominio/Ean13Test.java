package com.estoque.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Exemplo de teste unitário puro (sem Spring). */
class Ean13Test {

    @Test
    void aceitaCodigoComDigitoVerificadorCorreto() {
        assertThat(Ean13.ehValido("7891234567895")).isTrue();
    }

    @Test
    void recusaCodigoComDigitoVerificadorErrado() {
        assertThat(Ean13.ehValido("7891234567890")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"789123456789", "78912345678950", "789123456789A", " 7891234567895"})
    void recusaFormatoInvalido(String codigo) {
        assertThat(Ean13.ehValido(codigo)).isFalse();
    }

    @Test
    void geraCodigoValido() {
        assertThat(Ean13.gerar("789123456789")).isEqualTo("7891234567895");
    }
}
