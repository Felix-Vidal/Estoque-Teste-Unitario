package com.estoque.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.dominio.TipoMovimentacao;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.MovimentacaoRepository;
import com.estoque.repositorio.ProdutoRepository;

import jakarta.persistence.LockModeType;

/** Testes unitários do MovimentacaoService (RN11, RN12, RN13 e RN14), com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
class MovimentacaoServiceTest {

    private static final Long ID = 1L;
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    private MovimentacaoService service;
    private Produto produto;

    @BeforeEach
    void configurar() {
        service = new MovimentacaoService(produtoRepository, movimentacaoRepository, RELOGIO);
        produto = new Produto(Ean13.gerar("789123456789"), "Suco de uva", new BigDecimal("8.90"), 10,
                new Categoria("Bebidas"));
    }

    private void produtoExiste() {
        when(produtoRepository.buscarParaMovimentar(ID)).thenReturn(Optional.of(produto));
    }

    private void salvarDevolveOArgumento() {
        when(movimentacaoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private void definirSaldo(int saldo) {
        if (saldo > 0) {
            Movimentacao.entrada(produto, saldo, AGORA);
        }
    }

    @Nested
    class RN11ProdutoInativo {

        @BeforeEach
        void inativar() {
            produto.inativar();
        }

        @Test
        void deveRecusarEntradaEmProdutoInativo() {
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarEntrada(ID, 5));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN11");
            verify(movimentacaoRepository, never()).save(any());
        }

        @Test
        void deveRecusarSaidaEmProdutoInativo() {
            definirSaldo(20);
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarSaida(ID, 5));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN11");
            verify(movimentacaoRepository, never()).save(any());
            assertThat(produto.getSaldo()).isEqualTo(20);
        }

        @Test
        void deveVerificarRN11AntesDaQuantidade() {
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarEntrada(ID, 0));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN11");
            verify(movimentacaoRepository, never()).save(any());
        }

        @Test
        void deveVerificarProdutoInexistenteAntesDaRN11() {
            when(produtoRepository.buscarParaMovimentar(anyLong())).thenReturn(Optional.empty());

            assertThrows(RecursoNaoEncontradoException.class, () -> service.registrarEntrada(99L, 5));

            verify(movimentacaoRepository, never()).save(any());
        }
    }

    @Nested
    class RN12Quantidade {

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, 10_001})
        void deveRecusarEntradaComQuantidadeForaDaFaixa(Integer quantidade) {
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarEntrada(ID, quantidade));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN12");
            verify(movimentacaoRepository, never()).save(any());
            assertThat(produto.getSaldo()).isZero();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, 10_001})
        void deveRecusarSaidaComQuantidadeForaDaFaixa(Integer quantidade) {
            definirSaldo(10_000);
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarSaida(ID, quantidade));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN12");
            verify(movimentacaoRepository, never()).save(any());
            assertThat(produto.getSaldo()).isEqualTo(10_000);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 10_000})
        void deveAceitarEntradaNosLimitesDaFaixa(int quantidade) {
            produtoExiste();
            salvarDevolveOArgumento();

            Movimentacao entrada = service.registrarEntrada(ID, quantidade);

            assertThat(entrada.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
            assertThat(entrada.getQuantidade()).isEqualTo(quantidade);
            assertThat(entrada.getSaldoResultante()).isEqualTo(quantidade);
            assertThat(entrada.getDataHora()).isEqualTo(AGORA);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 10_000})
        void deveAceitarSaidaNosLimitesDaFaixa(int quantidade) {
            definirSaldo(10_000);
            produtoExiste();
            salvarDevolveOArgumento();

            Movimentacao saida = service.registrarSaida(ID, quantidade);

            assertThat(saida.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
            assertThat(saida.getQuantidade()).isEqualTo(quantidade);
            assertThat(saida.getSaldoResultante()).isEqualTo(10_000 - quantidade);
        }
    }

    @Nested
    class RN13SaidaMaiorQueSaldo {

        @Test
        void deveRecusarSaidaUmAcimaDoSaldo() {
            definirSaldo(20);
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarSaida(ID, 21));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN13");
            verify(movimentacaoRepository, never()).save(any());
            assertThat(produto.getSaldo()).isEqualTo(20);
        }

        @Test
        void deveAceitarSaidaExatamenteIgualAoSaldo() {
            definirSaldo(20);
            produtoExiste();
            salvarDevolveOArgumento();

            Movimentacao saida = service.registrarSaida(ID, 20);

            assertThat(saida.getSaldoResultante()).isZero();
            assertThat(produto.getSaldo()).isZero();
        }

        @Test
        void deveAceitarSaidaUmAbaixoDoSaldo() {
            definirSaldo(20);
            produtoExiste();
            salvarDevolveOArgumento();

            Movimentacao saida = service.registrarSaida(ID, 19);

            assertThat(saida.getSaldoResultante()).isEqualTo(1);
        }

        @Test
        void deveRecusarSaidaComSaldoZero() {
            produtoExiste();

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarSaida(ID, 1));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN13");
            verify(movimentacaoRepository, never()).save(any());
            assertThat(produto.getSaldo()).isZero();
        }
    }

    /**
     * A trava real só existe no banco; aqui verificamos que a saída usa a consulta com trava
     * e que, processadas uma após a outra, duas saídas concorrentes nunca deixam o saldo negativo.
     */
    @Nested
    class RN14SaidasConcorrentes {

        @Test
        void deveTravarALinhaDoProdutoNaConsultaDaSaida() throws NoSuchMethodException {
            Lock trava = ProdutoRepository.class.getMethod("buscarParaMovimentar", Long.class)
                    .getAnnotation(Lock.class);

            assertThat(trava).isNotNull();
            assertThat(trava.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        }

        @Test
        void deveBuscarOProdutoPelaConsultaComTravaNaSaida() {
            definirSaldo(10);
            produtoExiste();
            salvarDevolveOArgumento();

            service.registrarSaida(ID, 5);

            verify(produtoRepository).buscarParaMovimentar(ID);
            verify(produtoRepository, never()).findById(any());
        }

        @Test
        void deveRecusarSegundaSaidaQueCompetePeloMesmoSaldo() {
            definirSaldo(10);
            produtoExiste();
            salvarDevolveOArgumento();

            Movimentacao primeira = service.registrarSaida(ID, 7);

            assertThat(primeira.getSaldoResultante()).isEqualTo(3);
            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.registrarSaida(ID, 7));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN13");
            assertThat(produto.getSaldo()).isEqualTo(3);
        }
    }
}
