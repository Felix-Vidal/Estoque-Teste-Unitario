package com.estoque.servico;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.dominio.Categoria;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.ProdutoRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Categoria criar(String nome) {
        String nomeLimpo = nome == null ? null : nome.strip();

        // RN01: nome obrigatório, 3 a 50 caracteres
        if (nomeLimpo == null || nomeLimpo.length() < 3 || nomeLimpo.length() > 50) {
            throw new RegraNegocioException("RN01", "O nome da categoria deve ter entre 3 e 50 caracteres");
        }
        // RN01: nome único, sem diferenciar maiúsculas e minúsculas
        if (categoriaRepository.existsByNomeIgnoreCase(nomeLimpo)) {
            throw new RegraNegocioException("RN01", "Já existe uma categoria com o nome '" + nomeLimpo + "'");
        }
        return categoriaRepository.save(new Categoria(nomeLimpo));
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrada"));
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscar(id);

        // RN02: categoria com produtos não pode ser excluída
        if (produtoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("RN02", "A categoria possui produtos vinculados e não pode ser excluída");
        }
        categoriaRepository.delete(categoria);
    }
}
