package com.estoque.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.estoque.api.dto.Requisicoes.ProdutoRequest;
import com.estoque.api.dto.Respostas.ProdutoResponse;
import com.estoque.dominio.Produto;
import com.estoque.servico.ProdutoService;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> cadastrar(@RequestBody ProdutoRequest req) {
        Produto produto = produtoService.cadastrar(req.ean(), req.nome(), req.preco(),
                req.estoqueMinimo(), req.categoriaId());
        return ResponseEntity.created(URI.create("/produtos/" + produto.getId()))
                .body(ProdutoResponse.de(produto));
    }

    @GetMapping
    public List<ProdutoResponse> listar() {
        return produtoService.listar().stream().map(ProdutoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return ProdutoResponse.de(produtoService.buscar(id));
    }

    @PutMapping("/{id}")
    public ProdutoResponse alterar(@PathVariable Long id, @RequestBody ProdutoRequest req) {
        return ProdutoResponse.de(produtoService.alterar(id, req.ean(), req.nome(), req.preco(),
                req.estoqueMinimo(), req.categoriaId()));
    }

    @PatchMapping("/{id}/inativar")
    public ProdutoResponse inativar(@PathVariable Long id) {
        return ProdutoResponse.de(produtoService.inativar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
