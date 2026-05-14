package com.silva.projeto.model.dto;

import java.time.LocalDate;
import java.util.List;

public class VendaDTO {

    private Long id;
    private LocalDate dataVenda;
    private Double valorTotal;

    private ClienteDTO cliente;
    private FuncionarioDTO funcionario;
    private List<ProdutoDTO> produtos;

    public VendaDTO(
            Long id,
            LocalDate dataVenda,
            Double valorTotal,
            ClienteDTO cliente,
            FuncionarioDTO funcionario,
            List<ProdutoDTO> produtos) {

        this.id = id;
        this.dataVenda = dataVenda;
        this.valorTotal = valorTotal;
        this.cliente = cliente;
        this.funcionario = funcionario;
        this.produtos = produtos;
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

    public ClienteDTO getCliente() {
        return cliente;
    }

    public FuncionarioDTO getFuncionario() {
        return funcionario;
    }

    public List<ProdutoDTO> getProdutos() {
        return produtos;
    }
}