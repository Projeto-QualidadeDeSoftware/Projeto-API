package com.silva.projeto.controller;

import java.util.List;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.model.dto.FuncionarioDTO;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.Venda;
import com.silva.projeto.service.FuncionarioService;

@RestController
@RequestMapping("/funcionario")
public class FuncionarioController {

    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Funcionario> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public FuncionarioDTO buscarPorId(@PathVariable Long id) {
        Funcionario funcionario = service.buscarPorId(id);
        return service.converterParaDTO(funcionario);
    }

    @PostMapping
    public Funcionario salvar(@RequestBody @Valid Funcionario funcionario) {
        return service.salvar(funcionario);
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable Long id,
                                 @RequestBody @Valid Funcionario funcionario) {

        return service.atualizar(id, funcionario);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
    
    @GetMapping("/{id}/vendas")
    public List<Venda> listarVendasDoFuncionario(@PathVariable Long id) {
        Funcionario funcionario = service.buscarPorId(id);
        return funcionario.getVendas();
    }
}