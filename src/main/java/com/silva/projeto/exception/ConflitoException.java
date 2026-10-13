package com.silva.projeto.exception;

/** Operação válida, mas que conflita com o estado atual dos dados (HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
