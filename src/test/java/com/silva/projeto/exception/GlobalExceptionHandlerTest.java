package com.silva.projeto.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void restricaoDoBancoViraConflitoSemMensagemTecnica() {
        DataIntegrityViolationException erro = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("Cannot delete or update a parent row: a foreign key constraint fails"));

        ResponseEntity<ErroResposta> resposta = handler.tratarIntegridade(erro);

        assertThat(resposta.getStatusCode().value()).isEqualTo(409);
        assertThat(resposta.getBody().getStatus()).isEqualTo(409);
        assertThat(resposta.getBody().getMensagem())
                .isEqualTo("Operação não permitida: o registro está vinculado a outros dados")
                .doesNotContain("foreign key");
        assertThat(resposta.getBody().getDataHora()).isNotNull();
    }

    @Test
    void erroInesperadoViraErroInternoSemDetalhes() {
        ResponseEntity<ErroResposta> resposta =
                handler.tratarInesperado(new IllegalStateException("NullPointer em VendaService"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getBody().getMensagem())
                .isEqualTo("Erro interno no servidor. Tente novamente.");
    }

    @Test
    void falhaDeAutenticacaoVira401() {
        ResponseEntity<ErroResposta> resposta =
                handler.tratarAutenticacao(new BadCredentialsException("CPF ou senha inválidos"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(401);
        assertThat(resposta.getBody().getMensagem()).isEqualTo("CPF ou senha inválidos");
    }
}
