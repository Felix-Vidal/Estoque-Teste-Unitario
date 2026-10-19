package com.estoque.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.dominio.Produto;

import jakarta.persistence.LockModeType;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    boolean existsByEan(String ean);

    boolean existsByCategoriaId(Long categoriaId);

    /**
     * Busca o produto travando a linha no banco até o fim da transação (RN14).
     * Assim, duas saídas simultâneas do mesmo produto são processadas uma de cada vez,
     * e a segunda já enxerga o saldo atualizado pela primeira.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarParaMovimentar(@Param("id") Long id);

    @Query("select p from Produto p where p.ativo = true and p.saldo < p.estoqueMinimo")
    List<Produto> buscarAtivosAbaixoDoMinimo();
}
