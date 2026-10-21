package com.silva.projeto.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.silva.projeto.exception.AcessoNegadoException;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.RegistroAuditoria;
import com.silva.projeto.repository.AuditoriaRepository;
import com.silva.projeto.repository.FuncionarioRepository;

@Service
public class AuditoriaService {

    static final String CARGO_ADMINISTRADOR = "Administrador";
    static final int TAMANHO_MAXIMO = 200;

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository repository;
    private final FuncionarioRepository funcionarioRepository;

    public AuditoriaService(
            AuditoriaRepository repository,
            FuncionarioRepository funcionarioRepository) {

        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
    }

    /** Falha ao gravar a auditoria fica no log, mas não derruba a operação do usuário. */
    public void registrar(RegistroAuditoria registro) {
        try {
            repository.save(registro);
        } catch (RuntimeException erro) {
            log.error("Falha ao gravar auditoria de {} {}: {}",
                    registro.getMetodo(), registro.getRecurso(), erro.getMessage());
        }
    }

    public String nomeDoFuncionario(Long id) {
        return funcionarioRepository.findById(id)
                .map(Funcionario::getNome)
                .orElse("funcionário " + id);
    }

    /** Registros mais recentes primeiro. */
    public List<RegistroAuditoria> listar(int pagina, int tamanho) {
        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);

        return repository
                .findAllByOrderByIdDesc(PageRequest.of(paginaValida, tamanhoValido))
                .getContent();
    }

    public void exigirAdministrador(String idDoToken) {
        boolean administrador = funcionarioRepository.findById(Long.valueOf(idDoToken))
                .map(funcionario -> CARGO_ADMINISTRADOR.equalsIgnoreCase(funcionario.getCargo()))
                .orElse(false);

        if (!administrador) {
            throw new AcessoNegadoException("Somente administradores podem consultar a auditoria");
        }
    }
}
