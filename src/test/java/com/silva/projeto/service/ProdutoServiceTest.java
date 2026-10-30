package com.silva.projeto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.exception.RecursoNaoEncontradoException;
import com.silva.projeto.model.dto.ProdutoDTO;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.repository.ItemVendaRepository;
import com.silva.projeto.repository.ProdutoRepository;

class ProdutoServiceTest {

    private final ProdutoRepository repository = mock(ProdutoRepository.class);
    private final ItemVendaRepository itemVendaRepository = mock(ItemVendaRepository.class);
    private final ProdutoService service = new ProdutoService(repository, itemVendaRepository);

    private Produto agua() {
        Produto agua = new Produto("Água", "Água mineral 500ml", 3.5, 10);
        agua.setId(1L);
        return agua;
    }

    @Test
    void listarTodosDevolveOQueEstaNoBanco() {
        when(repository.findAll()).thenReturn(List.of(agua()));

        assertThat(service.listarTodos()).hasSize(1);
    }

    @Test
    void buscarPorIdInexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(9L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
    void salvarGravaOProduto() {
        when(repository.save(any(Produto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.salvar(agua()).getNome()).isEqualTo("Água");
    }

    @Test
    void atualizarTrocaTodosOsCampos() {
        when(repository.findById(1L)).thenReturn(Optional.of(agua()));
        when(repository.save(any(Produto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Produto atualizado = service.atualizar(1L, new Produto("Água com gás", "Água com gás 500ml", 4.0, 20));

        assertThat(atualizado.getNome()).isEqualTo("Água com gás");
        assertThat(atualizado.getDescricao()).isEqualTo("Água com gás 500ml");
        assertThat(atualizado.getPreco()).isEqualTo(4.0);
        assertThat(atualizado.getEstoque()).isEqualTo(20);
    }

    @Test
    void deletarProdutoSemVendas() {
        Produto agua = agua();
        when(repository.findById(1L)).thenReturn(Optional.of(agua));
        when(itemVendaRepository.existsByProdutoId(1L)).thenReturn(false);

        service.deletar(1L);

        verify(repository).delete(agua);
    }

    @Test
    void deletarRecusaProdutoQueApareceEmVendas() {
        when(repository.findById(1L)).thenReturn(Optional.of(agua()));
        when(itemVendaRepository.existsByProdutoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deletar(1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível excluir o produto porque ele aparece em vendas registradas");

        verify(repository, never()).delete(any());
    }

    @Test
    void converterParaDTOCopiaOsCampos() {
        ProdutoDTO dto = service.converterParaDTO(agua());

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNome()).isEqualTo("Água");
        assertThat(dto.getPreco()).isEqualTo(3.5);
        assertThat(dto.getEstoque()).isEqualTo(10);
    }
}
