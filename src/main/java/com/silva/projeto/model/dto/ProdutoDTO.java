package com.silva.projeto.model.dto;

public class ProdutoDTO {

    private Long id;
    private String nome;
    private Double preco;
    private Integer estoque;

    public ProdutoDTO(
            Long id,
            String nome,
            Double preco,
            Integer estoque) {

        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.estoque = estoque;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Double getPreco() {
        return preco;
    }

    public Integer getEstoque() {
        return estoque;
    }
}