package com.silva.projeto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErroResposta> tratarRuntime(RuntimeException ex) {

        ErroResposta erro =
                new ErroResposta(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(
            MethodArgumentNotValidException ex) {

        String mensagem =
                ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        ErroResposta erro =
                new ErroResposta(
                        HttpStatus.BAD_REQUEST.value(),
                        mensagem);

        return ResponseEntity
                .badRequest()
                .body(erro);
    }
}