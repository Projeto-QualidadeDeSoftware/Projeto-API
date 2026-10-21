package com.silva.projeto.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.model.entity.RegistroAuditoria;
import com.silva.projeto.service.AuditoriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auditoria")
@Tag(name = "Auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as operações registradas, das mais recentes para as mais antigas",
            description = "Somente para funcionários com cargo Administrador. Até 200 registros por página.")
    public List<RegistroAuditoria> listar(
            Authentication autenticacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {

        service.exigirAdministrador(autenticacao.getName());
        return service.listar(pagina, tamanho);
    }
}
