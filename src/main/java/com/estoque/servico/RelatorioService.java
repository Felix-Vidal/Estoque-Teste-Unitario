package com.estoque.servico;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.dominio.Produto;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.ProdutoRepository;

@Service
public class RelatorioService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public RelatorioService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * RN18: produtos ativos abaixo do mínimo, do que mais falta para o que menos falta.
     * Em caso de empate na quantidade faltante, ordena pelo nome.
     *
     * @param categoriaId filtro opcional; se informado, a categoria precisa existir
     */
    @Transactional(readOnly = true)
    public List<Produto> reposicao(Long categoriaId) {
        if (categoriaId != null && !categoriaRepository.existsById(categoriaId)) {
            throw new RecursoNaoEncontradoException("Categoria " + categoriaId + " não encontrada");
        }
        return produtoRepository.buscarAtivosAbaixoDoMinimo().stream()
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .sorted(Comparator.comparingInt(Produto::quantidadeFaltante).reversed()
                        .thenComparing(Produto::getNome))
                .toList();
    }
}
