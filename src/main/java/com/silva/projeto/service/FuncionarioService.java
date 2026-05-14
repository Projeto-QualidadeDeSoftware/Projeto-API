package com.silva.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.FuncionarioRepository;
import com.silva.projeto.model.dto.FuncionarioDTO;

@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;

    public FuncionarioService(FuncionarioRepository repository) {
        this.repository = repository;
    }

    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
    }

    public Funcionario salvar(Funcionario funcionario) {
        return repository.save(funcionario);
    }

    public Funcionario atualizar(Long id, Funcionario funcionarioAtualizado) {

        Funcionario funcionario = buscarPorId(id);

        funcionario.setNome(funcionarioAtualizado.getNome());
        funcionario.setCpf(funcionarioAtualizado.getCpf());
        funcionario.setCargo(funcionarioAtualizado.getCargo());
        funcionario.setSenha(funcionarioAtualizado.getSenha());
        funcionario.setSalario(funcionarioAtualizado.getSalario());

        return repository.save(funcionario);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
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