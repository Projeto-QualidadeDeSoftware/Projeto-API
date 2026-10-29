package com.silva.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.exception.RecursoNaoEncontradoException;
import com.silva.projeto.model.dto.ClienteDTO;
import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.repository.ClienteRepository;
import com.silva.projeto.repository.VendaRepository;
import com.silva.projeto.util.Cpf;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final VendaRepository vendaRepository;

    public ClienteService(ClienteRepository repository, VendaRepository vendaRepository) {
        this.repository = repository;
        this.vendaRepository = vendaRepository;
    }

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }

    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    public Cliente salvar(Cliente cliente) {

        cliente.setCpf(Cpf.validar(cliente.getCpf()));

        if (repository.findFirstByCpf(cliente.getCpf()).isPresent()) {
            throw new ConflitoException("Já existe um cliente com este CPF");
        }

        return repository.save(cliente);
    }

    public Cliente atualizar(Long id, Cliente clienteAtualizado) {

        Cliente cliente = buscarPorId(id);
        String cpf = Cpf.validar(clienteAtualizado.getCpf());

        repository.findFirstByCpf(cpf)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ConflitoException("Já existe um cliente com este CPF");
                });

        cliente.setNome(clienteAtualizado.getNome());
        cliente.setCpf(cpf);
        cliente.setEmail(clienteAtualizado.getEmail());
        cliente.setTelefone(clienteAtualizado.getTelefone());

        return repository.save(cliente);
    }

    public void deletar(Long id) {

        Cliente cliente = buscarPorId(id);

        if (vendaRepository.existsByClienteId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir o cliente porque ele tem vendas registradas");
        }

        repository.delete(cliente);
    }

    public ClienteDTO converterParaDTO(Cliente cliente) {

        return new ClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpf(),
                cliente.getEmail(),
                cliente.getTelefone()
        );
    }
}
