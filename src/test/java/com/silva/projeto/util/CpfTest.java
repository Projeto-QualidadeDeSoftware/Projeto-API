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
    void validarRecusaQuantidadeErradaDeDigitos() {
        assertThat(Cpf.validar("123.456.789-01")).isEqualTo("12345678901");
        assertThatThrownBy(() -> Cpf.validar("123")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void mascararEscondeOsDigitosDoMeio() {
        assertThat(Cpf.mascarar("123.456.789-01")).isEqualTo("123.***.***-01");
        assertThat(Cpf.mascarar("12345678901")).isEqualTo("123.***.***-01");
        assertThat(Cpf.mascarar("123")).isEqualTo("***");
        assertThat(Cpf.mascarar(null)).isEqualTo("***");
    }
}
