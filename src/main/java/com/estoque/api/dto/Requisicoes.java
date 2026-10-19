package com.estoque.api.dto;

import java.math.BigDecimal;

import com.estoque.excecao.RegraNegocioException;
import com.estoque.servico.MovimentacaoService;

/** Corpos das requisições recebidas pela API. */
public final class Requisicoes {

    private Requisicoes() {
    }

    public record CategoriaRequest(String nome) {
    }

    /**
     * Usado no cadastro e na alteração.
     * Na alteração, o EAN é opcional; se vier, precisa ser igual ao atual (RN09).
     * Não existe campo de saldo: se o cliente mandar um, ele é ignorado (RN08).
     */
    public record ProdutoRequest(String ean,
                                 String nome,
                                 BigDecimal preco,
                                 Integer estoqueMinimo,
                                 Long categoriaId) {
    }

    /**
     * A quantidade é recebida como número decimal para que valores como 2.5 sejam
     * recusados pela RN12, em vez de serem truncados silenciosamente para 2.
     */
    public record MovimentacaoRequest(BigDecimal quantidade) {

        public Integer quantidadeInteira() {
            if (quantidade == null) {
                return null;
            }
            try {
                return quantidade.intValueExact();
            } catch (ArithmeticException e) {
                throw new RegraNegocioException("RN12",
                        "A quantidade deve ser um número inteiro entre "
                                + MovimentacaoService.QUANTIDADE_MINIMA + " e " + MovimentacaoService.QUANTIDADE_MAXIMA);
            }
        }
    }
}
