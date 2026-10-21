package com.silva.projeto.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma linha da trilha de auditoria: quem fez, o quê, quando, de onde e qual foi o resultado.
 * Os registros só são incluídos, nunca alterados.
 */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    private Long funcionarioId;

    @Column(length = 120)
    private String usuario;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(nullable = false, length = 10)
    private String metodo;

    @Column(nullable = false)
    private String recurso;

    private int status;

    @Column(length = 64)
    private String ip;

    private long duracaoMs;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(
            LocalDateTime dataHora,
            Long funcionarioId,
            String usuario,
            String acao,
            String metodo,
            String recurso,
            int status,
            String ip,
            long duracaoMs) {

        this.dataHora = dataHora;
        this.funcionarioId = funcionarioId;
        this.usuario = usuario;
        this.acao = acao;
        this.metodo = metodo;
        this.recurso = recurso;
        this.status = status;
        this.ip = ip;
        this.duracaoMs = duracaoMs;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAcao() {
        return acao;
    }

    public String getMetodo() {
        return metodo;
    }

    public String getRecurso() {
        return recurso;
    }

    public int getStatus() {
        return status;
    }

    public String getIp() {
        return ip;
    }

    public long getDuracaoMs() {
        return duracaoMs;
    }
}
