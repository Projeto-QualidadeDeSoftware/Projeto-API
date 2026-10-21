package com.silva.projeto.config;

import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.ClienteRepository;
import com.silva.projeto.repository.FuncionarioRepository;
import com.silva.projeto.util.Cpf;

/**
 * Executa ao iniciar a aplicação:
 * 1) ajusta os dados antigos: senhas em texto puro viram hash e CPFs com máscara
 *    passam a ficar só com números;
 * 2) cria o primeiro usuário quando não existe nenhum funcionário, já que
 *    sem login não é mais possível cadastrar ninguém.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Pattern HASH_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final FuncionarioRepository repository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminCpf;
    private final String adminSenha;

    public DataInitializer(
            FuncionarioRepository repository,
            ClienteRepository clienteRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.cpf}") String adminCpf,
            @Value("${app.admin.senha}") String adminSenha) {

        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminCpf = Cpf.normalizar(adminCpf);
        this.adminSenha = adminSenha;
    }

    @Override
    public void run(ApplicationArguments args) {
        ajustarFuncionariosAntigos();
        ajustarClientesAntigos();
        criarAdministradorInicial();
    }

    private void ajustarFuncionariosAntigos() {

        for (Funcionario funcionario : repository.findAll()) {

            boolean alterado = false;

            String senha = funcionario.getSenha();
            if (senha != null && !HASH_BCRYPT.matcher(senha).matches()) {
                funcionario.setSenha(passwordEncoder.encode(senha));
                alterado = true;
            }

            String cpf = Cpf.normalizar(funcionario.getCpf());
            if (!Objects.equals(cpf, funcionario.getCpf())) {
                funcionario.setCpf(cpf);
                alterado = true;
            }

            if (!alterado) {
                continue;
            }

            try {
                repository.save(funcionario);
            } catch (RuntimeException erro) {
                // Ex.: dois funcionários com o mesmo CPF gravado em formatos diferentes.
                log.warn("Não foi possível ajustar o funcionário {}: {}", funcionario.getId(), erro.getMessage());
            }
        }
    }

    private void ajustarClientesAntigos() {

        for (Cliente cliente : clienteRepository.findAll()) {

            String cpf = Cpf.normalizar(cliente.getCpf());
            if (Objects.equals(cpf, cliente.getCpf())) {
                continue;
            }

            try {
                cliente.setCpf(cpf);
                clienteRepository.save(cliente);
            } catch (RuntimeException erro) {
                log.warn("Não foi possível ajustar o CPF do cliente {}: {}", cliente.getId(), erro.getMessage());
            }
        }
    }

    private void criarAdministradorInicial() {

        if (repository.count() > 0) {
            return;
        }

        if (adminSenha == null || adminSenha.isBlank()) {
            log.warn("Nenhum funcionário cadastrado e ADMIN_SENHA não definida: ninguém conseguirá fazer login");
            return;
        }

        Funcionario admin = new Funcionario(
                "Administrador",
                adminCpf,
                "Administrador",
                passwordEncoder.encode(adminSenha),
                null);

        repository.save(admin);

        log.info("Usuário inicial criado com o CPF {}", adminCpf);
    }
}
