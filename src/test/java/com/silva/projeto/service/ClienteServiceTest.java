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
import com.silva.projeto.exception.DadoInvalidoException;
import com.silva.projeto.exception.RecursoNaoEncontradoException;
import com.silva.projeto.model.dto.ClienteDTO;
import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.repository.ClienteRepository;
import com.silva.projeto.repository.VendaRepository;

class ClienteServiceTest {

    private final ClienteRepository repository = mock(ClienteRepository.class);
    private final VendaRepository vendaRepository = mock(VendaRepository.class);
    private final ClienteService service = new ClienteService(repository, vendaRepository);

    private Cliente maria() {
        return new Cliente("Maria", "529.982.247-25", "maria@email.com", "18999999999");
    }

    private Cliente comId(Cliente cliente, Long id) {
        cliente.setId(id);
        return cliente;
    }

    @Test
    void listarTodosDevolveOQueEstaNoBanco() {
        when(repository.findAll()).thenReturn(List.of(maria()));

        assertThat(service.listarTodos()).hasSize(1);
    }

    @Test
    void buscarPorIdInexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(9L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    void salvarGuardaCpfSoComNumeros() {
        when(repository.findFirstByCpf("52998224725")).thenReturn(Optional.empty());
        when(repository.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.salvar(maria()).getCpf()).isEqualTo("52998224725");
    }

    @Test
    void salvarRecusaCpfRepetido() {
        when(repository.findFirstByCpf("52998224725")).thenReturn(Optional.of(comId(maria(), 1L)));

        assertThatThrownBy(() -> service.salvar(maria()))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um cliente com este CPF");

        verify(repository, never()).save(any());
    }

    @Test
    void salvarRecusaCpfInvalido() {
        Cliente invalido = new Cliente("Joana", "111.111.111-11", "joana@email.com", "18999999999");

        assertThatThrownBy(() -> service.salvar(invalido))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF inválido");

        verify(repository, never()).save(any());
    }

    @Test
    void atualizarTrocaOsDadosEMantemOProprioCpf() {
        when(repository.findById(1L)).thenReturn(Optional.of(comId(maria(), 1L)));
        when(repository.findFirstByCpf("52998224725")).thenReturn(Optional.of(comId(maria(), 1L)));
        when(repository.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Cliente alteracao = new Cliente("Maria Souza", "52998224725", "nova@email.com", "18988887777");
        Cliente atualizado = service.atualizar(1L, alteracao);

        assertThat(atualizado.getNome()).isEqualTo("Maria Souza");
        assertThat(atualizado.getEmail()).isEqualTo("nova@email.com");
        assertThat(atualizado.getTelefone()).isEqualTo("18988887777");
    }

    @Test
    void atualizarRecusaCpfDeOutroCliente() {
        when(repository.findById(1L)).thenReturn(Optional.of(comId(maria(), 1L)));
        when(repository.findFirstByCpf("52998224725")).thenReturn(Optional.of(comId(maria(), 2L)));

        assertThatThrownBy(() -> service.atualizar(1L, maria()))
                .isInstanceOf(ConflitoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deletarClienteSemVendas() {
        Cliente maria = comId(maria(), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(maria));
        when(vendaRepository.existsByClienteId(1L)).thenReturn(false);

        service.deletar(1L);

        verify(repository).delete(maria);
    }

    @Test
    void deletarRecusaClienteComVendas() {
        when(repository.findById(1L)).thenReturn(Optional.of(comId(maria(), 1L)));
        when(vendaRepository.existsByClienteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deletar(1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível excluir o cliente porque ele tem vendas registradas");

        verify(repository, never()).delete(any());
    }

    @Test
    void converterParaDTOCopiaOsCampos() {
        ClienteDTO dto = service.converterParaDTO(comId(maria(), 3L));

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getNome()).isEqualTo("Maria");
        assertThat(dto.getCpf()).isEqualTo("529.982.247-25");
        assertThat(dto.getEmail()).isEqualTo("maria@email.com");
        assertThat(dto.getTelefone()).isEqualTo("18999999999");
    }
}
