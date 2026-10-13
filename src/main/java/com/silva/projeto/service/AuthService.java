package com.silva.projeto.service;

import java.time.Instant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.silva.projeto.model.dto.LoginRequest;
import com.silva.projeto.model.dto.LoginResponse;
import com.silva.projeto.model.dto.UsuarioLogadoDTO;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.FuncionarioRepository;

@Service
public class AuthService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            FuncionarioRepository repository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest dados) {

        // Mesma mensagem para CPF inexistente e senha errada, para não revelar quais CPFs existem.
        Funcionario funcionario = repository.findFirstByCpf(dados.cpf())
                .filter(encontrado -> encontrado.getSenha() != null
                        && passwordEncoder.matches(dados.senha(), encontrado.getSenha()))
                .orElseThrow(() -> new BadCredentialsException("CPF ou senha inválidos"));

        Instant agora = Instant.now();

        return new LoginResponse(
                tokenService.gerar(funcionario, agora),
                "Bearer",
                tokenService.calcularExpiracao(agora),
                converter(funcionario));
    }

    public UsuarioLogadoDTO buscarUsuarioLogado(String idDoToken) {

        return repository.findById(Long.valueOf(idDoToken))
                .map(this::converter)
                .orElseThrow(() -> new BadCredentialsException("Usuário do token não existe mais"));
    }

    private UsuarioLogadoDTO converter(Funcionario funcionario) {

        return new UsuarioLogadoDTO(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCargo());
    }
}
