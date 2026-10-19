package com.estoque.servico;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.MovimentacaoRepository;
import com.estoque.repositorio.ProdutoRepository;

@Service
public class MovimentacaoService {

    public static final int QUANTIDADE_MINIMA = 1;
    public static final int QUANTIDADE_MAXIMA = 10_000;

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final Clock clock;

    public MovimentacaoService(ProdutoRepository produtoRepository,
                               MovimentacaoRepository movimentacaoRepository,
                               Clock clock) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.clock = clock;
    }

    @Transactional
    public Movimentacao registrarEntrada(Long produtoId, Integer quantidade) {
        Produto produto = buscarProdutoParaMovimentar(produtoId);
        validarQuantidade(quantidade);

        Movimentacao entrada = Movimentacao.entrada(produto, quantidade, LocalDateTime.now(clock));
        return movimentacaoRepository.save(entrada);
    }

    @Transactional
    public Movimentacao registrarSaida(Long produtoId, Integer quantidade) {
        Produto produto = buscarProdutoParaMovimentar(produtoId);
        validarQuantidade(quantidade);

        // RN13: a saída não pode ser maior que o saldo (igual é permitido)
        // RN14: como o produto foi lido com trava, esta verificação é segura com saídas simultâneas
        if (quantidade > produto.getSaldo()) {
            throw new RegraNegocioException("RN13",
                    "Saldo insuficiente: saldo atual " + produto.getSaldo() + ", saída solicitada " + quantidade);
        }

        Movimentacao saida = Movimentacao.saida(produto, quantidade, LocalDateTime.now(clock));
        return movimentacaoRepository.save(saida);
    }

    /** Histórico do produto em ordem cronológica. */
    @Transactional(readOnly = true)
    public List<Movimentacao> listarPorProduto(Long produtoId) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new RecursoNaoEncontradoException("Produto " + produtoId + " não encontrado");
        }
        return movimentacaoRepository.findByProdutoIdOrderByDataHoraAscIdAsc(produtoId);
    }

    private Produto buscarProdutoParaMovimentar(Long produtoId) {
        Produto produto = produtoRepository.buscarParaMovimentar(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + produtoId + " não encontrado"));

        // RN11: produto inativo não aceita movimentações
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("RN11", "O produto está inativo e não aceita movimentações");
        }
        return produto;
    }

    // RN12: quantidade inteira entre 1 e 10.000
    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade < QUANTIDADE_MINIMA || quantidade > QUANTIDADE_MAXIMA) {
            throw new RegraNegocioException("RN12",
                    "A quantidade deve ser um número inteiro entre " + QUANTIDADE_MINIMA + " e " + QUANTIDADE_MAXIMA);
        }
    }
}
