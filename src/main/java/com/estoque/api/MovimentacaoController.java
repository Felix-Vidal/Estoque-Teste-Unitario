package com.estoque.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.estoque.api.dto.Requisicoes.MovimentacaoRequest;
import com.estoque.api.dto.Respostas.MovimentacaoResponse;
import com.estoque.servico.MovimentacaoService;

/**
 * Não há endpoints para editar ou excluir movimentações (RN16):
 * PUT e DELETE nesses caminhos respondem 405 Method Not Allowed.
 */
@RestController
@RequestMapping("/produtos/{produtoId}")
public class MovimentacaoController {

    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService) {
        this.movimentacaoService = movimentacaoService;
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse entrada(@PathVariable Long produtoId, @RequestBody MovimentacaoRequest req) {
        return MovimentacaoResponse.de(movimentacaoService.registrarEntrada(produtoId, req.quantidadeInteira()));
    }

    @PostMapping("/saidas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse saida(@PathVariable Long produtoId, @RequestBody MovimentacaoRequest req) {
        return MovimentacaoResponse.de(movimentacaoService.registrarSaida(produtoId, req.quantidadeInteira()));
    }

    @GetMapping("/movimentacoes")
    public List<MovimentacaoResponse> historico(@PathVariable Long produtoId) {
        return movimentacaoService.listarPorProduto(produtoId).stream().map(MovimentacaoResponse::de).toList();
    }
}
