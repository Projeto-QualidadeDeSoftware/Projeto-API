package com.silva.projeto.config;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.FuncionarioRepository;

/**
 * Executa ao iniciar a aplicação:
 * 1) converte para hash as senhas antigas que estavam gravadas em texto puro;
 * 2) cria o primeiro usuário quando não existe nenhum funcionário, já que
 *    sem login não é mais possível cadastrar ninguém.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Pattern HASH_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String adminCpf;
    private final String adminSenha;

    public DataInitializer(
            FuncionarioRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.cpf}") String adminCpf,
            @Value("${app.admin.senha}") String adminSenha) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.adminCpf = adminCpf;
        this.adminSenha = adminSenha;
    }

    @Override
    public void run(ApplicationArguments args) {
        protegerSenhasAntigas();
        criarAdministradorInicial();
    }

    private void protegerSenhasAntigas() {

        for (Funcionario funcionario : repository.findAll()) {

            String senha = funcionario.getSenha();

            if (senha == null || HASH_BCRYPT.matcher(senha).matches()) {
                continue;
            }

            try {
                funcionario.setSenha(passwordEncoder.encode(senha));
                repository.save(funcionario);
            } catch (RuntimeException erro) {
                log.warn("Não foi possível proteger a senha do funcionário {}: {}",
                        funcionario.getId(), erro.getMessage());
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
