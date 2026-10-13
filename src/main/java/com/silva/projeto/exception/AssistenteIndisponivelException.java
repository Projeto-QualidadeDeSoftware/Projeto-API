package com.silva.projeto.exception;

/** O serviço de IA não está configurado ou não respondeu (HTTP 503). */
public class AssistenteIndisponivelException extends RuntimeException {

    public AssistenteIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public AssistenteIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
