package com.estoque.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProdutoTest {

    private static final Categoria BEBIDAS = new Categoria("Bebidas");

    /** Cria um produto e coloca o saldo desejado (o teste está no mesmo pacote, então acessa adicionarAoSaldo). */
    private Produto produtoCom(int saldo, int estoqueMinimo) {
        Produto produto = new Produto(Ean13.gerar("789123456789"), "Suco de uva",
                new BigDecimal("8.90"), estoqueMinimo, BEBIDAS);
        produto.adicionarAoSaldo(saldo);
        return produto;
    }

    //RN17
    @ParameterizedTest(name = "saldo {0}, mínimo {1} -> abaixo do mínimo: {2}")
    @CsvSource({
            "9,  10, true",   // um abaixo do mínimo
            "10, 10, false",  // exatamente no mínimo: não gera alerta
            "11, 10, false",  // um acima do mínimo
            "0,  1,  true",   // zerado com mínimo 1
            "0,  0,  false",  // zerado com mínimo 0: nunca fica abaixo
    })
    void deveIdentificarProdutoAbaixoDoMinimo(int saldo, int estoqueMinimo, boolean esperado) {
        Produto produto = produtoCom(saldo, estoqueMinimo);

        assertThat(produto.estaAbaixoDoMinimo()).isEqualTo(esperado);
    }

    //RN17
    @Test
    void produtoRecemCadastradoComMinimoMaiorQueZeroJaEstaAbaixoDoMinimo() {
        // RN08: todo produto nasce com saldo zero
        Produto produto = produtoCom(0, 10);

        assertThat(produto.estaAbaixoDoMinimo()).isTrue();
    }

    //RN17
    @Test
    void deveFicarAbaixoDoMinimoQuandoOMinimoSobeSemMovimentacao() {
        Produto produto = produtoCom(10, 10);
        assertThat(produto.estaAbaixoDoMinimo()).isFalse();

        produto.atualizarDados("Suco de uva", new BigDecimal("8.90"), 11, BEBIDAS);

        assertThat(produto.estaAbaixoDoMinimo()).isTrue();
    }

    //RN17
    @ParameterizedTest(name = "saldo {0}, mínimo {1} -> faltam {2}")
    @CsvSource({
            "7,  10, 3",
            "9,  10, 1",
            "10, 10, 0",   // no mínimo: não falta nada
            "15, 10, 0",   // acima do mínimo: zero, nunca negativo
            "0,  10, 10",
    })
    void deveCalcularQuantidadeFaltante(int saldo, int estoqueMinimo, int esperado) {
        Produto produto = produtoCom(saldo, estoqueMinimo);

        assertThat(produto.quantidadeFaltante()).isEqualTo(esperado);
    }
}