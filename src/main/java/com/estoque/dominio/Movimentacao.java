package com.estoque.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Registro de uma entrada ou saída de estoque (RN15).
 * Não tem setters: depois de criada, uma movimentação não muda (RN16).
 * As validações de regra de negócio ficam em MovimentacaoService.
 */
@Entity
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, updatable = false)
    private int saldoResultante;

    protected Movimentacao() {
        // exigido pelo JPA
    }

    private Movimentacao(Produto produto, TipoMovimentacao tipo, int quantidade, LocalDateTime dataHora) {
        this.produto = produto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.dataHora = dataHora;
        this.saldoResultante = produto.getSaldo();
    }

    /** Soma a quantidade ao saldo do produto e registra a entrada. */
    public static Movimentacao entrada(Produto produto, int quantidade, LocalDateTime dataHora) {
        produto.adicionarAoSaldo(quantidade);
        return new Movimentacao(produto, TipoMovimentacao.ENTRADA, quantidade, dataHora);
    }

    /** Subtrai a quantidade do saldo do produto e registra a saída. */
    public static Movimentacao saida(Produto produto, int quantidade, LocalDateTime dataHora) {
        produto.retirarDoSaldo(quantidade);
        return new Movimentacao(produto, TipoMovimentacao.SAIDA, quantidade, dataHora);
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getSaldoResultante() {
        return saldoResultante;
    }
}
