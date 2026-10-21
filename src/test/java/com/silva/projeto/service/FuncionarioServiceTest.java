package com.silva.projeto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.exception.DadoInvalidoException;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.repository.FuncionarioRepository;

class FuncionarioServiceTest {

    private final FuncionarioRepository repository = mock(FuncionarioRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final FuncionarioService service = new FuncionarioService(repository, passwordEncoder);

    private Funcionario carlos() {
        return new Funcionario("Carlos", "98765432100", "Vendedor", "123456", 2500.0);
    }

    @Test
    void salvarGravaASenhaComoHash() {
        when(repository.findFirstByCpf("98765432100")).thenReturn(Optional.empty());
        when(repository.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario salvo = service.salvar(carlos());

        assertThat(salvo.getSenha()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", salvo.getSenha())).isTrue();
    }

    @Test
    void salvarRecusaCpfRepetido() {
        when(repository.findFirstByCpf("98765432100")).thenReturn(Optional.of(carlos()));

        assertThatThrownBy(() -> service.salvar(carlos()))
                .isInstanceOf(ConflitoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void atualizarRecusaCpfDeOutroFuncionario() {
        Funcionario existente = carlos();
        existente.setId(1L);

        Funcionario outro = carlos();
        outro.setId(2L);

        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.findFirstByCpf("98765432100")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.atualizar(1L, carlos()))
                .isInstanceOf(ConflitoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void atualizarAceitaManterOProprioCpfETrocaASenha() {
        Funcionario existente = carlos();
        existente.setId(1L);

        Funcionario alteracao = carlos();
        alteracao.setSenha("novaSenha");

        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.findFirstByCpf("98765432100")).thenReturn(Optional.of(existente));
        when(repository.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario atualizado = service.atualizar(1L, alteracao);

        assertThat(passwordEncoder.matches("novaSenha", atualizado.getSenha())).isTrue();
    }

    @Test
    void salvarGuardaCpfSoComNumeros() {
        Funcionario comMascara = new Funcionario("Carlos", "987.654.321-00", "Vendedor", "123456", 2500.0);
        when(repository.findFirstByCpf("98765432100")).thenReturn(Optional.empty());
        when(repository.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario salvo = service.salvar(comMascara);

        assertThat(salvo.getCpf()).isEqualTo("98765432100");
    }

    @Test
    void salvarRecusaCpfRepetidoEmOutroFormato() {
        Funcionario comMascara = new Funcionario("Carlos", "987.654.321-00", "Vendedor", "123456", 2500.0);
        when(repository.findFirstByCpf("98765432100")).thenReturn(Optional.of(carlos()));

        assertThatThrownBy(() -> service.salvar(comMascara))
                .isInstanceOf(ConflitoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void salvarRecusaCpfSemOnzeDigitos() {
        Funcionario invalido = new Funcionario("Carlos", "123.456", "Vendedor", "123456", 2500.0);

        assertThatThrownBy(() -> service.salvar(invalido))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF deve ter 11 dígitos");

        verify(repository, never()).save(any());
    }
}
