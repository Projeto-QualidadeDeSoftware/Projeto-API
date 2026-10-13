package com.silva.projeto.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.model.dto.LoginRequest;
import com.silva.projeto.model.dto.LoginResponse;
import com.silva.projeto.model.dto.UsuarioLogadoDTO;
import com.silva.projeto.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest dados) {
        return service.login(dados);
    }

    @GetMapping("/me")
    public UsuarioLogadoDTO usuarioLogado(Authentication autenticacao) {
        return service.buscarUsuarioLogado(autenticacao.getName());
    }
}
