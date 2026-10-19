package com.estoque.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.estoque.dominio.Categoria;
import com.estoque.dominio.Ean13;
import com.estoque.dominio.Movimentacao;
import com.estoque.dominio.Produto;
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.servico.MovimentacaoService;

/** Testes da camada HTTP das movimentações (RN11, RN12 e RN13), com o serviço simulado. */
@WebMvcTest(MovimentacaoController.class)
class MovimentacaoControllerTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MovimentacaoService service;

    private Produto produto;

    @BeforeEach
    void configurar() {
        produto = new Produto(Ean13.gerar("789123456789"), "Suco de uva", new BigDecimal("8.90"), 10,
                new Categoria("Bebidas"));
    }

    private ResultActions enviar(String operacao, String corpo) throws Exception {
        return mvc.perform(post("/produtos/1/" + operacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Nested
    class RN11ProdutoInativo {

        @Test
        void deveResponder422ComRN11AoMovimentarProdutoInativo() throws Exception {
            when(service.registrarEntrada(1L, 5))
                    .thenThrow(new RegraNegocioException("RN11", "O produto está inativo"));

            enviar("entradas", """
                    {"quantidade": 5}
                    """)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("RN11"));
        }

        @Test
        void deveResponder404QuandoProdutoNaoExistir() throws Exception {
            when(service.registrarEntrada(1L, 5))
                    .thenThrow(new RecursoNaoEncontradoException("Produto 1 não encontrado"));

            enviar("entradas", """
                    {"quantidade": 5}
                    """)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        }
    }

    @Nested
    class RN12Quantidade {

        @Test
        void deveRecusarQuantidadeDecimalSemChamarOServico() throws Exception {
            enviar("entradas", """
                    {"quantidade": 2.5}
                    """)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("RN12"));

            verify(service, never()).registrarEntrada(anyLong(), any());
        }

        @Test
        void deveRepassarQuantidadeInteiraAoServico() throws Exception {
            when(service.registrarEntrada(1L, 10_000))
                    .thenReturn(Movimentacao.entrada(produto, 10_000, AGORA));

            enviar("entradas", """
                    {"quantidade": 10000}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                    .andExpect(jsonPath("$.quantidade").value(10_000))
                    .andExpect(jsonPath("$.saldoResultante").value(10_000));

            verify(service).registrarEntrada(1L, 10_000);
        }

        @Test
        void deveResponder422ComRN12QuandoServicoRecusar() throws Exception {
            when(service.registrarEntrada(eq(1L), eq(0)))
                    .thenThrow(new RegraNegocioException("RN12", "Quantidade inválida"));

            enviar("entradas", """
                    {"quantidade": 0}
                    """)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("RN12"));
        }

        @Test
        void deveResponder400ParaQuantidadeNaoNumerica() throws Exception {
            enviar("entradas", """
                    {"quantidade": "abc"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

            verify(service, never()).registrarEntrada(anyLong(), any());
        }
    }

    @Nested
    class RN13SaidaMaiorQueSaldo {

        @Test
        void deveResponder422ComRN13NaSaidaMaiorQueSaldo() throws Exception {
            when(service.registrarSaida(1L, 21))
                    .thenThrow(new RegraNegocioException("RN13", "Saldo insuficiente"));

            enviar("saidas", """
                    {"quantidade": 21}
                    """)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("RN13"));
        }

        @Test
        void deveResponder201NaSaidaIgualAoSaldo() throws Exception {
            Movimentacao.entrada(produto, 20, AGORA);
            when(service.registrarSaida(1L, 20))
                    .thenReturn(Movimentacao.saida(produto, 20, AGORA));

            enviar("saidas", """
                    {"quantidade": 20}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("SAIDA"))
                    .andExpect(jsonPath("$.saldoResultante").value(0));
        }
    }
}
