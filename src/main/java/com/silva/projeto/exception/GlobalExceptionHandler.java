package com.silva.projeto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErroResposta> tratarRuntime(RuntimeException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> tratarAutenticacao(AuthenticationException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> tratarConflito(ConflitoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AssistenteIndisponivelException.class)
    public ResponseEntity<ErroResposta> tratarAssistenteIndisponivel(
            AssistenteIndisponivelException ex) {

        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(
            MethodArgumentNotValidException ex) {

        String mensagem =
                ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return responder(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<ErroResposta> responder(HttpStatus status, String mensagem) {

        return ResponseEntity
                .status(status)
                .body(new ErroResposta(status.value(), mensagem));
    }
}
