package com.silva.projeto.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Entity
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Data da venda é obrigatória")
    private LocalDate dataVenda;

    private Double valorTotal;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @NotNull(message = "Cliente é obrigatório")
    @JsonIgnoreProperties("vendas")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    @NotNull(message = "Funcionário é obrigatório")
    @JsonIgnoreProperties("vendas")
    private Funcionario funcionario;

    @ManyToMany
    @JoinTable(
        name = "venda_produto",
        joinColumns = @JoinColumn(name = "venda_id"),
        inverseJoinColumns = @JoinColumn(name = "produto_id")
    )
    @NotEmpty(message = "A venda deve possuir pelo menos um produto")
    private List<Produto> produtos = new ArrayList<>();

    public Venda() {
    }

    public Venda(LocalDate dataVenda, Double valorTotal) {
        this.dataVenda = dataVenda;
        this.valorTotal = valorTotal;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDataVenda() {
        return dataVenda;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public List<Produto> getProdutos() {
        return produtos;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDataVenda(LocalDate dataVenda) {
        this.dataVenda = dataVenda;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public void setProdutos(List<Produto> produtos) {
        this.produtos = produtos;
    }
}