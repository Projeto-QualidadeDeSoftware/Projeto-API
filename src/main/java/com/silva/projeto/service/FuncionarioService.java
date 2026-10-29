package com.silva.projeto.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.exception.RecursoNaoEncontradoException;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.FuncionarioRepository;
import com.silva.projeto.repository.VendaRepository;
import com.silva.projeto.util.Cpf;
import com.silva.projeto.model.dto.FuncionarioDTO;

@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final VendaRepository vendaRepository;

    public FuncionarioService(
            FuncionarioRepository repository,
            PasswordEncoder passwordEncoder,
            VendaRepository vendaRepository) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.vendaRepository = vendaRepository;
    }

    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
    }

    public Funcionario salvar(Funcionario funcionario) {

        funcionario.setCpf(Cpf.validar(funcionario.getCpf()));

        // O CPF é o login, então não pode se repetir.
        if (repository.findFirstByCpf(funcionario.getCpf()).isPresent()) {
            throw new ConflitoException("Já existe um funcionário com este CPF");
        }

        funcionario.setSenha(passwordEncoder.encode(funcionario.getSenha()));

        return repository.save(funcionario);
    }

    public Funcionario atualizar(Long id, Funcionario funcionarioAtualizado) {

        Funcionario funcionario = buscarPorId(id);
        String cpf = Cpf.validar(funcionarioAtualizado.getCpf());

        repository.findFirstByCpf(cpf)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ConflitoException("Já existe um funcionário com este CPF");
                });

        funcionario.setNome(funcionarioAtualizado.getNome());
        funcionario.setCpf(cpf);
        funcionario.setCargo(funcionarioAtualizado.getCargo());
        funcionario.setSenha(passwordEncoder.encode(funcionarioAtualizado.getSenha()));
        funcionario.setSalario(funcionarioAtualizado.getSalario());

        return repository.save(funcionario);
    }

    public void deletar(Long id) {

        Funcionario funcionario = buscarPorId(id);

        if (vendaRepository.existsByFuncionarioId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir o funcionário porque ele tem vendas registradas");
        }

        repository.delete(funcionario);
    }
    
    public FuncionarioDTO converterParaDTO(Funcionario funcionario) {

        return new FuncionarioDTO(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCpf(),
                funcionario.getCargo(),
                funcionario.getSalario()
        );
    }
}
