package com.silva.projeto.exception;

import java.time.LocalDateTime;

public class ErroResposta {

    private int status;
    private String mensagem;
    private LocalDateTime dataHora;

    public ErroResposta(int status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
        this.dataHora = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}