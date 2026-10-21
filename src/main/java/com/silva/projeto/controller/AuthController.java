package com.silva.projeto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.config.AuditoriaInterceptor;
import com.silva.projeto.model.dto.LoginRequest;
import com.silva.projeto.model.dto.LoginResponse;
import com.silva.projeto.model.dto.UsuarioLogadoDTO;
import com.silva.projeto.service.AuthService;
import com.silva.projeto.util.Cpf;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Login")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Faz login com CPF e senha e devolve o token")
    public LoginResponse login(@RequestBody @Valid LoginRequest dados, HttpServletRequest request) {
        request.setAttribute(AuditoriaInterceptor.ATRIBUTO_USUARIO, Cpf.mascarar(dados.cpf()));

        LoginResponse resposta = service.login(dados);

        request.setAttribute(AuditoriaInterceptor.ATRIBUTO_FUNCIONARIO, resposta.funcionario().id());
        return resposta;
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do funcionário dono do token")
    public UsuarioLogadoDTO usuarioLogado(Authentication autenticacao) {
        return service.buscarUsuarioLogado(autenticacao.getName());
    }
}
