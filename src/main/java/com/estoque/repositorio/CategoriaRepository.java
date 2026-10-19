package com.estoque.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.estoque.dominio.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNomeIgnoreCase(String nome);
}
