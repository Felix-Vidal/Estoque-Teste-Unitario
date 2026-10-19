package com.estoque.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.estoque.excecao.RecursoNaoEncontradoException;
import com.estoque.excecao.RegraNegocioException;
import com.estoque.repositorio.CategoriaRepository;
import com.estoque.repositorio.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final Long ID = 1L;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private CategoriaService service;

    @BeforeEach
    void configurar() {
        service = new CategoriaService(categoriaRepository, produtoRepository);
    }

    private void nomeAindaNaoExiste() {
        when(categoriaRepository.existsByNomeIgnoreCase(anyString())).thenReturn(false);
    }

    private void salvarDevolveOArgumento() {
        when(categoriaRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private void categoriaExiste(Categoria categoria) {
        when(categoriaRepository.findById(ID)).thenReturn(Optional.of(categoria));
    }

    @Nested
    class RN01TamanhoDoNome {

        //RN01
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "Ab", "  Ab  "})
        void deveRecusarNomeVazioOuComMenosDe3Caracteres(String nome) {
            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.criar(nome));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN01");
            verify(categoriaRepository, never()).save(any());
        }

        //RN01
        @Test
        void deveRecusarNomeCom51Caracteres() {
            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.criar("A".repeat(51)));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN01");
            verify(categoriaRepository, never()).save(any());
        }

        //RN01
        @ParameterizedTest
        @ValueSource(ints = {3, 50})
        void deveAceitarNomeNosLimitesDoTamanho(int tamanho) {
            nomeAindaNaoExiste();
            salvarDevolveOArgumento();
            String nome = "A".repeat(tamanho);

            Categoria categoria = service.criar(nome);

            assertThat(categoria.getNome()).isEqualTo(nome);
        }

        //RN01
        @Test
        void deveContarOTamanhoDepoisDeRemoverEspacosDasPontas() {
            nomeAindaNaoExiste();
            salvarDevolveOArgumento();

            // 54 caracteres no total, mas 50 depois de remover os espaços: deve ser aceito
            Categoria categoria = service.criar("  " + "A".repeat(50) + "  ");

            assertThat(categoria.getNome()).isEqualTo("A".repeat(50));
        }

        //RN01
        @Test
        void deveSalvarONomeSemEspacosNasPontas() {
            nomeAindaNaoExiste();
            salvarDevolveOArgumento();

            service.criar("  Bebidas  ");

            ArgumentCaptor<Categoria> salva = ArgumentCaptor.forClass(Categoria.class);
            verify(categoriaRepository).save(salva.capture());
            assertThat(salva.getValue().getNome()).isEqualTo("Bebidas");
        }

        //RN01
        @Test
        void deveVerificarOTamanhoAntesDaUnicidade() {
            assertThrows(RegraNegocioException.class, () -> service.criar("Ab"));

            verify(categoriaRepository, never()).existsByNomeIgnoreCase(anyString());
        }
    }

    @Nested
    class RN01NomeUnico {

        //RN01
        @Test
        void deveRecusarNomeJaExistente() {
            when(categoriaRepository.existsByNomeIgnoreCase("Bebidas")).thenReturn(true);

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.criar("Bebidas"));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN01");
            verify(categoriaRepository, never()).save(any());
        }

        //RN01
        @Test
        void deveConsultarUnicidadeComONomeSemEspacos() {
            nomeAindaNaoExiste();
            salvarDevolveOArgumento();

            service.criar("  Bebidas  ");

            // Se consultasse "  Bebidas  ", um nome repetido com espaços passaria pela verificação
            verify(categoriaRepository).existsByNomeIgnoreCase("Bebidas");
        }

        //RN01
        @Test
        void deveCriarCategoriaComNomeNovo() {
            nomeAindaNaoExiste();
            salvarDevolveOArgumento();

            Categoria categoria = service.criar("Bebidas");

            assertThat(categoria.getNome()).isEqualTo("Bebidas");
            verify(categoriaRepository).save(any(Categoria.class));
        }
    }

    @Nested
    class RN02ExclusaoDeCategoria {

        //RN02
        @Test
        void deveRecusarExclusaoDeCategoriaComProdutos() {
            categoriaExiste(new Categoria("Bebidas"));
            when(produtoRepository.existsByCategoriaId(ID)).thenReturn(true);

            RegraNegocioException excecao = assertThrows(RegraNegocioException.class,
                    () -> service.excluir(ID));

            assertThat(excecao.getCodigoRegra()).isEqualTo("RN02");
            verify(categoriaRepository, never()).delete(any());
        }

        //RN02
        @Test
        void deveExcluirCategoriaSemProdutos() {
            Categoria categoria = new Categoria("Bebidas");
            categoriaExiste(categoria);
            when(produtoRepository.existsByCategoriaId(ID)).thenReturn(false);

            service.excluir(ID);

            verify(categoriaRepository).delete(categoria);
        }

        //RN02
        @Test
        void deveRecusarExclusaoDeCategoriaInexistente() {
            when(categoriaRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(RecursoNaoEncontradoException.class, () -> service.excluir(99L));

            verify(produtoRepository, never()).existsByCategoriaId(anyLong());
            verify(categoriaRepository, never()).delete(any());
        }
    }

    @Nested
    class Consultas {

        @Test
        void deveBuscarCategoriaExistente() {
            Categoria categoria = new Categoria("Bebidas");
            categoriaExiste(categoria);

            assertThat(service.buscar(ID)).isSameAs(categoria);
        }

        @Test
        void deveLancarNaoEncontradoAoBuscarCategoriaInexistente() {
            when(categoriaRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(RecursoNaoEncontradoException.class, () -> service.buscar(99L));
        }

        @Test
        void deveListarTodasAsCategorias() {
            List<Categoria> categorias = List.of(new Categoria("Bebidas"), new Categoria("Limpeza"));
            when(categoriaRepository.findAll()).thenReturn(categorias);

            assertThat(service.listar()).containsExactlyElementsOf(categorias);
        }
    }
}