package com.estoque.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.estoque.dominio.Movimentacao;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    boolean existsByProdutoId(Long produtoId);

    List<Movimentacao> findByProdutoIdOrderByDataHoraAscIdAsc(Long produtoId);
}
