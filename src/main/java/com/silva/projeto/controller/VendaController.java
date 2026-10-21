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
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.model.dto.VendaDTO;
import com.silva.projeto.model.dto.VendaRequest;
import com.silva.projeto.service.VendaService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/venda")
@Tag(name = "Vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping
    public List<VendaDTO> listar() {
        return vendaService.listarTodos()
                .stream()
                .map(vendaService::converterParaDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public VendaDTO buscarPorId(@PathVariable Long id) {
        return vendaService.converterParaDTO(vendaService.buscarPorId(id));
    }

    @PostMapping
    public VendaDTO salvar(@RequestBody @Valid VendaRequest dados) {
        return vendaService.converterParaDTO(vendaService.salvar(dados));
    }

    @PutMapping("/{id}")
    public VendaDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid VendaRequest dados) {

        return vendaService.converterParaDTO(vendaService.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        vendaService.deletar(id);
    }
}
