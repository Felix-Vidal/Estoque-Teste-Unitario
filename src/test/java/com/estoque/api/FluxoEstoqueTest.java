package com.estoque.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Exemplo de teste de integração: sobe a aplicação inteira com o banco H2
 * e exercita a API pelo MockMvc.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FluxoEstoqueTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void cadastraProdutoMovimentaEConsultaHistorico() throws Exception {
        mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bebidas"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ean": "7891234567895", "nome": "Suco de uva", "preco": 8.90,
                                 "estoqueMinimo": 10, "categoriaId": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0));

        mvc.perform(post("/produtos/1/entradas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": 20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoResultante").value(20));

        // RN13: saída maior que o saldo é recusada
        mvc.perform(post("/produtos/1/saidas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": 21}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN13"));

        mvc.perform(get("/produtos/1/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
