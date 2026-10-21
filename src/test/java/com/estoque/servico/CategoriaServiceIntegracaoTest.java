package com.estoque.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.CategoriaRepository;

@SpringBootTest
@Transactional
@DirtiesContext
class CategoriaServiceIntegracaoTest {

    @Autowired
    private CategoriaService service;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    //RN01
    @Test
    void deveRecusarNomeQueSoDifereEmMaiusculasEMinusculas() {
        service.criar("Bebidas");

        RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                () -> service.criar("BEBIDAS"));

        assertThat(excecao.getCodigoRegra()).isEqualTo("RN01");
        assertThat(categoriaRepository.count()).isEqualTo(1);
    }

    //RN01
    @Test
    void deveRecusarNomeRepetidoComEspacosNasPontas() {
        service.criar("Bebidas");
        RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                () -> service.criar("  bebidas  "));

        assertThat(excecao.getCodigoRegra()).isEqualTo("RN01");
    }

    //RN02
    @Test
    void deveRecusarExclusaoDeCategoriaComProdutoCadastrado() {
        Categoria categoria = service.criar("Bebidas");
        produtoService.cadastrar(Ean13.gerar("789123456789"), "Suco de uva",
                new BigDecimal("8.90"), 10, categoria.getId());

        RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                () -> service.excluir(categoria.getId()));

        assertThat(excecao.getCodigoRegra()).isEqualTo("RN02");
        assertThat(categoriaRepository.existsById(categoria.getId())).isTrue();
    }

    //RN02
    @Test
    void deveExcluirCategoriaSemProdutos() {
        Categoria categoria = service.criar("Bebidas");

        service.excluir(categoria.getId());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscar(categoria.getId()));
    }
}