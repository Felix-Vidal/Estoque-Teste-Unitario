package com.estoque.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código de barras EAN-13. Único e imutável (RN03, RN09). */
    @Column(nullable = false, unique = true, length = 13, updatable = false)
    private String ean;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private int estoqueMinimo;

    /** Saldo atual. Começa em zero e só muda por movimentações (RN08). */
    @Column(nullable = false)
    private int saldo;

    @Column(nullable = false)
    private boolean ativo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    protected Produto() {
        // exigido pelo JPA
    }

    public Produto(String ean, String nome, BigDecimal preco, int estoqueMinimo, Categoria categoria) {
        this.ean = ean;
        this.nome = nome;
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
        this.categoria = categoria;
        this.saldo = 0;
        this.ativo = true;
    }

    /** RN17: abaixo do mínimo quando o saldo é MENOR que o mínimo (igual não conta). */
    public boolean estaAbaixoDoMinimo() {
        return saldo < estoqueMinimo;
    }

    /** Quanto falta para o saldo atingir o mínimo (zero se não estiver abaixo). */
    public int quantidadeFaltante() {
        return Math.max(0, estoqueMinimo - saldo);
    }

    void adicionarAoSaldo(int quantidade) {
        this.saldo += quantidade;
    }

    void retirarDoSaldo(int quantidade) {
        this.saldo -= quantidade;
    }

    public void atualizarDados(String nome, BigDecimal preco, int estoqueMinimo, Categoria categoria) {
        this.nome = nome;
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
        this.categoria = categoria;
    }

    public void inativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getEan() {
        return ean;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public int getSaldo() {
        return saldo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
