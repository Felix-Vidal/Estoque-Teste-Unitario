package com.estoque.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.dominio.TipoMovimentacao;

/** Corpos das respostas devolvidas pela API. */
public final class Respostas {

    private Respostas() {
    }

    public record CategoriaResponse(Long id, String nome) {

        public static CategoriaResponse de(Categoria categoria) {
            return new CategoriaResponse(categoria.getId(), categoria.getNome());
        }
    }

    public record ProdutoResponse(Long id,
                                  String ean,
                                  String nome,
                                  BigDecimal preco,
                                  int estoqueMinimo,
                                  int saldo,
                                  boolean ativo,
                                  boolean abaixoDoMinimo,
                                  CategoriaResponse categoria) {

        public static ProdutoResponse de(Produto p) {
            return new ProdutoResponse(p.getId(), p.getEan(), p.getNome(), p.getPreco(),
                    p.getEstoqueMinimo(), p.getSaldo(), p.isAtivo(), p.estaAbaixoDoMinimo(),
                    CategoriaResponse.de(p.getCategoria()));
        }
    }

    public record MovimentacaoResponse(Long id,
                                       Long produtoId,
                                       TipoMovimentacao tipo,
                                       int quantidade,
                                       LocalDateTime dataHora,
                                       int saldoResultante) {

        public static MovimentacaoResponse de(Movimentacao m) {
            return new MovimentacaoResponse(m.getId(), m.getProduto().getId(), m.getTipo(),
                    m.getQuantidade(), m.getDataHora(), m.getSaldoResultante());
        }
    }

    public record ItemReposicaoResponse(Long produtoId,
                                        String ean,
                                        String nome,
                                        String categoria,
                                        int saldo,
                                        int estoqueMinimo,
                                        int quantidadeFaltante) {

        public static ItemReposicaoResponse de(Produto p) {
            return new ItemReposicaoResponse(p.getId(), p.getEan(), p.getNome(), p.getCategoria().getNome(),
                    p.getSaldo(), p.getEstoqueMinimo(), p.quantidadeFaltante());
        }
    }

    /** Formato padrão de erro. "codigo" traz a regra violada (ex.: "RN13") ou o tipo de erro. */
    public record ErroResponse(String codigo, String mensagem) {
    }
}
