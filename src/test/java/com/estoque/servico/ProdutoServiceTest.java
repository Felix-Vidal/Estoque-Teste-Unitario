package com.estoque.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.MovimentacaoRepository;
import com.estoque.repositorio.ProdutoRepository;

/** Testes unitários do ProdutoService (RN03 a RN11), com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    private static final Long ID = 1L;
    private static final Long BEBIDAS_ID = 10L;
    private static final Long CATEGORIA_INEXISTENTE_ID = 99L;
    private static final Long CATEGORIA_ID = 1L;
    private static final String EAN = Ean13.gerar("789123456789");
    private static final String EAN_VALIDO = EAN;
    private static final String NOME_VALIDO = "Suco de uva";
    private static final BigDecimal PRECO = new BigDecimal("8.90");
    private static final BigDecimal PRECO_VALIDO = PRECO;
    private static final int ESTOQUE_MINIMO_VALIDO = 10;
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

    private void eanAindaNaoExiste() {
        when(produtoRepository.existsByEan(anyString())).thenReturn(false);
    }

    private void categoriaExiste() {
        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(new Categoria("Bebidas")));
    }

    private void salvarDevolveOArgumento() {
        when(produtoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    // ---------------------------------------------------------------------
    // Regras validadas diretamente nos métodos do ProdutoService (RN07, RN08, RN09, RN10 e RN11)
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // Regras de validação do cadastro de produto (RN03, RN04, RN05 e RN06)
    // ---------------------------------------------------------------------

    @Nested
    class RN03CodigoEan {

        //RN03
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"123", "78912345678950", "789123456789A", "7891234567890"})
        void deveRecusarEanInvalido(String ean) {
            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(ean, NOME_VALIDO, PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN03");
            verify(produtoRepository, never()).save(any());
        }

        //RN03
        @Test
        void deveRecusarEanDuplicado() {
            when(produtoRepository.existsByEan(EAN_VALIDO)).thenReturn(true);

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, NOME_VALIDO, PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN03");
            verify(produtoRepository, never()).save(any());
        }

        //RN03
        @Test
        void deveCadastrarProdutoComEanValidoENaoExistente() {
            eanAindaNaoExiste();
            categoriaExiste();
            salvarDevolveOArgumento();

            service.cadastrar(EAN_VALIDO, NOME_VALIDO, PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID);

            verify(produtoRepository).existsByEan(EAN_VALIDO);
            verify(produtoRepository).save(any(Produto.class));
        }
    }

    @Nested
    class RN04NomeDoProduto {

        //RN04
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "Ab", "  Ab  "})
        void deveRecusarNomeVazioOuComMenosDe3Caracteres(String nome) {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, nome, PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN04");
            verify(produtoRepository, never()).save(any());
        }

        //RN04
        @Test
        void deveRecusarNomeCom101Caracteres() {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, "A".repeat(101), PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN04");
            verify(produtoRepository, never()).save(any());
        }

        //RN04
        @ParameterizedTest
        @ValueSource(ints = {3, 100})
        void deveAceitarNomeNosLimitesDoTamanho(int tamanho) {
            eanAindaNaoExiste();
            categoriaExiste();
            salvarDevolveOArgumento();
            String nome = "A".repeat(tamanho);

            Produto produto = service.cadastrar(EAN_VALIDO, nome, PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID);

            assertThat(produto.getNome()).isEqualTo(nome);
        }

        //RN04
        @Test
        void deveSalvarONomeSemEspacosNasPontas() {
            eanAindaNaoExiste();
            categoriaExiste();
            salvarDevolveOArgumento();

            service.cadastrar(EAN_VALIDO, "  Suco de uva  ", PRECO_VALIDO, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID);

            ArgumentCaptor<Produto> salvo = ArgumentCaptor.forClass(Produto.class);
            verify(produtoRepository).save(salvo.capture());
            assertThat(salvo.getValue().getNome()).isEqualTo("Suco de uva");
        }
    }

    @Nested
    class RN05PrecoDoProduto {

        //RN05
        @Test
        void deveRecusarPrecoNulo() {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, NOME_VALIDO, null, ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN05");
            verify(produtoRepository, never()).save(any());
        }

        //RN05
        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
        void deveRecusarPrecoMenorOuIgualAZero(String preco) {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, NOME_VALIDO, new BigDecimal(preco), ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN05");
            verify(produtoRepository, never()).save(any());
        }

        //RN05
        @Test
        void deveRecusarPrecoComMaisDeDuasCasasDecimais() {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, NOME_VALIDO, new BigDecimal("10.999"), ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN05");
            verify(produtoRepository, never()).save(any());
        }

        //RN05
        @ParameterizedTest
        @ValueSource(strings = {"0.01", "8.90", "10", "10.5", "10.00"})
        void deveAceitarPrecoValido(String preco) {
            eanAindaNaoExiste();
            categoriaExiste();
            salvarDevolveOArgumento();

            Produto produto = service.cadastrar(EAN_VALIDO, NOME_VALIDO, new BigDecimal(preco), ESTOQUE_MINIMO_VALIDO, CATEGORIA_ID);

            assertThat(produto.getPreco()).isEqualByComparingTo(new BigDecimal(preco));
        }
    }

    @Nested
    class RN06EstoqueMinimo {

        //RN06
        @Test
        void deveRecusarEstoqueMinimoNulo() {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, NOME_VALIDO, PRECO_VALIDO, null, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN06");
            verify(produtoRepository, never()).save(any());
        }

        //RN06
        @Test
        void deveRecusarEstoqueMinimoNegativo() {
            eanAindaNaoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(EAN_VALIDO, NOME_VALIDO, PRECO_VALIDO, -1, CATEGORIA_ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN06");
            verify(produtoRepository, never()).save(any());
        }

        //RN06
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 100})
        void deveAceitarEstoqueMinimoMaiorOuIgualAZero(int estoqueMinimo) {
            eanAindaNaoExiste();
            categoriaExiste();
            salvarDevolveOArgumento();

            Produto produto = service.cadastrar(EAN_VALIDO, NOME_VALIDO, PRECO_VALIDO, estoqueMinimo, CATEGORIA_ID);

            assertThat(produto.getEstoqueMinimo()).isEqualTo(estoqueMinimo);
        }
    }
}
