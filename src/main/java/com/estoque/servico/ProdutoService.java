package com.estoque.servico;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.dominio.Produto;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.MovimentacaoRepository;
import com.estoque.repositorio.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public ProdutoService(ProdutoRepository produtoRepository,
                          CategoriaRepository categoriaRepository,
                          MovimentacaoRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    /**
     * Cadastra um produto. As regras são verificadas na ordem RN03 → RN04 → RN05 → RN06 → RN07,
     * e a primeira violada interrompe o cadastro.
     * O saldo não é recebido: todo produto começa com saldo zero (RN08).
     */
    @Transactional
    public Produto cadastrar(String ean, String nome, BigDecimal preco, Integer estoqueMinimo, Long categoriaId) {
        // RN03: EAN-13 obrigatório, válido e único
        if (!Ean13.ehValido(ean)) {
            throw new RegraNegocioException("RN03", "O código EAN-13 é obrigatório e deve ser válido");
        }
        if (produtoRepository.existsByEan(ean)) {
            throw new RegraNegocioException("RN03", "Já existe um produto com o EAN " + ean);
        }
        String nomeLimpo = validarNome(nome);
        validarPreco(preco);
        validarEstoqueMinimo(estoqueMinimo);
        Categoria categoria = validarCategoria(categoriaId);

        Produto produto = new Produto(ean, nomeLimpo, preco, estoqueMinimo, categoria);
        return produtoRepository.save(produto);
    }

    /**
     * Altera nome, preço, estoque mínimo e categoria (RN09).
     * O EAN é opcional na requisição; se vier diferente do atual, a alteração é recusada.
     */
    @Transactional
    public Produto alterar(Long id, String ean, String nome, BigDecimal preco, Integer estoqueMinimo, Long categoriaId) {
        Produto produto = buscar(id);

        // RN09: o código EAN é imutável
        if (ean != null && !ean.equals(produto.getEan())) {
            throw new RegraNegocioException("RN09", "O código EAN não pode ser alterado");
        }
        String nomeLimpo = validarNome(nome);
        validarPreco(preco);
        validarEstoqueMinimo(estoqueMinimo);
        Categoria categoria = validarCategoria(categoriaId);

        produto.atualizarDados(nomeLimpo, preco, estoqueMinimo, categoria);
        return produto;
    }

    @Transactional
    public void excluir(Long id) {
        Produto produto = buscar(id);

        // RN10: produto com movimentações não pode ser excluído, apenas inativado
        if (movimentacaoRepository.existsByProdutoId(id)) {
            throw new RegraNegocioException("RN10", "O produto possui movimentações; inative-o em vez de excluir");
        }
        produtoRepository.delete(produto);
    }

    @Transactional
    public Produto inativar(Long id) {
        Produto produto = buscar(id);
        produto.inativar();
        return produto;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado"));
    }

    // RN04: nome obrigatório, 3 a 100 caracteres (espaços nas pontas são removidos)
    private String validarNome(String nome) {
        String nomeLimpo = nome == null ? null : nome.strip();
        if (nomeLimpo == null || nomeLimpo.length() < 3 || nomeLimpo.length() > 100) {
            throw new RegraNegocioException("RN04", "O nome do produto deve ter entre 3 e 100 caracteres");
        }
        return nomeLimpo;
    }

    // RN05: preço maior que zero, com no máximo 2 casas decimais
    private void validarPreco(BigDecimal preco) {
        if (preco == null || preco.signum() <= 0) {
            throw new RegraNegocioException("RN05", "O preço deve ser maior que zero");
        }
        if (preco.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("RN05", "O preço deve ter no máximo 2 casas decimais");
        }
    }

    // RN06: estoque mínimo obrigatório e maior ou igual a zero
    private void validarEstoqueMinimo(Integer estoqueMinimo) {
        if (estoqueMinimo == null || estoqueMinimo < 0) {
            throw new RegraNegocioException("RN06", "O estoque mínimo é obrigatório e não pode ser negativo");
        }
    }

    // RN07: todo produto pertence a uma categoria existente
    private Categoria validarCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new RegraNegocioException("RN07", "A categoria é obrigatória");
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RegraNegocioException("RN07", "A categoria " + categoriaId + " não existe"));
    }
}
