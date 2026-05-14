package com.silva.projeto.model.dto;

public class FuncionarioDTO {

    private Long id;
    private String nome;
    private String cpf;
    private String cargo;
    private Double salario;

    public FuncionarioDTO(
            Long id,
            String nome,
            String cpf,
            String cargo,
            Double salario) {

        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.cargo = cargo;
        this.salario = salario;
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

    public String getCargo() {
        return cargo;
    }

    public Double getSalario() {
        return salario;
    }
}