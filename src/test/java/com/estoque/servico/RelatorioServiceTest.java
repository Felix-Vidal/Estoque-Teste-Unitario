package com.estoque.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final Long BEBIDAS_ID = 1L;
    private static final Long LIMPEZA_ID = 2L;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    private RelatorioService service;
    private Categoria bebidas;
    private Categoria limpeza;
    private int sequenciaEan;

    @BeforeEach
    void configurar() {
        service = new RelatorioService(produtoRepository, categoriaRepository);
        // O id é gerado pelo banco; como aqui não há banco, ele é definido manualmente
        bebidas = new Categoria("Bebidas");
        ReflectionTestUtils.setField(bebidas, "id", BEBIDAS_ID);
        limpeza = new Categoria("Limpeza");
        ReflectionTestUtils.setField(limpeza, "id", LIMPEZA_ID);
    }

    private Produto produto(String nome, int saldo, int estoqueMinimo, Categoria categoria) {
        String ean = Ean13.gerar(String.format("789%09d", ++sequenciaEan));
        Produto produto = new Produto(ean, nome, new BigDecimal("10.00"), estoqueMinimo, categoria);
        if (saldo > 0) {
            Movimentacao.entrada(produto, saldo, LocalDateTime.now());
        }
        return produto;
    }

    private void consultaDevolve(Produto... produtos) {
        when(produtoRepository.buscarAtivosAbaixoDoMinimo()).thenReturn(List.of(produtos));
    }

    //RN18
    @Test
    void deveOrdenarPelaQuantidadeFaltanteDoMaiorParaOMenor() {
        Produto faltam2 = produto("Água", 8, 10, bebidas);
        Produto faltam8 = produto("Refrigerante", 2, 10, bebidas);
        Produto faltam5 = produto("Suco", 5, 10, bebidas);
        consultaDevolve(faltam2, faltam8, faltam5);

        List<Produto> relatorio = service.reposicao(null);

        assertThat(relatorio).containsExactly(faltam8, faltam5, faltam2);
    }

    //RN18
    @Test
    void deveDesempatarPeloNomeQuandoAQuantidadeFaltanteForIgual() {
        Produto suco = produto("Suco", 7, 10, bebidas);
        Produto leite = produto("Leite", 2, 5, bebidas);
        Produto cha = produto("Chá gelado", 0, 3, bebidas);
        consultaDevolve(suco, leite, cha);

        List<Produto> relatorio = service.reposicao(null);

        // Os três têm 3 unidades faltando: a ordem é alfabética
        assertThat(relatorio).extracting(Produto::getNome)
                .containsExactly("Chá gelado", "Leite", "Suco");
    }

    //RN18
    @Test
    void deveListarApenasProdutosDaCategoriaInformada() {
        Produto suco = produto("Suco", 0, 10, bebidas);
        Produto detergente = produto("Detergente", 0, 20, limpeza);
        Produto agua = produto("Água", 3, 10, bebidas);
        when(categoriaRepository.existsById(BEBIDAS_ID)).thenReturn(true);
        consultaDevolve(suco, detergente, agua);

        List<Produto> relatorio = service.reposicao(BEBIDAS_ID);

        assertThat(relatorio).containsExactly(suco, agua);
    }

    //RN18
    @Test
    void deveListarTodasAsCategoriasQuandoNaoHouverFiltro() {
        Produto suco = produto("Suco", 0, 10, bebidas);
        Produto detergente = produto("Detergente", 0, 20, limpeza);
        consultaDevolve(suco, detergente);

        List<Produto> relatorio = service.reposicao(null);

        assertThat(relatorio).containsExactly(detergente, suco);
        verify(categoriaRepository, never()).existsById(anyLong());
    }

    //RN18
    @Test
    void deveDevolverListaVaziaQuandoACategoriaNaoTemProdutosAbaixoDoMinimo() {
        Produto detergente = produto("Detergente", 0, 20, limpeza);
        when(categoriaRepository.existsById(BEBIDAS_ID)).thenReturn(true);
        consultaDevolve(detergente);

        assertThat(service.reposicao(BEBIDAS_ID)).isEmpty();
    }

    //RN18
    @Test
    void deveRecusarFiltroPorCategoriaInexistente() {
        when(categoriaRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> service.reposicao(99L));

        verify(produtoRepository, never()).buscarAtivosAbaixoDoMinimo();
    }

    //RN18
    @Test
    void deveDevolverListaVaziaQuandoNenhumProdutoEstiverAbaixoDoMinimo() {
        consultaDevolve();

        assertThat(service.reposicao(null)).isEmpty();
    }
}