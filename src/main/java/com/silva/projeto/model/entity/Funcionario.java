package com.silva.projeto.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.OneToMany;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    public String nome;

    @NotBlank(message = "CPF é obrigatório")
    public String cpf;

    @NotBlank(message = "Cargo é obrigatório")
    public String cargo;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 4, message = "Senha deve ter no mínimo 4 caracteres")
    public String senha;

    @Positive(message = "Salário deve ser maior que zero")
    public Double salario;

    public Funcionario() {
    }

    public Funcionario(String nome, String cpf, String cargo, String senha, Double salario) {

        this.nome = nome;
        this.cpf = cpf;
        this.cargo = cargo;
        this.senha = senha;
        this.salario = salario;
    }

    @OneToMany(mappedBy = "funcionario")
    @JsonIgnore
    private List<Venda> vendas = new ArrayList<>();

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

    public String getSenha() {
        return senha;
    }

    public Double getSalario() {
        return salario;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public void setSalario(Double salario) {
        this.salario = salario;
    }

    public List<Venda> getVendas() {
        return vendas;
    }

    public void setVendas(List<Venda> vendas) {
        this.vendas = vendas;
    }
}