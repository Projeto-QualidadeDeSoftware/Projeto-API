package com.silva.projeto.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.model.dto.VendaDTO;
import com.silva.projeto.model.entity.Venda;
import com.silva.projeto.service.VendaService;

@RestController
@RequestMapping("/venda")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping
    public List<Venda> listar() {
        return vendaService.listarTodos();
    }

    @GetMapping("/{id}")
    public VendaDTO buscarPorId(@PathVariable Long id) {
        Venda venda = vendaService.buscarPorId(id);
        return vendaService.converterParaDTO(venda);
    }

    @PostMapping
    public Venda salvar(
            @RequestBody @Valid Venda venda,
            @RequestParam Long clienteId,
            @RequestParam Long funcionarioId,
            @RequestParam List<Long> produtosIds) {

        return vendaService.salvar(
                venda,
                clienteId,
                funcionarioId,
                produtosIds);
    }
    
    @PutMapping("/{id}")
    public Venda atualizar(
            @PathVariable Long id,
            @RequestBody @Valid Venda venda,
            @RequestParam Long clienteId,
            @RequestParam Long funcionarioId,
            @RequestParam List<Long> produtosIds) {

        return vendaService.atualizar(
                id,
                venda,
                clienteId,
                funcionarioId,
                produtosIds);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        vendaService.deletar(id);
    }
}