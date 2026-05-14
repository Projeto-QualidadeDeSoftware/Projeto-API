package com.silva.projeto.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Integer quantidade;
    public Double subtotal;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    public Produto produto;

    @ManyToOne
    @JoinColumn(name = "venda_id")
    public Venda venda;

    public ItemVenda() {
    }

    public ItemVenda(Integer quantidade,
                     Double subtotal,
                     Produto produto,
                     Venda venda) {

        this.quantidade = quantidade;
        this.subtotal = subtotal;
        this.produto = produto;
        this.venda = venda;
    }

    public Long getId() {
        return id;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public Produto getProduto() {
        return produto;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }
}