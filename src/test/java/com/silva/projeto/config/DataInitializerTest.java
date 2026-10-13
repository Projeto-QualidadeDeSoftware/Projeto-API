package com.silva.projeto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.FuncionarioRepository;

class DataInitializerTest {

    private final FuncionarioRepository repository = mock(FuncionarioRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private DataInitializer inicializador(String adminSenha) {
        return new DataInitializer(repository, passwordEncoder, "00000000000", adminSenha);
    }

    @Test
    void converteSenhaAntigaEmTextoPuroParaHash() {
        Funcionario antigo = new Funcionario("Carlos", "98765432100", "Vendedor", "123456", 2500.0);
        when(repository.findAll()).thenReturn(List.of(antigo));
        when(repository.count()).thenReturn(1L);

        inicializador("admin123").run(null);

        assertThat(passwordEncoder.matches("123456", antigo.getSenha())).isTrue();
        verify(repository).save(antigo);
    }

    @Test
    void naoAlteraSenhaQueJaEstaProtegida() {
        String hash = passwordEncoder.encode("123456");
        Funcionario atual = new Funcionario("Carlos", "98765432100", "Vendedor", hash, 2500.0);
        when(repository.findAll()).thenReturn(List.of(atual));
        when(repository.count()).thenReturn(1L);

        inicializador("admin123").run(null);

        assertThat(atual.getSenha()).isEqualTo(hash);
        verify(repository, never()).save(any());
    }

    @Test
    void criaAdministradorQuandoNaoHaFuncionarios() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.count()).thenReturn(0L);

        inicializador("admin123").run(null);

        ArgumentCaptor<Funcionario> criado = ArgumentCaptor.forClass(Funcionario.class);
        verify(repository).save(criado.capture());

        assertThat(criado.getValue().getCpf()).isEqualTo("00000000000");
        assertThat(passwordEncoder.matches("admin123", criado.getValue().getSenha())).isTrue();
    }

    @Test
    void naoCriaAdministradorSemSenhaDefinida() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.count()).thenReturn(0L);

        inicializador("").run(null);

        verify(repository, never()).save(any());
    }
}
