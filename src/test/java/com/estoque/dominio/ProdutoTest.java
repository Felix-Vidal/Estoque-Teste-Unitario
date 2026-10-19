package com.estoque.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Testes unitários da RN17 (alerta de estoque abaixo do mínimo).
class ProdutoTest {

    private static Produto produtoCom(int estoqueMinimo, int saldo) {
        Produto produto = new Produto(Ean13.gerar("789123456789"), "Suco de uva", new BigDecimal("8.90"),
                estoqueMinimo, new Categoria("Bebidas"));
        if (saldo > 0) {
            Movimentacao.entrada(produto, saldo, LocalDateTime.of(2026, 1, 1, 10, 0));
        }
        return produto;
    }

    @ParameterizedTest(name = "mínimo {0}, saldo {1} → abaixo={2}, faltam {3}")
    @CsvSource({
            "10,  0, true,  10",
            "10,  9, true,   1",
            "10, 10, false,  0",
            "10, 11, false,  0",
            " 0,  0, false,  0",
    })
    void deveAlertarSomenteQuandoSaldoForMenorQueOMinimo(int estoqueMinimo, int saldo,
                                                   boolean abaixo, int faltante) {
        Produto produto = produtoCom(estoqueMinimo, saldo);

        assertThat(produto.estaAbaixoDoMinimo()).isEqualTo(abaixo);
        assertThat(produto.quantidadeFaltante()).isEqualTo(faltante);
    }
}
