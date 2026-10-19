package com.estoque.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.estoque.api.dto.Requisicoes.CategoriaRequest;
import com.estoque.api.dto.Respostas.CategoriaResponse;
import com.estoque.dominio.Categoria;
import com.estoque.servico.CategoriaService;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@RequestBody CategoriaRequest req) {
        Categoria categoria = categoriaService.criar(req.nome());
        return ResponseEntity.created(URI.create("/categorias/" + categoria.getId()))
                .body(CategoriaResponse.de(categoria));
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listar().stream().map(CategoriaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return CategoriaResponse.de(categoriaService.buscar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
