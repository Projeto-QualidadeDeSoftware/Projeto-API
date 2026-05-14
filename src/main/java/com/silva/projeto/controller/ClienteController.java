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

import com.silva.projeto.model.dto.ClienteDTO;
import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.model.entity.Venda;
import com.silva.projeto.service.ClienteService;

@RestController
@RequestMapping("/cliente")
public class ClienteController {
	
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Cliente> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ClienteDTO buscarPorId(@PathVariable Long id) {
        Cliente cliente = service.buscarPorId(id);
        return service.converterParaDTO(cliente);
    }

    @PostMapping
    public Cliente salvar(@RequestBody @Valid Cliente cliente) {
        return service.salvar(cliente);
    }

    @PutMapping("/{id}")
    public Cliente atualizar(
            @PathVariable Long id,
            @RequestBody @Valid Cliente cliente) {

        return service.atualizar(id, cliente);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
    
    @GetMapping("/{id}/vendas")
    public List<Venda> listarVendasDoCliente(@PathVariable Long id) {
        Cliente cliente = service.buscarPorId(id);
        return cliente.getVendas();
    }
}