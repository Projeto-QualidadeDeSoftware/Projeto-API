package com.silva.projeto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.silva.projeto.exception.AcessoNegadoException;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.RegistroAuditoria;
import com.silva.projeto.repository.AuditoriaRepository;
import com.silva.projeto.repository.FuncionarioRepository;

class AuditoriaServiceTest {

    private final AuditoriaRepository repository = mock(AuditoriaRepository.class);
    private final FuncionarioRepository funcionarioRepository = mock(FuncionarioRepository.class);
    private final AuditoriaService service = new AuditoriaService(repository, funcionarioRepository);

    private static RegistroAuditoria registro() {
        return new RegistroAuditoria(LocalDateTime.now(), 1L, "Administrador",
                "INCLUSAO", "POST", "/produto", 200, "127.0.0.1", 12);
    }

    @Test
    void falhaAoGravarAuditoriaNaoInterrompeAOperacao() {
        when(repository.save(any(RegistroAuditoria.class))).thenThrow(new IllegalStateException("banco fora"));

        assertThatCode(() -> service.registrar(registro())).doesNotThrowAnyException();
    }

    @Test
    void registrarGravaORegistro() {
        RegistroAuditoria registro = registro();

        service.registrar(registro);

        verify(repository).save(registro);
    }

    @Test
    void listarLimitaPaginaETamanho() {
        Page<RegistroAuditoria> vazia = new PageImpl<>(List.of());
        when(repository.findAllByOrderByIdDesc(any(Pageable.class))).thenReturn(vazia);

        service.listar(-3, 5000);

        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByIdDesc(pedido.capture());
        assertThat(pedido.getValue().getPageNumber()).isZero();
        assertThat(pedido.getValue().getPageSize()).isEqualTo(AuditoriaService.TAMANHO_MAXIMO);
    }

    @Test
    void somenteAdministradorConsultaAAuditoria() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(
                new Funcionario("Ana", "11122233344", "Administrador", "hash", 3000.0)));
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(
                new Funcionario("Vera", "52998224725", "Vendedor", "hash", 1800.0)));

        assertThatCode(() -> service.exigirAdministrador("1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.exigirAdministrador("2"))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.exigirAdministrador("99"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void nomeDoFuncionarioRemovidoUsaOId() {
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(service.nomeDoFuncionario(7L)).isEqualTo("funcionário 7");
    }
}
