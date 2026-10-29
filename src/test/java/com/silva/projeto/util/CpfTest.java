package com.silva.projeto.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.silva.projeto.exception.DadoInvalidoException;

class CpfTest {

    @Test
    void normalizarDeixaSoOsNumeros() {
        assertThat(Cpf.normalizar("123.456.789-01")).isEqualTo("12345678901");
        assertThat(Cpf.normalizar(null)).isNull();
    }

    @Test
    void validarAceitaCpfValidoComOuSemMascara() {
        assertThat(Cpf.validar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(Cpf.validar("98765432100")).isEqualTo("98765432100");
        assertThat(Cpf.validar("111.222.333-96")).isEqualTo("11122233396");
    }

    @Test
    void validarRecusaQuantidadeErradaDeDigitos() {
        assertThatThrownBy(() -> Cpf.validar("123"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF deve ter 11 dígitos");
        assertThatThrownBy(() -> Cpf.validar(null))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF deve ter 11 dígitos");
    }

    @Test
    void validarRecusaDigitosVerificadoresErrados() {
        assertThatThrownBy(() -> Cpf.validar("123.456.789-01"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF inválido");
        assertThatThrownBy(() -> Cpf.validar("52998224724"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF inválido");
    }

    @Test
    void validarRecusaCpfComTodosOsDigitosIguais() {
        assertThatThrownBy(() -> Cpf.validar("111.111.111-11"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF inválido");
        assertThatThrownBy(() -> Cpf.validar("00000000000"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF inválido");
    }

    @Test
    void mascararEscondeOsDigitosDoMeio() {
        assertThat(Cpf.mascarar("123.456.789-01")).isEqualTo("123.***.***-01");
        assertThat(Cpf.mascarar("12345678901")).isEqualTo("123.***.***-01");
        assertThat(Cpf.mascarar("123")).isEqualTo("***");
        assertThat(Cpf.mascarar(null)).isEqualTo("***");
    }
}
