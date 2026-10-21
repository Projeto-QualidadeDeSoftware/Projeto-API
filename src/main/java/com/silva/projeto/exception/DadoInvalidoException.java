package com.silva.projeto.exception;

/** Dado recebido em formato inválido (HTTP 400). */
public class DadoInvalidoException extends RuntimeException {

    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
