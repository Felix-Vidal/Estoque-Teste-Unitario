package com.estoque.servico;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.MovimentacaoRepository;
import com.estoque.repositorio.ProdutoRepository;

/** Testes unitários do ProdutoService, um por regra (RN07 a RN11), com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    private static final Long ID = 1L;
    private static final Long BEBIDAS_ID = 10L;
    private static final Long CATEGORIA_INEXISTENTE_ID = 99L;
    private static final String EAN = Ean13.gerar("789123456789");
    private static final BigDecimal PRECO = new BigDecimal("8.90");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    private ProdutoService service;
    private Categoria bebidas;
    private Produto produto;

    @BeforeEach
    void configurar() {
        service = new ProdutoService(produtoRepository, categoriaRepository, movimentacaoRepository);
        bebidas = new Categoria("Bebidas");
        // Produto já cadastrado, usado nos testes de alteração, exclusão e inativação
        produto = new Produto(EAN, "Suco de uva", PRECO, 10, bebidas);
    }

    private void produtoExiste() {
        when(produtoRepository.findById(ID)).thenReturn(Optional.of(produto));
    }

    //RN07
    @Test
    void deveRecusarCadastroComCategoriaInexistente() {
        when(produtoRepository.existsByEan(EAN)).thenReturn(false);
        when(categoriaRepository.findById(CATEGORIA_INEXISTENTE_ID)).thenReturn(Optional.empty());

        RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar(EAN, "Suco de uva", PRECO, 10, CATEGORIA_INEXISTENTE_ID));

        assertThat(excecao.getCodigoRegra()).isEqualTo("RN07");
        verify(produtoRepository, never()).save(any());
    }

    //RN08
    @Test
    void deveCadastrarProdutoComSaldoZero() {
        when(produtoRepository.existsByEan(EAN)).thenReturn(false);
        when(categoriaRepository.findById(BEBIDAS_ID)).thenReturn(Optional.of(bebidas));
        when(produtoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        Produto cadastrado = service.cadastrar(EAN, "Suco de uva", PRECO, 10, BEBIDAS_ID);

        assertThat(cadastrado.getSaldo()).isZero();
        // O cadastro não gera movimentação: o saldo só muda por entradas e saídas
        verifyNoInteractions(movimentacaoRepository);
    }

    //RN09
    @Test
    void deveRecusarAlteracaoQueTentaMudarOEan() {
        produtoExiste();
        String outroEan = Ean13.gerar("789123456780");

        RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                () -> service.alterar(ID, outroEan, "Suco de laranja", PRECO, 10, BEBIDAS_ID));

        assertThat(excecao.getCodigoRegra()).isEqualTo("RN09");
        // A alteração é recusada por inteiro: nem o EAN nem o nome mudam
        assertThat(produto.getEan()).isEqualTo(EAN);
        assertThat(produto.getNome()).isEqualTo("Suco de uva");
    }

    //RN10
    @Test
    void deveRecusarExclusaoDeProdutoComMovimentacoes() {
        produtoExiste();
        when(movimentacaoRepository.existsByProdutoId(ID)).thenReturn(true);

        RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                () -> service.excluir(ID));

        assertThat(excecao.getCodigoRegra()).isEqualTo("RN10");
        verify(produtoRepository, never()).delete(any());
    }

    //RN11
    @Test
    void produtoInativadoNaoDeveAceitarMovimentacoes() {
        // Com saldo, a saída só pode ser recusada por causa da RN11
        Movimentacao.entrada(produto, 15, LocalDateTime.now(RELOGIO));
        produtoExiste();
        service.inativar(ID);

        // O MovimentacaoService usa os mesmos repositórios simulados
        MovimentacaoService movimentacaoService =
                new MovimentacaoService(produtoRepository, movimentacaoRepository, RELOGIO);
        when(produtoRepository.buscarParaMovimentar(ID)).thenReturn(Optional.of(produto));

        RegraNegocioException entrada = assertThrows(RegraNegocioException.class,
                () -> movimentacaoService.registrarEntrada(ID, 5));
        RegraNegocioException saida = assertThrows(RegraNegocioException.class,
                () -> movimentacaoService.registrarSaida(ID, 5));

        assertThat(entrada.getCodigoRegra()).isEqualTo("RN11");
        assertThat(saida.getCodigoRegra()).isEqualTo("RN11");
        assertThat(produto.getSaldo()).isEqualTo(15);
        verify(movimentacaoRepository, never()).save(any());
    }
}
