package com.estoque.servico;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.dominio.Ean13;
import com.estoque.dominio.Produto;

@SpringBootTest
@Transactional
@DirtiesContext
class RelatorioServiceIntegracaoTest {

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private MovimentacaoService movimentacaoService;

    private Long bebidasId;
    private Long limpezaId;
    private int sequenciaEan;

    @BeforeEach
    void configurar() {
        bebidasId = categoriaService.criar("Bebidas").getId();
        limpezaId = categoriaService.criar("Limpeza").getId();
    }

    private Produto cadastrar(String nome, int saldo, int estoqueMinimo, Long categoriaId) {
        String ean = Ean13.gerar(String.format("789%09d", ++sequenciaEan));
        Produto produto = produtoService.cadastrar(ean, nome, new BigDecimal("10.00"), estoqueMinimo, categoriaId);
        if (saldo > 0) {
            movimentacaoService.registrarEntrada(produto.getId(), saldo);
        }
        return produto;
    }

    private List<String> nomesNoRelatorio(Long categoriaId) {
        return relatorioService.reposicao(categoriaId).stream().map(Produto::getNome).toList();
    }

    //RN17
    @Test
    void deveIncluirProdutoUmAbaixoDoMinimoEExcluirProdutoNoMinimo() {
        Produto umAbaixo = cadastrar("Suco", 9, 10, bebidasId);
        Produto noMinimo = cadastrar("Água", 10, 10, bebidasId);

        assertThat(nomesNoRelatorio(null)).containsExactly("Suco");

        // A entidade e a consulta precisam dar a mesma resposta
        assertThat(umAbaixo.estaAbaixoDoMinimo()).isTrue();
        assertThat(noMinimo.estaAbaixoDoMinimo()).isFalse();
    }

    //RN17
    @Test
    void deveIncluirProdutoQueFicouAbaixoDoMinimoDepoisDeUmaSaida() {
        Produto suco = cadastrar("Suco", 10, 10, bebidasId);
        assertThat(nomesNoRelatorio(null)).isEmpty();

        movimentacaoService.registrarSaida(suco.getId(), 1);

        assertThat(nomesNoRelatorio(null)).containsExactly("Suco");
    }

    //RN17
    @Test
    void deveIncluirProdutoQueFicouAbaixoDoMinimoPorAlteracaoDoMinimo() {
        Produto suco = cadastrar("Suco", 10, 10, bebidasId);

        produtoService.alterar(suco.getId(), null, "Suco", new BigDecimal("10.00"), 11, bebidasId);

        assertThat(nomesNoRelatorio(null)).containsExactly("Suco");
    }

    //RN18
    @Test
    void naoDeveIncluirProdutoInativoMesmoAbaixoDoMinimo() {
        Produto inativo = cadastrar("Suco", 0, 10, bebidasId);
        cadastrar("Água", 0, 5, bebidasId);

        produtoService.inativar(inativo.getId());

        assertThat(nomesNoRelatorio(null)).containsExactly("Água");
    }

    //RN18
    @Test
    void deveFiltrarPorCategoriaEOrdenarPelaQuantidadeFaltante() {
        cadastrar("Água", 8, 10, bebidasId);        // faltam 2
        cadastrar("Detergente", 0, 50, limpezaId);  // faltam 50, mas é de outra categoria
        cadastrar("Refrigerante", 2, 10, bebidasId); // faltam 8
        cadastrar("Suco", 20, 10, bebidasId);       // acima do mínimo

        assertThat(nomesNoRelatorio(bebidasId)).containsExactly("Refrigerante", "Água");
        assertThat(nomesNoRelatorio(null)).containsExactly("Detergente", "Refrigerante", "Água");
    }
}