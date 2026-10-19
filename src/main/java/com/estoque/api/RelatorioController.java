package com.estoque.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.estoque.api.dto.Respostas.ItemReposicaoResponse;
import com.estoque.servico.RelatorioService;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/reposicao")
    public List<ItemReposicaoResponse> reposicao(@RequestParam(required = false) Long categoriaId) {
        return relatorioService.reposicao(categoriaId).stream().map(ItemReposicaoResponse::de).toList();
    }
}
