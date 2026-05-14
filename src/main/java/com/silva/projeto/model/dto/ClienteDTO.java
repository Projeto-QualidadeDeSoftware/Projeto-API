package com.silva.projeto.model.dto;

public class ClienteDTO {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;

    public ClienteDTO(
            Long id,
            String nome,
            String cpf,
            String email,
            String telefone) {

        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.telefone = telefone;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }
}